package com.civitai.server.repositories.civitaiSQL;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.civitai.server.models.entities.civitaiSQL.Models_Details_Table_Entity;

@Repository
public interface Models_Details_Table_Repository extends JpaRepository<Models_Details_Table_Entity, Integer> {
    // Setup AuthorRepository to use the JpaRepository
    // then in our project, we can use AuthorRepository(JpaRepo) functions
    // mostly use in /services

    // You can setup internal query here
}
