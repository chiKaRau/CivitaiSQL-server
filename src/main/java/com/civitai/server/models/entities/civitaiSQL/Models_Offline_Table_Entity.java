package com.civitai.server.models.entities.civitaiSQL;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "model_offline_table", schema = "Civitai")
public class Models_Offline_Table_Entity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "_id")
    private Long id;

    // parent props (camelCase -> snake_case in DB)
    private String civitaiFileName;
    private String civitaiBaseModel;
    private String selectedCategory;

    @Column(length = 1000)
    private String downloadFilePath;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_suggested_download_file_path", columnDefinition = "json")
    private List<String> aiSuggestedDownloadFilePath;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "jikan_suggested_download_file_path", columnDefinition = "json")
    private List<String> jikanSuggestedDownloadFilePath;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "local_suggested_download_file_path", columnDefinition = "json")
    private List<String> localSuggestedDownloadFilePath;

    @Column(name = "ai_suggested_artwork_title", length = 500)
    private String aiSuggestedArtworkTitle;

    @Column(name = "jikan_normalized_artwork_title", length = 500)
    private String jikanNormalizedArtworkTitle;

    @Column(columnDefinition = "TEXT")
    private String civitaiUrl;

    @Column(name = "civitai_version_id") // <- map to snake_case column
    private Long civitaiVersionID;

    @Column(name = "civitai_model_id") // <- map to snake_case column
    private Long civitaiModelID;

    // nested as JSON (kept as String; Hibernate validates)
    @Column(columnDefinition = "json")
    private String civitaiModelFileList;

    @Column(columnDefinition = "json")
    private String civitaiTags;

    @Column(columnDefinition = "json")
    private String modelVersionObject;

    @Column(columnDefinition = "json")
    private String imageUrlsArray;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_error", columnDefinition = "TINYINT(1)")
    private Boolean isError;

    @Column(name = "hold", nullable = false, columnDefinition = "TINYINT(1)")
    @Builder.Default
    private Boolean hold = false;

    @Column(name = "download_priority", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    @Builder.Default
    private int downloadPriority = 5;

    @Column(name = "early_access_ends_at")
    private LocalDateTime earlyAccessEndsAt;

    @PrePersist
    void clampDefaults() {
        if (downloadPriority < 1 || downloadPriority > 10)
            downloadPriority = 10;
    }

}