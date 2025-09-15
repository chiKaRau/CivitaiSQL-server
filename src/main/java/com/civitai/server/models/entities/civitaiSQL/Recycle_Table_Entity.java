package com.civitai.server.models.entities.civitaiSQL;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "recycle_records", schema = "Civitai")
public class Recycle_Table_Entity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    // Store enum as lowercase strings ("set" / "directory")
    @Convert(converter = RecordTypeConverter.class)
    @Column(name = "type", nullable = false/* , columnDefinition = "ENUM('set','directory')" */)
    private RecordType type;

    @Column(name = "original_path", length = 1024, nullable = false)
    private String originalPath;

    @Column(name = "deleted_from_path", length = 1024)
    private String deletedFromPath;

    @Column(name = "deleted_date", nullable = false/* , columnDefinition = "DATETIME(3)" */)
    private LocalDateTime deletedDate;

    // Persist as MySQL JSON; exposed as List<String> in Java
    @Builder.Default
    @Convert(converter = FilesJsonConverter.class)
    @Column(name = "files", nullable = false, columnDefinition = "json")
    private List<String> files = new ArrayList<>();

    /* ===== Types & Converters ===== */

    public enum RecordType {
        SET, DIRECTORY
    }

    /**
     * Persist enum as lowercase strings to match SQL values.
     */
    @Converter(autoApply = false)
    public static class RecordTypeConverter implements AttributeConverter<RecordType, String> {
        @Override
        public String convertToDatabaseColumn(RecordType attribute) {
            return attribute == null ? null : attribute.name().toLowerCase();
        }

        @Override
        public RecordType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : RecordType.valueOf(dbData.toUpperCase());
        }
    }

    /**
     * Store List<String> as JSON text (MySQL JSON column).
     */
    @Converter(autoApply = false)
    public static class FilesJsonConverter implements AttributeConverter<List<String>, String> {
        private static final ObjectMapper MAPPER = new ObjectMapper();
        private static final TypeReference<List<String>> TYPE = new TypeReference<List<String>>() {
        };

        @Override
        public String convertToDatabaseColumn(List<String> attribute) {
            try {
                return (attribute == null) ? "[]" : MAPPER.writeValueAsString(attribute);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to serialize files list to JSON", e);
            }
        }

        @Override
        public List<String> convertToEntityAttribute(String dbData) {
            try {
                if (dbData == null || dbData.isEmpty())
                    return new ArrayList<>();
                return MAPPER.readValue(dbData, TYPE);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to deserialize files JSON to List<String>", e);
            }
        }
    }
}