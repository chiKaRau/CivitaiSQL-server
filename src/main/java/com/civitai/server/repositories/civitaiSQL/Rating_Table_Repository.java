package com.civitai.server.repositories.civitaiSQL;

import com.civitai.server.models.entities.civitaiSQL.Rating_Table_Entity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface Rating_Table_Repository extends JpaRepository<Rating_Table_Entity, Long> {
    // add custom queries later if you need (e.g., findByRating)
}
