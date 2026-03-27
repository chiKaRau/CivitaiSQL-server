package com.civitai.server.models.entities.civitaiSQL;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "model_offline_download_history_table", schema = "Civitai")
public class Model_Offline_Download_History_Table_Entity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "_id")
    private Long id;

    @Column(name = "civitai_version_id")
    private Long civitaiVersionID;

    @Column(name = "civitai_model_id")
    private Long civitaiModelID;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "image_url_list", columnDefinition = "JSON")
    private List<String> imageUrlList;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}