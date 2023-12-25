package com.civitai.server.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.civitai.server.models.dto.Models_DTO;
import com.civitai.server.models.dto.Tables_DTO;
import com.civitai.server.models.entities.civitaiSQL.Models_Table_Entity;

public interface CivitaiSQL_Service {
    // Single Tables only //
    Optional<List<Models_Table_Entity>> find_all_from_models_table();

    Optional<Models_Table_Entity> find_one_from_models_table(Integer id);

    Optional<List<String>> find_all_categories();

    // All Tables only //
    Optional<List<Tables_DTO>> find_all_from_all_tables();

    Optional<Tables_DTO> find_one_tables_DTO_from_all_tables(Integer id);

    Optional<Models_DTO> find_one_models_DTO_from_all_tables(Integer id);

    Optional<Models_DTO> find_one_models_DTO_from_all_tables_by_url(String url);

    Optional<List<Models_DTO>> find_List_of_models_DTO_from_all_tables_by_url(String url);

    Optional<Map<String, List<Models_DTO>>> find_lastest_three_models_DTO_in_each_category_from_all_table();

    Optional<Models_DTO> create_models_DTO_by_Url(String url, String category);

    // Add a record to all tables
    void create_record_to_all_tables(Models_DTO dto);

    // Update a record to all tables
    void update_record_to_all_tables_by_id(Models_DTO dto, Integer id);

    void delete_record_to_all_table_by_id(Integer id);

    // Add a dummy record to all table
    void create_dummy_record_to_all_tables();

    // Delete latest row from all tables
    void delete_latest_record_from_all_tables();

    // Delete every row from all tables
    void delete_every_record_from_all_tables();

    // Utils
    Models_DTO convertToDTO(Models_Table_Entity entity);

}
