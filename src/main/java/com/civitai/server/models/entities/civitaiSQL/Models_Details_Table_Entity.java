package com.civitai.server.models.entities.civitaiSQL;

import java.time.LocalDate;
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
@Table(name = "models_details_table", schema = "Civitai")
public class Models_Details_Table_Entity {
    @Id
    @Column(name = "_id")
    private int id;

    @Column(name = "type")
    private String type;

    @Column(name = "stats")
    private String stats;

    @Column(name = "uploaded")
    private LocalDate uploaded;

    @Column(name = "base_model")
    private String baseModel;

    @Column(name = "hash")
    private String hash;

    @Column(name = "usage_tips")
    private String usageTips;

    @Column(name = "creator_name")
    private String creatorName;
}
