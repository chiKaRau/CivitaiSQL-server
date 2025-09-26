package com.civitai.server.repositories.civitaiSQL.impl;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.civitai.server.models.entities.civitaiSQL.Models_Table_Entity;

public interface ModelsRepositoryFTS extends JpaRepository<Models_Table_Entity, Integer> {

    @Query(value = """
            SELECT DISTINCT m.*
            FROM models_table m
            LEFT JOIN models_urls_table u ON u._id = m._id
            WHERE
              MATCH(m.name, m.main_model_name, m.category, m.tags_text, m.trigger_words_text)
                AGAINST (?1 IN BOOLEAN MODE)
              OR
              MATCH(u.url)
                AGAINST (?1 IN BOOLEAN MODE)
            """, nativeQuery = true)
    List<Models_Table_Entity> searchAllByBooleanFTS(String booleanQuery);
}