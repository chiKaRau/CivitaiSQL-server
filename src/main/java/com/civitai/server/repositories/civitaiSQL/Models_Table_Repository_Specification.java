package com.civitai.server.repositories.civitaiSQL;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.civitai.server.models.entities.civitaiSQL.Models_Table_Entity;

public interface Models_Table_Repository_Specification
        extends JpaRepository<Models_Table_Entity, Integer>, JpaSpecificationExecutor<Models_Table_Entity> {
    // Your custom repository methods
}