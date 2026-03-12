package com.civitai.server.specification.civitaiSQL;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.civitai.server.models.entities.civitaiSQL.Models_Table_Entity;
import com.civitai.server.models.entities.civitaiSQL.Models_Urls_Table_Entity;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

/*
 * 
 * SELECT DISTINCT table1.* FROM table1
JOIN table2 ON table1.id = table2.table1_id
WHERE (table1.column1 LIKE '%girl%' OR table1.column2 LIKE '%girl%' OR table2.column3 LIKE '%girl%')
AND (table1.column1 LIKE '%glasses%' OR table1.column2 LIKE '%glasses%' OR table2.column3 LIKE '%glasses%')
AND (table1.column1 LIKE '%blown eye%' OR table1.column2 LIKE '%blown eye%' OR table2.column3 LIKE '%blown eye%');
 */

public class Models_Table_Specification {
    public static Specification<Models_Table_Entity> keywordInAnyColumn(String keyword) {
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();
            String lowerKeyword = keyword.toLowerCase();

            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + lowerKeyword + "%"));
            predicates.add(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("mainModelName")), "%" + lowerKeyword + "%"));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("tags")), "%" + lowerKeyword + "%"));
            predicates
                    .add(criteriaBuilder.like(criteriaBuilder.lower(root.get("localTags")), "%" + lowerKeyword + "%"));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("aliases")), "%" + lowerKeyword + "%"));
            predicates.add(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("triggerWords")), "%" + lowerKeyword + "%"));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("category")), "%" + lowerKeyword + "%"));
            predicates
                    .add(criteriaBuilder.like(criteriaBuilder.lower(root.get("localPath")), "%" + lowerKeyword + "%"));

            Join<Models_Table_Entity, Models_Urls_Table_Entity> join = root.join("modelsUrlsTable", JoinType.LEFT);
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(join.get("url")), "%" + lowerKeyword + "%"));

            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Models_Table_Entity> findByTagsList(List<String> keywords) {
        Specification<Models_Table_Entity> spec = Specification.where(null);

        for (String keyword : keywords) {
            spec = spec.and(keywordInAnyColumn(keyword));
        }

        return spec;
    }
}
