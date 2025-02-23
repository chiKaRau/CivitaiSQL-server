package com.civitai.server.repositories.civitaiSQL.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
}
