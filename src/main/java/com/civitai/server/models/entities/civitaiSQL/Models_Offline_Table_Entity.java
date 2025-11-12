package com.civitai.server.models.entities.civitaiSQL;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

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
    private boolean hold = false;

    @Column(name = "download_priority", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    @Builder.Default
    private int downloadPriority = 10;

    @Column(name = "early_access_ends_at")
    private LocalDateTime earlyAccessEndsAt;

    @PrePersist
    void clampDefaults() {
        if (downloadPriority < 1 || downloadPriority > 10)
            downloadPriority = 10;
    }

}