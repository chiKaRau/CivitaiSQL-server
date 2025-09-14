package com.civitai.server.models.entities.civitaiSQL;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "visited_paths", schema = "Civitai", indexes = {
        @Index(name = "idx_vp_parent", columnList = "parent_path"),
        @Index(name = "idx_vp_last", columnList = "last_accessed_at")
})
public class VisitedPath_Table_Entity {

    @Id
    @Column(name = "path", length = 768, nullable = false)
    private String path; // canonical full path (normalized)

    @Column(name = "parent_path", length = 768, nullable = false)
    private String parentPath; // canonical parent

    @Column(name = "drive", length = 10)
    private String drive; // e.g. "F", "G", "\\\\", "/"

    @Column(name = "context", nullable = true)
    private String context;

    // DB-managed timestamps (DEFAULT CURRENT_TIMESTAMP / ON UPDATE
    // CURRENT_TIMESTAMP)
    @Column(name = "first_accessed_at", insertable = false, updatable = false)
    private LocalDateTime firstAccessedAt;

    @Column(name = "last_accessed_at", insertable = false, updatable = false)
    private LocalDateTime lastAccessedAt;

    @Builder.Default
    @Column(name = "access_count", nullable = false)
    private Integer accessCount = 1;

    public enum Context {
        fs, virtual
    }
}