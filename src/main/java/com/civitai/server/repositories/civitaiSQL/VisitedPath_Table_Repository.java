package com.civitai.server.repositories.civitaiSQL;

import com.civitai.server.models.entities.civitaiSQL.VisitedPath_Table_Entity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VisitedPath_Table_Repository extends JpaRepository<VisitedPath_Table_Entity, String> {
    // Basic lookups
    boolean existsByPath(String path);

    List<VisitedPath_Table_Entity> findByParentPathAndContextOrderByLastAccessedAtDesc(
            String parentPath,
            VisitedPath_Table_Entity.Context context);

    List<VisitedPath_Table_Entity> findByParentPathAndContext(
            String parentPath,
            VisitedPath_Table_Entity.Context context,
            Pageable pageable);

    long countByParentPathAndContext(String parentPath, VisitedPath_Table_Entity.Context context);

    // Touch a row (increment count + update last_accessed_at) if it already exists
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            UPDATE VisitedPath_Table_Entity v
               SET v.lastAccessedAt = CURRENT_TIMESTAMP,
                   v.accessCount = v.accessCount + 1
             WHERE v.path = :path
            """)
    int touch(@Param("path") String path);

    // Atomic UPSERT (MySQL-specific). Inserts a row or updates it if the PK (path)
    // already exists.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = """
            INSERT INTO Civitai.visited_paths
                (path, parent_path, drive, context, access_count)
            VALUES
                (:path, :parentPath, :drive, :context, 1)
            ON DUPLICATE KEY UPDATE
                parent_path = VALUES(parent_path),
                drive = VALUES(drive),
                context = VALUES(context),
                last_accessed_at = CURRENT_TIMESTAMP,
                access_count = access_count + 1
            """, nativeQuery = true)
    void upsertVisit(
            @Param("path") String path,
            @Param("parentPath") String parentPath,
            @Param("drive") String drive);

    // Optional: housekeeping
    @Modifying
    @Transactional
    @Query("DELETE FROM VisitedPath_Table_Entity v WHERE v.lastAccessedAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);

    List<VisitedPath_Table_Entity> findByParentPath(String parentPath);

    List<VisitedPath_Table_Entity> findByParentPathAndContext(String parentPath,
            VisitedPath_Table_Entity.Context context);

}
