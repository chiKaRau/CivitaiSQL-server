package com.civitai.server.repositories.civitaiSQL;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.civitai.server.models.entities.civitaiSQL.Models_Urls_Table_Entity;

//return record or null
@Repository
public interface Models_Urls_Table_Repository extends JpaRepository<Models_Urls_Table_Entity, Integer> {
    // Setup AuthorRepository to use the JpaRepository
    // then in our project, we can use AuthorRepository(JpaRepo) functions
    // mostly use in /services

    // You can setup internal query here
    Models_Urls_Table_Entity findByUrl(String url);

    @Query("SELECT t FROM Models_Urls_Table_Entity t WHERE t.url LIKE %?1%")
    List<Models_Urls_Table_Entity> findAlikeUrl(String name);

    @Query("SELECT COUNT(*) FROM Models_Urls_Table_Entity WHERE url = ?1")
    long findQuantityByUrl(String url);


}
