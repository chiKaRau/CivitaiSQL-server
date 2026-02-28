package com.civitai.server.models.entities.civitaiSQL;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "category_prefixes_table", schema = "Civitai")
public class Category_Prefixes_Table_Entity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "_id")
    private Long id;

    @Column(name = "prefix_name")
    private String prefixName;

    @Column(name = "download_file_path", columnDefinition = "TEXT")
    private String downloadFilePath;

    @Column(name = "download_priority")
    private Integer downloadPriority;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}