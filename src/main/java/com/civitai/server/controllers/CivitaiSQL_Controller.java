package com.civitai.server.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.civitai.server.models.dto.Models_DTO;
import com.civitai.server.models.dto.Tables_DTO;
import com.civitai.server.models.entities.civitaiSQL.Models_Table_Entity;
import com.civitai.server.services.CivitaiSQL_Service;
import com.civitai.server.utils.CustomResponse;

@RestController
@RequestMapping("/api")
public class CivitaiSQL_Controller {
    // This is where we setup api endpoint or route

    private CivitaiSQL_Service civitaiSQL_Service;

    @Autowired
    public CivitaiSQL_Controller(CivitaiSQL_Service civitaiSQL_Service) {
        this.civitaiSQL_Service = civitaiSQL_Service;
    }

    //Testing Api Route 
    @GetMapping(path = "/testing")
    public ResponseEntity<?> findTesting() {

        return ResponseEntity.ok().body("Testing now");
    }

    //Single Table
    @GetMapping(path = "/find-all-models-table-entities")
    public ResponseEntity<CustomResponse<Map<String, List<Models_Table_Entity>>>> findAllFromModelsTable() {
        Optional<List<Models_Table_Entity>> entityOptional = civitaiSQL_Service.find_all_from_models_table();
        if (entityOptional.isPresent()) {
            List<Models_Table_Entity> entity = entityOptional.get();

            Map<String, List<Models_Table_Entity>> data = new HashMap<>();
            data.put("models", entity);

            return ResponseEntity.ok().body(CustomResponse.success("Model retrieval successful", data));
        } else {
            return ResponseEntity.ok().body(CustomResponse.failure("Model not found"));
        }
    }

