package com.civitai.server.repositories.civitaiSQL.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
    private static ParsedQ parseQ(String q) {
        if (q == null || q.isBlank()) {
            return new ParsedQ("", List.of());
        }
        Matcher m = FIELD_CLAUSE.matcher(q);
        List<FieldToken> tokens = new ArrayList<>();
        StringBuilder leftover = new StringBuilder(q);
        int shift = 0; // track deletions so we can remove by original span

        while (m.find()) {
            String field = m.group(1);
            String v1 = m.group(2);
            String v2 = m.group(3);
            String v3 = m.group(4);
            String raw = v1 != null ? v1 : v2 != null ? v2 : v3;

            if ("creator".equals(field)) {
                // allowed, but not in ALLOWED_FIELDS because it uses a JOIN (see method)
            } else if (!ALLOWED_FIELDS.containsKey(field)) {
                throw new IllegalArgumentException("Unknown search field: " + field);
            }

            // unescape \" or \' inside quotes
            String val = raw.replace("\\\"", "\"").replace("\\'", "'");

            tokens.add(new FieldToken(field, val, m.start()));

            // remove this span from the leftover
            int start = m.start() - shift;
            int end = m.end() - shift;
            leftover.replace(start, end, " ");
            shift += (m.end() - m.start());
        }

        return new ParsedQ(leftover.toString().trim(), tokens);
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

        // --- Path predicate (same behavior as before) ---
        Predicate pathPred = (q != null && !q.isBlank())
                ? cb.like(root.get("localPath"), "%" + normalizedPath + "%") // subtree
                : cb.like(root.get("localPath"), "%" + normalizedPath); // ends-with

        List<Predicate> preds = new ArrayList<>();
        preds.add(pathPred);

        // --- Parse fielded clauses out of q ---
        ParsedQ parsed = parseQ(q);
        String freeText = parsed.freeText;
        List<FieldToken> tokens = parsed.tokens;

        // --- Optional JOIN for creator ---
        // If you have a relation like:
        // @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="creator_id")
        // private Creator_Table_Entity creator;
        // then:
        Join<Object, Object> creatorJoin = null;
        boolean needsCreator = tokens.stream().anyMatch(t -> "creator".equals(t.field));
        if (needsCreator) {
            // 1) If you HAVE a JPA relation named "creator", use this:
            // creatorJoin = root.join("creator", JoinType.LEFT);
            //
            // 2) If you DO NOT have a relation, but you added a simple column "creator",
            // comment this out and handle via ALLOWED_FIELDS map in the loop below.
            //
            // If neither exists yet, this will throw at runtime; add one of the two options
            // first.
            creatorJoin = root.join("creator", JoinType.LEFT);
        }

        // --- Build predicates for fielded tokens, in the order they appeared (order
        // doesn’t change filtering, but helps relevance scoring) ---
        // Also prepare expressions for relevance scoring (optional).
        List<Expression<Integer>> relevanceBits = new ArrayList<>();
        int weight = tokens.size() > 0 ? tokens.size() : 1; // earlier tokens get higher weight

        for (FieldToken t : tokens) {
            String valLower = t.value.toLowerCase();

            Predicate p;
            if ("creator".equals(t.field)) {
                // JOIN path (option 1). Change "name" to whatever the creator field is.
                Expression<String> creatorName = creatorJoin.get("name");
                p = likeCi(cb, creatorName, valLower);

                // relevance: earlier tokens get bigger weight
                Expression<Integer> bit = cb.<Integer>selectCase().when(p, weight).otherwise(0);
                relevanceBits.add(bit);
            } else {
                String path = ALLOWED_FIELDS.get(t.field);
                Expression<?> column = root.get(path);

                Expression<String> asString;
                if (column.getJavaType() == String.class) {
                    asString = root.get(path);
                } else {
                    // cast non-strings to string for LIKE (dialect-specific; this works on many
                    // DBs)
                    asString = cb.function("CAST", String.class, column);
                }
                p = likeCi(cb, asString, valLower);

                Expression<Integer> bit = cb.<Integer>selectCase().when(p, weight).otherwise(0);
                relevanceBits.add(bit);
            }

            preds.add(p);
            weight--; // next token gets slightly less weight
        }

        // --- Free-text across your 4 columns (as before), if any text remains ---
        Predicate freeTextOr = null;
        if (freeText != null && !freeText.isBlank()) {
            String like = "%" + freeText.toLowerCase() + "%";
            Predicate byName = cb.like(cb.lower(root.get("name")), like);
            Predicate byMain = cb.like(cb.lower(root.get("mainModelName")), like);
            Predicate byVersion = cb.like(cb.lower(root.get("versionNumber")), like);
            Predicate byModelNumber = cb.like(cb.lower(root.get("modelNumber")), like);

            freeTextOr = cb.or(byName, byMain, byVersion, byModelNumber);
            preds.add(freeTextOr);

            // relevance bit for free text (weight 1)
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

        // Path predicate (same rule as list): subtree when q present, ends-with when no
        // q
        Predicate pathPred = (q != null && !q.isBlank())
                ? cb.like(root.get("localPath"), "%" + normalizedPath + "%")
                : cb.like(root.get("localPath"), "%" + normalizedPath);

        List<Predicate> preds = new ArrayList<>();
        preds.add(pathPred);

        // Parse q into fielded tokens + free text (reuse the same parseQ used in list)
        ParsedQ parsed = parseQ(q);

        // Fielded tokens -> LIKE on mapped columns (same ALLOWED_FIELDS logic as list)
        for (FieldToken t : parsed.tokens) {
            String valLower = t.value.toLowerCase();

            // NOTE: we only handle direct columns here (no creator join).
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

        // Free-text across the 4 columns (same as list)
        if (parsed.freeText != null && !parsed.freeText.isBlank()) {
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
