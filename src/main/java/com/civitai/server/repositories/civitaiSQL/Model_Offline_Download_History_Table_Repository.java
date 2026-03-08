package com.civitai.server.repositories.civitaiSQL;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.civitai.server.models.entities.civitaiSQL.Model_Offline_Download_History_Table_Entity;

public interface Model_Offline_Download_History_Table_Repository
        extends JpaRepository<Model_Offline_Download_History_Table_Entity, Long> {

    Page<Model_Offline_Download_History_Table_Entity> findByCivitaiModelIDOrderByCreatedAtDesc(
            Long civitaiModelID,
            Pageable pageable);

    Page<Model_Offline_Download_History_Table_Entity> findByCivitaiVersionIDOrderByCreatedAtDesc(
            Long civitaiVersionID,
            Pageable pageable);

    Page<Model_Offline_Download_History_Table_Entity> findByCivitaiModelIDAndCivitaiVersionIDOrderByCreatedAtDesc(
            Long civitaiModelID,
            Long civitaiVersionID,
            Pageable pageable);

    Page<Model_Offline_Download_History_Table_Entity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query(value = """
            SELECT
                _id,
                civitai_model_id,
                civitai_version_id,
                image_url,
                DATE_FORMAT(created_at, '%Y-%m-%dT%H:%i:%s') AS created_at,
                DATE_FORMAT(updated_at, '%Y-%m-%dT%H:%i:%s') AS updated_at
            FROM model_offline_download_history_table
            ORDER BY created_at DESC
            """, countQuery = """
            SELECT COUNT(*)
            FROM model_offline_download_history_table
            """, nativeQuery = true)
    Page<Object[]> findAllHistoryRows(Pageable pageable);

    @Query(value = """
            SELECT
                _id,
                civitai_model_id,
                civitai_version_id,
                image_url,
                DATE_FORMAT(created_at, '%Y-%m-%dT%H:%i:%s') AS created_at,
                DATE_FORMAT(updated_at, '%Y-%m-%dT%H:%i:%s') AS updated_at
            FROM model_offline_download_history_table
            WHERE created_at >= :start
              AND created_at < :end
            ORDER BY created_at DESC
            """, countQuery = """
            SELECT COUNT(*)
            FROM model_offline_download_history_table
            WHERE created_at >= :start
              AND created_at < :end
            """, nativeQuery = true)
    Page<Object[]> findHistoryRowsByCreatedAtRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

    @Query(value = """
            SELECT DISTINCT DATE_FORMAT(created_at, '%Y-%m-%d') AS created_date
            FROM model_offline_download_history_table
            WHERE created_at >= :start
              AND created_at < :end
            ORDER BY created_date ASC
            """, nativeQuery = true)
    List<String> findAvailableCreatedDatesByMonth(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

}