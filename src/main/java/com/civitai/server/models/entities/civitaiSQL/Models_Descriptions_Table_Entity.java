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
@Table(name = "models_descriptions_table", schema = "Civitai")
public class Models_Descriptions_Table_Entity {
    @Id
    @Column(name = "_id")
    private int id;

    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;
}
