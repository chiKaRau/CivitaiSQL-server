package com.civitai.server.repositories.civitaiSQL;

import com.civitai.server.models.entities.civitaiSQL.Creator_Table_Entity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface Creator_Table_Repository
                extends JpaRepository<Creator_Table_Entity, Long> {

        Optional<Creator_Table_Entity> findFirstByCivitaiUrl(String civitaiUrl);

        @Modifying
        @Query("update Creator_Table_Entity c set c.lastChecked=false where c.lastChecked=true")
        int clearLastCheckedForAll();

        @Modifying
        @Query("update Creator_Table_Entity c set c.status = :st where c.civitaiUrl = :url")
        int updateStatusByUrl(@Param("url") String url, @Param("st") String st);

        @Modifying
        @Query("update Creator_Table_Entity c set c.rating = :rt where c.civitaiUrl = :url")
        int updateRatingByUrl(@Param("url") String url, @Param("rt") String rt);

        @Modifying
        @Query("update Creator_Table_Entity c set c.lastChecked = :flag where c.civitaiUrl = :url")
        int updateLastCheckedByUrl(@Param("url") String url, @Param("flag") Boolean flag);

        // stable order that won’t shuffle after updates
        List<Creator_Table_Entity> findAllByOrderByIdAsc();

        long deleteByCivitaiUrl(String civitaiUrl); // returns number of rows deleted
}