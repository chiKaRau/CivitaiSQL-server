package com.civitai.server.repositories.civitaiSQL.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.civitai.server.models.entities.civitaiSQL.Models_Details_Table_Entity;
import com.civitai.server.models.entities.civitaiSQL.Models_Table_Entity;
import com.civitai.server.repositories.civitaiSQL.CustomModelsTableRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Repository
public class CustomModelsTableRepositoryImpl implements CustomModelsTableRepository {

    @PersistenceContext
    private EntityManager entityManager;

    // --- at class level ---
    private static final Pattern FIELD_CLAUSE = Pattern.compile(
            "(\\w+)\\s*:\\s*(?:\"((?:[^\"\\\\]|\\\\.)*)\"|'((?:[^'\\\\]|\\\\.)*)'|(\\S+))");

    /** Map of allowed field -> JPA path in Models_Table_Entity */
    private static final Map<String, String> ALLOWED_FIELDS = Map.of(
            // direct columns
            "name", "name",
            "mainModelName", "mainModelName",
            "category", "category",
            "versionNumber", "versionNumber",
            "modelNumber", "modelNumber",
            "localPath", "localPath",
            "tags", "tags", // JSON as text - substring search
            "aliases", "aliases", // JSON as text - substring search
            "triggerWords", "triggerWords", // JSON as text - substring search
            "myRating", "myRating" // will be cast to string for LIKE
    // "creator", "creator" // <-- ONLY if you add a creator column
    );

    /** One parsed fielded token in the original order */
    private static final class FieldToken {
        final String field;
        final String value;
        final int position; // start index in q, used for relevance weighting/order

        FieldToken(String field, String value, int position) {
            this.field = field;
            this.value = value;
            this.position = position;
        }
    }

    /**
     * Parse q into (fielded tokens in order, remaining free text). Throws on
     * unknown field.
     */
    // --- in parseQ(String q): sanitize first, and clean leftover at the end ---
    private static ParsedQ parseQ(String q) {
        q = stripOuterQuotes(q); // <--- NEW
        if (q == null || q.isBlank()) {
            return new ParsedQ("", List.of());
        }

        Matcher m = FIELD_CLAUSE.matcher(q);
        List<FieldToken> tokens = new ArrayList<>();
        StringBuilder leftover = new StringBuilder(q);
        int shift = 0;

        while (m.find()) {
            String fieldRaw = m.group(1);
            String field = normalizeField(fieldRaw);
            String v1 = m.group(2), v2 = m.group(3), v3 = m.group(4);
            String raw = v1 != null ? v1 : (v2 != null ? v2 : v3);

            if (!ALLOWED_FIELDS.containsKey(field) && !DETAILS_FIELDS.contains(field)) {
                throw new IllegalArgumentException("Unknown search field: " + fieldRaw);
            }

            String val = raw.replace("\\\"", "\"").replace("\\'", "'");
            tokens.add(new FieldToken(field, val, m.start()));

            int start = m.start() - shift, end = m.end() - shift;
            leftover.replace(start, end, " ");
            shift += (m.end() - m.start());
        }

        // collapse quotes-only leftovers to empty
        String rest = leftover.toString()
                .replace("\"", " ").replace("'", " ")
                .replaceAll("\\s+", " ")
                .trim();

        return new ParsedQ(rest, tokens);
    }

    private static final class ParsedQ {
        final String freeText;
        final List<FieldToken> tokens;

        ParsedQ(String freeText, List<FieldToken> tokens) {
            this.freeText = freeText;
            this.tokens = tokens.stream()
                    .sorted(Comparator.comparingInt(t -> t.position)) // keep original order
                    .toList();
        }
    }

    /** Case-insensitive LIKE: LOWER(expr) LIKE %val% */
    private static Predicate likeCi(CriteriaBuilder cb, Expression<String> expr, String valLower) {
        return cb.like(cb.lower(expr), "%" + valLower + "%");
    }

    /**
     * LOWER(REPLACE(expr, '\', '/')) so we can do slash-agnostic LIKEs safely
     * (MySQL-safe)
     */
    private static Expression<String> normPathExpr(CriteriaBuilder cb, Expression<String> pathExpr) {
        // CHAR(92) = backslash in MySQL; avoids escaping hell
        Expression<String> backslash = cb.function("CHAR", String.class, cb.literal(92));
        Expression<String> fwd = cb.function("REPLACE", String.class, pathExpr, backslash, cb.literal("/"));
        return cb.lower(fwd);
    }

    private static String normalizeNeedle(String raw) {
        if (raw == null)
            return "";
        String s = raw.replace('\\', '/').toLowerCase();
        // trim trailing slashes so "%/acg/appearance%" matches both ".../appearance"
        // and ".../appearance/..."
        while (s.endsWith("/"))
            s = s.substring(0, s.length() - 1);
        return s;
    }

