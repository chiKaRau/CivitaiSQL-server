package com.civitai.server.models.entities.civitaiSQL;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "download_file_path_count_table", schema = "Civitai")
public class Download_File_Path_Count_Table_Entity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "_id")
    private Long id;

    @Column(name = "download_file_path", columnDefinition = "LONGTEXT", nullable = false)
    private String downloadFilePath;

    /**
     * If your table uses:
     * path_hash BINARY(32) GENERATED ALWAYS AS (UNHEX(SHA2(download_file_path,
     * 256))) STORED
     *
     * then this should be read-only in JPA.
     */
    @Column(name = "path_hash", columnDefinition = "BINARY(32)", insertable = false, updatable = false)
    private byte[] pathHash;

    // Avoid naming the Java field "count" to prevent confusion with SQL/JPQL
    // COUNT(...)
    @Column(name = "count", nullable = false)
    private Integer pathCount;

    @Column(name = "last_added", nullable = false)
    private LocalDateTime lastAdded;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}