package com.civitai.server.models.entities.civitaiSQL;

import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "rating_table", schema = "Civitai")
public class Rating_Table_Entity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "_id")
    private Long id;

    @Column(name = "rating", length = 500)
    private String rating;

    @Column(name = "expected_max")
    private Integer expectedMax;
}
