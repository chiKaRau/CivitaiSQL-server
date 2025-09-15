package com.civitai.server.repositories.civitaiSQL;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.civitai.server.models.entities.civitaiSQL.Recycle_Table_Entity;

import java.util.List;

@Repository
public interface Recycle_Table_Repository extends JpaRepository<Recycle_Table_Entity, String> {

    // ---- Derived queries (no native SQL) ----
    List<Recycle_Table_Entity> findAllByOrderByDeletedDateDesc();

    List<Recycle_Table_Entity> findAllByTypeOrderByDeletedDateDesc(Recycle_Table_Entity.RecordType type);

    List<Recycle_Table_Entity> findAllByType(Recycle_Table_Entity.RecordType type);

    // ---- Native helpers for MySQL JSON column ('files') ----

    /** Find records whose JSON 'files' array contains the given absolute path. */
    @Query(value = "SELECT r.* FROM recycle_records r " +
            "WHERE JSON_SEARCH(r.files, 'one', :filePath) IS NOT NULL", nativeQuery = true)
    List<Recycle_Table_Entity> findAllContainingFile(@Param("filePath") String filePath);

    /** Delete records whose JSON 'files' array contains the given absolute path. */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM recycle_records " +
            "WHERE JSON_SEARCH(files, 'one', :filePath) IS NOT NULL", nativeQuery = true)
    int deleteAllContainingFile(@Param("filePath") String filePath);

    /**
     * Find records where 'files' JSON overlaps with the provided JSON array string.
     */
    @Query(value = "SELECT r.* FROM recycle_records r " +
            "WHERE JSON_OVERLAPS(r.files, CAST(:filesJson AS JSON))", nativeQuery = true)
    List<Recycle_Table_Entity> findAnyOverlappingFiles(@Param("filesJson") String filesJson);

    /**
     * Delete records where 'files' JSON overlaps with the provided JSON array
     * string.
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM recycle_records " +
            "WHERE JSON_OVERLAPS(files, CAST(:filesJson AS JSON))", nativeQuery = true)
    int deleteAnyOverlappingFiles(@Param("filesJson") String filesJson);
}