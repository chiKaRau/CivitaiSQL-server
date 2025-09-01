package com.civitai.server.models.entities.civitaiSQL;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "creator_table", schema = "Civitai")
public class Creator_Table_Entity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "_id")
    private Long id;

    private String status;

    @Column(length = 500)
    private String message;

    @Column(length = 500)
    private String categories;

    @Column(columnDefinition = "TEXT")
    private String civitaiUrl;

    private Boolean lastChecked;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}