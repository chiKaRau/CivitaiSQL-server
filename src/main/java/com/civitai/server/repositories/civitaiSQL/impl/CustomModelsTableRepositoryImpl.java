package com.civitai.server.repositories.civitaiSQL.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.civitai.server.models.entities.civitaiSQL.Models_Table_Entity;
import com.civitai.server.repositories.civitaiSQL.CustomModelsTableRepository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class CustomModelsTableRepositoryImpl implements CustomModelsTableRepository {

    @PersistenceContext
    private EntityManager entityManager;

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

    // in CustomModelsTableRepositoryImpl
    @Override
    public List<Models_Table_Entity> findVirtualFilesByPathPaged(
            String normalizedPath, int offset, int limit, String sortKey, boolean asc, String q) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Models_Table_Entity> cq = cb.createQuery(Models_Table_Entity.class);
        Root<Models_Table_Entity> root = cq.from(Models_Table_Entity.class);

        // Path predicate:
        // - no q: exact folder (endsWith normalizedPath)
        // - with q: subtree (contains normalizedPath anywhere)
        Predicate pathPred = (q != null && !q.isBlank())
                ? cb.like(root.get("localPath"), "%" + normalizedPath + "%") // subtree
                : cb.like(root.get("localPath"), "%" + normalizedPath); // exact folder

        List<Predicate> preds = new ArrayList<>();
        preds.add(pathPred);

        // Text predicate (case-insensitive) on name / mainModelName
        if (q != null && !q.isBlank()) {
            String like = "%" + q.toLowerCase() + "%";
            Predicate byName = cb.like(cb.lower(root.get("name")), like);
            Predicate byMain = cb.like(cb.lower(root.get("mainModelName")), like);
            preds.add(cb.or(byName, byMain));
        }

        cq.where(preds.toArray(new Predicate[0]));

        // Sort
        List<Order> orders = new ArrayList<>();
        String key = (sortKey == null ? "name" : sortKey).toLowerCase();
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
            case "name":
            default:
                orders.add(asc ? cb.asc(root.get("name")) : cb.desc(root.get("name")));
                break;
        }
        orders.add(asc ? cb.asc(root.get("id")) : cb.desc(root.get("id"))); // tiebreaker
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

        Predicate pathPred = (q != null && !q.isBlank())
                ? cb.like(root.get("localPath"), "%" + normalizedPath + "%")
                : cb.like(root.get("localPath"), "%" + normalizedPath);

        List<Predicate> preds = new ArrayList<>();
        preds.add(pathPred);

        if (q != null && !q.isBlank()) {
            String like = "%" + q.toLowerCase() + "%";
            Predicate byName = cb.like(cb.lower(root.get("name")), like);
            Predicate byMain = cb.like(cb.lower(root.get("mainModelName")), like);
            preds.add(cb.or(byName, byMain));
        }

        cq.select(cb.count(root)).where(preds.toArray(new Predicate[0]));
        return entityManager.createQuery(cq).getSingleResult();
    }

}
