package com.civitai.server.repositories.civitaiSQL;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    @Query("SELECT t.id FROM Models_Table_Entity t WHERE t.category=?1 ORDER BY t.createdAt DESC")
    List<Integer> findLastThreeAddedRecordsID(String category, Pageable pageable);

    @Query("SELECT category FROM Models_Table_Entity t GROUP BY category ORDER BY category ASC")
    List<String> findAllCategories();

    List<Models_Table_Entity> findByModelNumber(String modelID);

}
