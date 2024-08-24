package com.civitai.server.repositories.civitaiSQL;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.civitai.server.models.entities.civitaiSQL.Models_Table_Entity;

@Repository
public interface Models_Table_Repository extends JpaRepository<Models_Table_Entity, Integer> {
    // Setup AuthorRepository to use the JpaRepository
    // then in our project, we can use AuthorRepository(JpaRepo) functions
    // mostly use in /services

    // You can setup internal query here
    @Query("SELECT t FROM Models_Table_Entity t ORDER BY t.id DESC")
    Models_Table_Entity findTopByOrderByIdDesc();

    @Query("SELECT m.modelNumber, m.id, m.category FROM Models_Table_Entity m WHERE m.mainModelName IS NULL AND m.urlAccessable = true")
    List<Object[]> findModelNumberAndIdWhereMainModelNameIsNull();

    @Query("SELECT t.id FROM Models_Table_Entity t WHERE t.category=?1 ORDER BY t.createdAt DESC")
    List<Integer> findLastThreeAddedRecordsID(String category, Pageable pageable);

    @Query("SELECT category FROM Models_Table_Entity t GROUP BY category ORDER BY category ASC")
    List<String> findAllCategories();

    List<Models_Table_Entity> findByModelNumber(String modelID);

    @Query("SELECT t FROM Models_Table_Entity t WHERE t.name LIKE %?1%")
    List<Models_Table_Entity> findAlikeName(String name);

    @Query("SELECT t FROM Models_Table_Entity t WHERE JSON_SEARCH(t.tags, 'all', CONCAT('%', ?1, '%'), NULL, '$') IS NOT NULL")
    List<Models_Table_Entity> findAlikeTags(String name);

    @Query("SELECT t FROM Models_Table_Entity t WHERE JSON_SEARCH(t.triggerWords, 'all', CONCAT('%', ?1, '%'), NULL, '$') IS NOT NULL")
    List<Models_Table_Entity> findAlikeTriggerWords(String name);

    @Query("SELECT t.versionNumber FROM Models_Table_Entity t WHERE t.modelNumber = ?1")
    List<String> findListofVersionNumberByModelNumber(String modelID);

    @Query("SELECT COUNT(*) FROM Models_Table_Entity t WHERE t.modelNumber = ?1")
    long findQuantityByModelID(String modelID);

}
