package com.civitai.server.models.entities.civitaiSQL;

import java.sql.Timestamp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@Table(name = "models_table", schema = "Civitai")
@AllArgsConstructor
@NoArgsConstructor
public class Models_Table_Entity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "_id")
    private int id;

    @Column(name = "name")
    private String name;

    @Column(name = "tags", columnDefinition = "JSON")
    private String tags;

    @Column(name = "category")
    private String category;

    @Column(name = "version_number")
    private String versionNumber;

    @Column(name = "model_number")
    private String modelNumber;

    @Column(name = "trigger_words", columnDefinition = "JSON")
    private String triggerWords;

    @Column(name = "nsfw", columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean nsfw;

    @Column(name = "flag", columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean flag;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new Timestamp(System.currentTimeMillis());
        updatedAt = new Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Timestamp(System.currentTimeMillis());
    }

}
