package com.civitai.server.repositories.civitaiSQL;

import com.civitai.server.models.entities.civitaiSQL.Download_File_Path_Count_Table_Entity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.transaction.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface Download_File_Path_Count_Table_Repository
                extends JpaRepository<Download_File_Path_Count_Table_Entity, Long> {

        Optional<Download_File_Path_Count_Table_Entity> findByDownloadFilePath(String downloadFilePath);

        List<Download_File_Path_Count_Table_Entity> findAllByOrderByLastAddedDesc();

        List<Download_File_Path_Count_Table_Entity> findAllByOrderByPathCountDesc();

        @Modifying
        @Transactional
        @Query(value = "INSERT INTO download_file_path_count_table (download_file_path, `count`, last_added) " +
                        "VALUES (:path, 1, CURRENT_TIMESTAMP(6)) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "  `count` = `count` + 1, " +
                        "  last_added = CURRENT_TIMESTAMP(6)", nativeQuery = true)
        int incrementPathCount(@Param("path") String path);

        @Query(value = "SELECT last_added, download_file_path, `count` " +
                        "FROM download_file_path_count_table " +
                        "WHERE download_file_path NOT LIKE '%/@scan@/Update/%' " +
                        "  AND (:prefix IS NULL OR :prefix = '' OR download_file_path LIKE CONCAT(:prefix, '%')) " +
                        "  AND last_added >= :since " +
                        "ORDER BY `count` DESC " +
                        "LIMIT 10", nativeQuery = true)
        List<Object[]> findTop10ByCountSince(
                        @Param("prefix") String prefix,
                        @Param("since") Timestamp since);

        @Query(value = "SELECT last_added, download_file_path, `count` " +
                        "FROM download_file_path_count_table " +
                        "WHERE download_file_path NOT LIKE '%/@scan@/Update/%' " +
                        "  AND (:prefix IS NULL OR :prefix = '' OR download_file_path LIKE CONCAT(:prefix, '%')) " +
                        "ORDER BY last_added DESC " +
                        "LIMIT 10", nativeQuery = true)
        List<Object[]> findRecentAdded10(@Param("prefix") String prefix);

        @Query(value = "SELECT last_added, download_file_path, `count`, updated_at " +
                        "FROM download_file_path_count_table " +
                        "WHERE download_file_path NOT LIKE '%/@scan@/Update/%' " +
                        "  AND (:prefix IS NULL OR :prefix = '' OR download_file_path LIKE CONCAT(:prefix, '%')) " +
                        "ORDER BY updated_at DESC " +
                        "LIMIT 10", nativeQuery = true)
        List<Object[]> findRecentUpdated10(@Param("prefix") String prefix);

        @Modifying
        @Transactional
        @Query("DELETE FROM Download_File_Path_Count_Table_Entity e WHERE e.downloadFilePath = :path")
        int deleteByDownloadFilePathExact(@Param("path") String path);

}