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
@Table(name = "models_urls_table", schema = "Civitai")
public class Models_Urls_Table_Entity {
    @Id
    @Column(name = "_id")
    private int id;

    @Column(name = "url")
    private String url;
}