    @GetMapping("/models-table-entity/{id}")
    public ResponseEntity<CustomResponse<Map<String, Models_Table_Entity>>> findOneFromModelsTable(
            @PathVariable("id") Integer id) {
        Optional<Models_Table_Entity> entityOptional = civitaiSQL_Service.find_one_from_models_table(id);
        if (entityOptional.isPresent()) {
            Models_Table_Entity entity = entityOptional.get();

            Map<String, Models_Table_Entity> data = new HashMap<>();
            data.put("model", entity);

            return ResponseEntity.ok().body(CustomResponse.success("Model retrieval successful", data));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    //All Table
    @GetMapping(path = "/find-all-entities-in-all-table")
    public ResponseEntity<CustomResponse<Map<String, List<Tables_DTO>>>> findAllEntitiesInAllTables() {
        Optional<List<Tables_DTO>> entityOptional = civitaiSQL_Service.find_all_from_all_tables();
        if (entityOptional.isPresent()) {
            List<Tables_DTO> entity = entityOptional.get();

            Map<String, List<Tables_DTO>> data = new HashMap<>();
            data.put("models", entity);

            return ResponseEntity.ok().body(CustomResponse.success("Model retrieval successful", data));
        } else {
            return ResponseEntity.ok().body(CustomResponse.failure("Model not found"));
        }
    }

    @GetMapping(path = "/all-tables-tables-dto/{id}")
    public ResponseEntity<CustomResponse<Map<String, Tables_DTO>>> findOneTablesDTOFromAllTable(
            @PathVariable("id") Integer id) {
        Optional<Tables_DTO> entityOptional = civitaiSQL_Service.find_one_tables_DTO_from_all_tables(id);
        if (entityOptional.isPresent()) {
            Tables_DTO entity = entityOptional.get();

            Map<String, Tables_DTO> data = new HashMap<>();
            data.put("model", entity);

            return ResponseEntity.ok().body(CustomResponse.success("Model retrieval successful", data));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(path = "/all-tables-models-dto/{id}")
    public ResponseEntity<CustomResponse<Map<String, Models_DTO>>> findOneModelsDTOFromAllTable(
            @PathVariable("id") Integer id) {
        Optional<Models_DTO> entityOptional = civitaiSQL_Service.find_one_models_DTO_from_all_tables(id);
        if (entityOptional.isPresent()) {
            Models_DTO entity = entityOptional.get();

            Map<String, Models_DTO> data = new HashMap<>();
            data.put("model", entity);

            return ResponseEntity.ok().body(CustomResponse.success("Model retrieval successful", data));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(path = "/get-category-list")
    public ResponseEntity<CustomResponse<Map<String, List<String>>>> getCategoryList() {
        Optional<List<String>> categoriesOptional = civitaiSQL_Service.find_all_categories();
        if (categoriesOptional.isPresent()) {
            List<String> categories = categoriesOptional.get();

            Map<String, List<String>> data = new HashMap<>();
            data.put("category_list", categories);

            return ResponseEntity.ok().body(CustomResponse.success("Category list retrieval successful", data));
        } else {
            return ResponseEntity.ok().body(CustomResponse.failure("Model not found"));
        }
    }

    @GetMapping(path = "/find-latest-three-all-tables-models-dto")
    public ResponseEntity<CustomResponse<Map<String, Map<String, List<Models_DTO>>>>> findLatestModelsDTOFromAllTable() {

        Optional<Map<String, List<Models_DTO>>> entityOptional = civitaiSQL_Service
                .find_lastest_three_models_DTO_in_each_category_from_all_table();
        if (entityOptional.isPresent()) {
            Map<String, List<Models_DTO>> entity = entityOptional.get();

            Map<String, Map<String, List<Models_DTO>>> data = new HashMap<>();
            data.put("models", entity);

            return ResponseEntity.ok().body(CustomResponse.success("Model retrieval successful", data));
        } else {
            return ResponseEntity.ok().body(CustomResponse.failure("Model not found in the Database"));
        }
    }

    @PostMapping(path = "/find-one-models-dto-from-all-table-by-url")
    public ResponseEntity<CustomResponse<Map<String, Models_DTO>>> findOneModelsDTOFromAllTableByUrl(
            @RequestBody Map<String, Object> requestBody) {
        String url = (String) requestBody.get("url");
        Optional<Models_DTO> entityOptional = civitaiSQL_Service.find_one_models_DTO_from_all_tables_by_url(url);
        if (entityOptional.isPresent()) {
            Models_DTO entity = entityOptional.get();

            Map<String, Models_DTO> data = new HashMap<>();
            data.put("model", entity);

            return ResponseEntity.ok().body(CustomResponse.success("Model retrieval successful", data));
        } else {
            return ResponseEntity.ok().body(CustomResponse.failure("Model not found in the Database"));
        }
    }

    @PostMapping(path = "/find-list-of-models-dto-from-all-table-by-url")
    public ResponseEntity<CustomResponse<Map<String, List<Models_DTO>>>> findListofModelsDTOFromAllTableByUrl(
            @RequestBody Map<String, Object> requestBody) {
        String url = (String) requestBody.get("url");
        Optional<List<Models_DTO>> entityOptional = civitaiSQL_Service
                .find_List_of_models_DTO_from_all_tables_by_url(url);

        if (entityOptional.isPresent()) {
            List<Models_DTO> entity = entityOptional.get();

            Map<String, List<Models_DTO>> data = new HashMap<>();
            data.put("models", entity);

            return ResponseEntity.ok().body(CustomResponse.success("Model retrieval successful", data));
        } else {
            return ResponseEntity.ok().body(CustomResponse.failure("Model not found in the Database"));
        }
    }

    @PostMapping("/create-record-to-all-tables")
    public ResponseEntity<CustomResponse<String>> createRecordToAllTables(
            @RequestBody Map<String, Object> requestBody) {

        String category = (String) requestBody.get("category");
        String url = (String) requestBody.get("url");

        Optional<Models_DTO> entityOptional = civitaiSQL_Service.create_models_DTO_by_Url(url, category);

        if (entityOptional.isPresent()) {
            Models_DTO models_DTO = entityOptional.get();

            System.out.println(models_DTO);

            civitaiSQL_Service.create_record_to_all_tables(models_DTO);
            return ResponseEntity.ok().body(CustomResponse.success("Model created successfully"));
        } else {
            return ResponseEntity.ok()
                    .body(CustomResponse.failure("Failed to create the model record in the database"));
        }

    }

    @PostMapping("/update-record-to-all-tables-by")
    public ResponseEntity<CustomResponse<String>> updateRecordToAllTables(
            @RequestBody Map<String, Object> requestBody) {

        String category = (String) requestBody.get("category");
        String url = (String) requestBody.get("url");
        Integer id = (Integer) requestBody.get("id");

        Optional<Models_DTO> newUpdateEntityOptional = civitaiSQL_Service.create_models_DTO_by_Url(url, category);

        Optional<Models_Table_Entity> mySQLEntityOptional = civitaiSQL_Service.find_one_from_models_table(id);

        if (newUpdateEntityOptional.isPresent() && mySQLEntityOptional.isPresent()) {
            Models_DTO models_DTO = newUpdateEntityOptional.get();

            civitaiSQL_Service.update_record_to_all_tables_by_id(models_DTO, id);
            return ResponseEntity.ok().body(CustomResponse.success("Model Updated successfully"));
        } else {
            return ResponseEntity.ok()
                    .body(CustomResponse.failure("Model not found in the database"));
        }
    }

    @PostMapping("/delete-record-to-all-tables")
    public ResponseEntity<CustomResponse<String>> deleteRecordToAllTables(
            @RequestBody Map<String, Object> requestBody) {

        Integer id = (Integer) requestBody.get("id");

        Optional<Models_Table_Entity> entityOptional = civitaiSQL_Service.find_one_from_models_table(id);

        if (entityOptional.isPresent()) {

            civitaiSQL_Service.delete_record_to_all_table_by_id(id);
            return ResponseEntity.ok().body(CustomResponse.success("Model Deleted successfully"));
        } else {
            return ResponseEntity.ok()
                    .body(CustomResponse.failure("Model not found in the database"));
        }

    }
}