    /** Fields that live in models_details_table */
    private static final java.util.Set<String> DETAILS_FIELDS = java.util.Set.of(
            "type", "baseModel", "creatorName");

    /** Accept friendly aliases in q: base_model, creator_name, creator */
    private static String normalizeField(String raw) {
        if (raw == null)
            return null;
        switch (raw) {
            case "base_model":
                return "baseModel";
            case "creator_name":
            case "creator":
                return "creatorName";
            default:
                return raw;
        }
    }

    /**
     * EXISTS (select 1 from models_details_table d where d._id = m._id and
     * lower(d.field) like %val%)
     */
    /**
     * m.id IN (select d.id from models_details_table d
     * where d.id = m.id and lower(d.<field>) like %val%)
     */
    private static Predicate detailsLikeIn(CriteriaBuilder cb,
            CriteriaQuery<?> parent,
            Root<Models_Table_Entity> root,
            String detailsField,
            String valLower) {

        Subquery<Integer> sq = parent.subquery(Integer.class);
        Root<Models_Details_Table_Entity> d = sq.from(Models_Details_Table_Entity.class);

        Expression<?> col = d.get(detailsField);
        Expression<String> asString = (col.getJavaType() == String.class)
                ? d.get(detailsField)
                : cb.function("CAST", String.class, col);

        sq.select(d.get("id"))
                .where(
                        cb.and(
                                cb.equal(d.get("id"), root.get("id")), // link d._id = m._id
                                cb.like(cb.lower(asString), "%" + valLower + "%") // filter on details field
                        ));

        return root.get("id").in(sq);
    }

    /**
     * EXISTS (select 1
     * from models_details_table d
     * where d.id = m.id
     * and lower(d.<field>) like %val%)
     */
    private static Predicate detailsLikeExists(
            CriteriaBuilder cb,
            CriteriaQuery<?> parent,
            Root<Models_Table_Entity> root,
            String detailsField,
            String valLower) {

        Subquery<Integer> sq = parent.subquery(Integer.class);
        Root<Models_Details_Table_Entity> d = sq.from(Models_Details_Table_Entity.class);

        Expression<?> col = d.get(detailsField);
        Expression<String> asString = (col.getJavaType() == String.class)
                ? d.get(detailsField)
                : cb.function("CAST", String.class, col);

        sq.select(cb.literal(1))
                .where(
                        cb.and(
                                cb.equal(d.get("id"), root.get("id")), // link d._id = m._id
                                cb.like(cb.lower(asString), "%" + valLower + "%") // filter on details field
                        ));

        return cb.exists(sq);
    }

    // --- add helpers near the top of the class ---

