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

    @OneToOne
    @JoinColumn(name = "models_table_id") // This column in the Models_Urls_Table_Entity table references the id of Models_Table_Entity
    private Models_Table_Entity modelsTableEntity;

}
