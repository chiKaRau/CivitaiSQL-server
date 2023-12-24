package com.civitai.server.models.entities.civitaiSQL;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "models_images_table", schema = "Civitai")
public class Models_Images_Table_Entity {
    @Id
    @Column(name = "_id")
    private int id;

    @Column(name = "image_urls", columnDefinition = "JSON")
    private String imageUrls;
}