    /** If q is wrapped in a single pair of quotes, remove them. */
    private static String stripOuterQuotes(String s) {
        if (s == null)
            return null;
        s = s.trim();
        if ((s.length() >= 2) &&
                ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")))) {
            return s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    /** Is there any meaningful free text (at least one letter or digit)? */
    private static boolean hasMeaningfulFreeText(String s) {
        if (s == null)
            return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c))
                return true;
        }
        return false;
    }

    @Override
    @Transactional
    public int updateLocalPathByPairsDynamic(List<Object[]> pairs, String localPath) {
        if (pairs == null || pairs.isEmpty()) {
            return 0;
        }
        StringBuilder queryBuilder = new StringBuilder("UPDATE models_table SET local_path = :localPath WHERE ");
        List<String> conditions = new ArrayList<>();

        for (int i = 0; i < pairs.size(); i++) {
            // Each condition compares a pair of values
            conditions.add("(model_number = :modelID" + i + " AND version_number = :versionID" + i + ")");
        }
        queryBuilder.append(String.join(" OR ", conditions));

        Query query = entityManager.createNativeQuery(queryBuilder.toString());
        query.setParameter("localPath", localPath);

        for (int i = 0; i < pairs.size(); i++) {
            Object[] pair = pairs.get(i);
            query.setParameter("modelID" + i, pair[0]);
            query.setParameter("versionID" + i, pair[1]);
        }

        return query.executeUpdate();
    }

    @Override
    public List<Models_Table_Entity> findByModelNumberAndVersionNumberIn(List<Object[]> pairs) {
        if (pairs == null || pairs.isEmpty()) {
            return new ArrayList<>();
        }
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Models_Table_Entity> query = cb.createQuery(Models_Table_Entity.class);
        Root<Models_Table_Entity> root = query.from(Models_Table_Entity.class);

        List<Predicate> predicates = new ArrayList<>();
        for (Object[] pair : pairs) {
            String modelID = (String) pair[0];
            String versionID = (String) pair[1];
            Predicate condition = cb.and(
                    cb.equal(root.get("modelNumber"), modelID),
                    cb.equal(root.get("versionNumber"), versionID));
            predicates.add(condition);
        }
        query.where(cb.or(predicates.toArray(new Predicate[0])));
        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public List<Models_Table_Entity> findVirtualFilesByPathPaged(
            String normalizedPath, int offset, int limit, String sortKey, boolean asc, String q) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Models_Table_Entity> cq = cb.createQuery(Models_Table_Entity.class);
        Root<Models_Table_Entity> root = cq.from(Models_Table_Entity.class);

        // predicates list FIRST
        List<Predicate> preds = new ArrayList<>();

        // normalize the DB column + needle
        Expression<String> normLocal = normPathExpr(cb, root.get("localPath"));
        String needle = normalizeNeedle(normalizedPath);

        // path predicate
        Predicate pathPred = (q != null && !q.isBlank())
                ? cb.like(normLocal, "%" + needle + "%") // subtree when searching
                : cb.like(normLocal, "%" + needle); // ends-with-ish
        preds.add(pathPred);

        // --- Parse fielded clauses out of q ---
        ParsedQ parsed = parseQ(q);
        String freeText = parsed.freeText;
        List<FieldToken> tokens = parsed.tokens;

        // --- Build predicates for fielded tokens, in the order they appeared (order
        // doesn’t change filtering, but helps relevance scoring) ---
        // Also prepare expressions for relevance scoring (optional).
        List<Expression<Integer>> relevanceBits = new ArrayList<>();
        int weight = tokens.size() > 0 ? tokens.size() : 1; // earlier tokens get higher weight

        for (FieldToken t : tokens) {
            String valLower = t.value.toLowerCase();
            Predicate p;

            if (DETAILS_FIELDS.contains(t.field)) {
                // models_details_table via EXISTS
                p = detailsLikeExists(cb, cq, root, t.field, valLower);
            } else {
                // main-table column (existing behavior)
                String path = ALLOWED_FIELDS.get(t.field);
                Expression<?> column = root.get(path);
                Expression<String> asString = (column.getJavaType() == String.class)
                        ? root.get(path)
                        : cb.function("CAST", String.class, column);
                p = likeCi(cb, asString, valLower);
            }

            preds.add(p);

            // (optional) relevance scoring you already had
            Expression<Integer> bit = cb.<Integer>selectCase().when(p, weight).otherwise(0);
            relevanceBits.add(bit);
            weight--;
        }

        // --- Free-text across your 4 columns (as before), if any text remains ---
        // --- in findVirtualFilesByPathPaged(...) where you add free text ---
        Predicate freeTextOr = null;
        if (hasMeaningfulFreeText(freeText)) { // <--- changed condition
            String like = "%" + freeText.toLowerCase() + "%";
            Predicate byName = cb.like(cb.lower(root.get("name")), like);
            Predicate byMain = cb.like(cb.lower(root.get("mainModelName")), like);
            Predicate byVersion = cb.like(cb.lower(root.get("versionNumber")), like);
            Predicate byModelNumber = cb.like(cb.lower(root.get("modelNumber")), like);

            freeTextOr = cb.or(byName, byMain, byVersion, byModelNumber);
            preds.add(freeTextOr);

            relevanceBits.add(cb.<Integer>selectCase().when(freeTextOr, 1).otherwise(0));
        }

        cq.where(preds.toArray(new Predicate[0]));

        // --- Sorting (add "relevance" as a new option) ---
        List<Order> orders = new ArrayList<>();
        String key = (sortKey == null ? "name" : sortKey).toLowerCase();

        if ("relevance".equals(key) && !relevanceBits.isEmpty()) {
            // sum bits into a score, then sort by score desc first
            Expression<Integer> score = relevanceBits.get(0);
            for (int i = 1; i < relevanceBits.size(); i++) {
                score = cb.sum(score, relevanceBits.get(i));
            }
            orders.add(cb.desc(score));
        }

        switch (key) {
            case "created":
                orders.add(asc ? cb.asc(root.get("createdAt")) : cb.desc(root.get("createdAt")));
                break;
            case "modified":
                orders.add(asc ? cb.asc(root.get("updatedAt")) : cb.desc(root.get("updatedAt")));
                break;
            case "myrating": {
                Expression<Integer> nullsLast = cb.<Integer>selectCase()
                        .when(cb.isNull(root.get("myRating")), 1)
                        .otherwise(0);
                orders.add(cb.asc(nullsLast));
                orders.add(asc ? cb.asc(root.get("myRating")) : cb.desc(root.get("myRating")));
                break;
            }

            case "modelnumber": {
                // Put null/empty last
                Predicate isNullOrEmpty = cb.or(
                        cb.isNull(root.get("modelNumber")),
                        cb.equal(cb.trim(root.get("modelNumber")), ""));
                Expression<Integer> nullsLast = cb.<Integer>selectCase()
                        .when(isNullOrEmpty, 1).otherwise(0);
                orders.add(cb.asc(nullsLast));

                // Prefer a numeric cast for correct numeric ordering
                // Most Hibernate+MySQL setups will emit CAST(model_number AS integer)
                Expression<Integer> modelNumAsInt = root.get("modelNumber").as(Integer.class);
                orders.add(asc ? cb.asc(modelNumAsInt) : cb.desc(modelNumAsInt));

                // (Optional) stable fallback if your dialect can’t cast:
                // orders.add(asc ? cb.asc(cb.length(root.get("modelNumber"))) :
                // cb.desc(cb.length(root.get("modelNumber"))));
                // orders.add(asc ? cb.asc(root.get("modelNumber")) :
                // cb.desc(root.get("modelNumber")));
                break;
            }

            case "versionnumber": {
                Predicate isNullOrEmpty = cb.or(
                        cb.isNull(root.get("versionNumber")),
                        cb.equal(cb.trim(root.get("versionNumber")), ""));
                Expression<Integer> nullsLast = cb.<Integer>selectCase()
                        .when(isNullOrEmpty, 1).otherwise(0);
                orders.add(cb.asc(nullsLast));

                Expression<Integer> versionNumAsInt = root.get("versionNumber").as(Integer.class);
                orders.add(asc ? cb.asc(versionNumAsInt) : cb.desc(versionNumAsInt));

                // (Optional) fallback as above if needed
                // orders.add(asc ? cb.asc(cb.length(root.get("versionNumber"))) :
                // cb.desc(cb.length(root.get("versionNumber"))));
                // orders.add(asc ? cb.asc(root.get("versionNumber")) :
                // cb.desc(root.get("versionNumber")));
                break;
            }

            case "relevance":
                // already added above; also add id tiebreaker below
                break;
            case "name":
            default:
                orders.add(asc ? cb.asc(root.get("name")) : cb.desc(root.get("name")));
                break;
        }

        // stable tiebreaker
        orders.add(asc ? cb.asc(root.get("id")) : cb.desc(root.get("id")));
        cq.orderBy(orders);

        return entityManager.createQuery(cq)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public long countVirtualFilesByPath(String normalizedPath, String q) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Models_Table_Entity> root = cq.from(Models_Table_Entity.class);

        // predicates list FIRST
        List<Predicate> preds = new ArrayList<>();

        // normalize the DB column + needle
        Expression<String> normLocal = normPathExpr(cb, root.get("localPath"));
        String needle = normalizeNeedle(normalizedPath);

        // path predicate
        Predicate pathPred = (q != null && !q.isBlank())
                ? cb.like(normLocal, "%" + needle + "%") // subtree when searching
                : cb.like(normLocal, "%" + needle); // ends-with-ish
        preds.add(pathPred);

        // Parse q into fielded tokens + free text (reuse the same parseQ used in list)
        ParsedQ parsed = parseQ(q);

        // Fielded tokens -> LIKE on mapped columns (same ALLOWED_FIELDS logic as list)
        for (FieldToken t : parsed.tokens) {
            String valLower = t.value.toLowerCase();

            if (DETAILS_FIELDS.contains(t.field)) {
                preds.add(detailsLikeExists(cb, cq, root, t.field, valLower));
            } else {
                String path = ALLOWED_FIELDS.get(t.field);
                if (path == null) {
                    throw new IllegalArgumentException("Unknown search field: " + t.field);
                }
                Expression<?> col = root.get(path);
                Expression<String> asString = (col.getJavaType() == String.class)
                        ? root.get(path)
                        : cb.function("CAST", String.class, col);
                preds.add(cb.like(cb.lower(asString), "%" + valLower + "%"));
            }
        }

        // --- in countVirtualFilesByPath(...) the same free-text condition ---
        if (hasMeaningfulFreeText(parsed.freeText)) { // <--- changed condition
            String like = "%" + parsed.freeText.toLowerCase() + "%";
            Predicate byName = cb.like(cb.lower(root.get("name")), like);
            Predicate byMain = cb.like(cb.lower(root.get("mainModelName")), like);
            Predicate byVersion = cb.like(cb.lower(root.get("versionNumber")), like);
            Predicate byModelNumber = cb.like(cb.lower(root.get("modelNumber")), like);
            preds.add(cb.or(byName, byMain, byVersion, byModelNumber));
        }

        cq.select(cb.count(root));
        cq.where(preds.toArray(new Predicate[0]));

        Long cnt = entityManager.createQuery(cq).getSingleResult();
        return (cnt == null ? 0L : cnt);
    }

}
