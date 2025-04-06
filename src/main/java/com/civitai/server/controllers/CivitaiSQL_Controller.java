package com.civitai.server.controllers;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.civitai.server.models.dto.Models_DTO;
import com.civitai.server.models.dto.Tables_DTO;
import com.civitai.server.models.entities.civitaiSQL.Models_Table_Entity;
import com.civitai.server.models.entities.civitaiSQL.Models_Urls_Table_Entity;
import com.civitai.server.services.CivitaiSQL_Service;
import com.civitai.server.services.Civitai_Service;
import com.civitai.server.services.File_Service;
import com.civitai.server.utils.CustomResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

@RestController
@RequestMapping("/api")
public class CivitaiSQL_Controller {
    // This is where we setup api endpoint or route

    private CivitaiSQL_Service civitaiSQL_Service;
    private Civitai_Service civitai_Service;

    @Autowired
    public CivitaiSQL_Controller(CivitaiSQL_Service civitaiSQL_Service, Civitai_Service civitai_Service) {
        this.civitaiSQL_Service = civitaiSQL_Service;
        this.civitai_Service = civitai_Service;
    }

    //Testing Api Route 
    @GetMapping(path = "/testing")
    public ResponseEntity<?> findTesting() {

        return ResponseEntity.ok().body("Testing now");
    }

    //Single Table
    @GetMapping(path = "/verify-connecting-database")
    public ResponseEntity<CustomResponse<Map<String, List<Models_Table_Entity>>>> verifyConnectingDatabase() {
        return ResponseEntity.ok().body(CustomResponse.success("Model retrieval successful"));
    }

    //Single Table
    @GetMapping(path = "/find-all-models-table-entities")
    public ResponseEntity<CustomResponse<Map<String, List<Models_Table_Entity>>>> findAllFromModelsTable() {
        Optional<List<Models_Table_Entity>> entityOptional = civitaiSQL_Service.find_all_from_models_table();
        if (entityOptional.isPresent()) {
            List<Models_Table_Entity> entity = entityOptional.get();

            Map<String, List<Models_Table_Entity>> payload = new HashMap<>();
            payload.put("modelsList", entity);

            return ResponseEntity.ok().body(CustomResponse.success("Model retrieval successful", payload));
        } else {
            return ResponseEntity.ok().body(CustomResponse.failure("Model not found"));
        }
    }

    @GetMapping("/models-table-entity/{id}")
    public ResponseEntity<CustomResponse<Map<String, Models_Table_Entity>>> findOneFromModelsTable(
            @PathVariable("id") Integer id) {

        // Validate null or empty
        if (id == null) {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        Optional<Models_Table_Entity> entityOptional = civitaiSQL_Service.find_one_from_models_table(id);
        if (entityOptional.isPresent()) {
            Models_Table_Entity entity = entityOptional.get();

            Map<String, Models_Table_Entity> payload = new HashMap<>();
            payload.put("model", entity);

            return ResponseEntity.ok().body(CustomResponse.success("Model retrieval successful", payload));
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

            Map<String, List<Tables_DTO>> payload = new HashMap<>();
            payload.put("modelsList", entity);

            return ResponseEntity.ok().body(CustomResponse.success("Model retrieval successful", payload));
        } else {
            return ResponseEntity.ok().body(CustomResponse.failure("Model not found"));
        }
    }

    @GetMapping(path = "/all-tables-tables-dto/{id}")
    public ResponseEntity<CustomResponse<Map<String, Tables_DTO>>> findOneTablesDTOFromAllTable(
            @PathVariable("id") Integer id) {

        // Validate null or empty
        if (id == null) {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        Optional<Tables_DTO> entityOptional = civitaiSQL_Service.find_one_tables_DTO_from_all_tables(id);
        if (entityOptional.isPresent()) {
            Tables_DTO entity = entityOptional.get();

            Map<String, Tables_DTO> payload = new HashMap<>();
            payload.put("model", entity);

            return ResponseEntity.ok().body(CustomResponse.success("Model retrieval successful", payload));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(path = "/all-tables-models-dto/{id}")
    public ResponseEntity<CustomResponse<Map<String, Models_DTO>>> findOneModelsDTOFromAllTable(
            @PathVariable("id") Integer id) {

        // Validate null or empty
        if (id == null) {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        Optional<Models_DTO> entityOptional = civitaiSQL_Service.find_one_models_DTO_from_all_tables_by_id(id);
        if (entityOptional.isPresent()) {
            Models_DTO entity = entityOptional.get();

            Map<String, Models_DTO> payload = new HashMap<>();
            payload.put("model", entity);

            return ResponseEntity.ok().body(CustomResponse.success("Model retrieval successful", payload));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(path = "/get-categories-list")
    public ResponseEntity<CustomResponse<Map<String, List<String>>>> getCategoryList() {
        Optional<List<String>> categoriesOptional = civitaiSQL_Service.find_all_categories();
        if (categoriesOptional.isPresent()) {
            List<String> categories = categoriesOptional.get();

            Map<String, List<String>> payload = new HashMap<>();
            payload.put("categoriesList", categories);

            return ResponseEntity.ok().body(CustomResponse.success("Category list retrieval successful", payload));
        } else {
            return ResponseEntity.ok().body(CustomResponse.failure("Model not found"));
        }
    }

    @GetMapping(path = "/find-latest-three-models-dto-from-all-tables")
    public ResponseEntity<CustomResponse<Map<String, Map<String, List<Models_DTO>>>>> findLatestThreeModelsDTOfromAllTables() {

        Optional<Map<String, List<Models_DTO>>> entityOptional = civitaiSQL_Service
                .find_lastest_three_models_DTO_in_each_category_from_all_table();
        if (entityOptional.isPresent()) {
            Map<String, List<Models_DTO>> entity = entityOptional.get();

            Map<String, Map<String, List<Models_DTO>>> payload = new HashMap<>();
            payload.put("modelsList", entity);

            return ResponseEntity.ok().body(CustomResponse.success("Model retrieval successful", payload));
        } else {
            return ResponseEntity.ok().body(CustomResponse.failure("Model not found in the Database"));
        }
    }

    @CrossOrigin(origins = "*")
    @PostMapping(path = "/find-version-numbers-for-model")
    public ResponseEntity<CustomResponse<Map<String, List<String>>>> findVersionNumbersForModel(
            @RequestBody Map<String, Object> requestBody) {

        String modelNumber = (String) requestBody.get("modelNumber");
        List<String> versionNumbers = ((List<?>) requestBody.get("versionNumbers")).stream()
                .map(Object::toString) // Ensures each element is treated as a String
                .collect(Collectors.toList());

        // Validate null or empty
        if (modelNumber == null || modelNumber.isEmpty() || versionNumbers == null || versionNumbers.isEmpty()) {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        Optional<List<String>> versionExistenceMapOptional = civitaiSQL_Service
                .find_version_numbers_for_model(modelNumber, versionNumbers);

        Map<String, List<String>> payload = new HashMap<>();
        if (versionExistenceMapOptional.isPresent()) {
            List<String> versionExistenceMap = versionExistenceMapOptional.get();
            payload.put("existedVersionsList", versionExistenceMap);

            return ResponseEntity.ok()
                    .body(CustomResponse.success("Version number check successful", payload));
        } else {
            // Return success with an empty list when no models are found
            payload.put("existedVersionsList", new ArrayList<>());
            return ResponseEntity.ok()
                    .body(CustomResponse.success("No existing versions found", payload));
        }
    }

    @PostMapping(path = "/find-one-models-dto-from-all-table-by-url")
    public ResponseEntity<CustomResponse<Map<String, Models_DTO>>> findOneModelsDTOFromAllTableByUrl(
            @RequestBody Map<String, Object> requestBody) {
        String url = (String) requestBody.get("url");

        // Validate null or empty
        if (url == null || url == "") {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        Optional<Models_DTO> entityOptional = civitaiSQL_Service.find_one_models_DTO_from_all_tables_by_url(url);
        if (entityOptional.isPresent()) {
            Models_DTO entity = entityOptional.get();

            Map<String, Models_DTO> payload = new HashMap<>();
            payload.put("model", entity);

            return ResponseEntity.ok().body(CustomResponse.success("Model retrieval successful", payload));
        } else {
            return ResponseEntity.ok().body(CustomResponse.failure("Model not found in the Database"));
        }
    }

    @PostMapping(path = "/check-if-url-exist-in-database")
    public ResponseEntity<CustomResponse<Map<String, Boolean>>> checkIfUrlExistInDatabase(
            @RequestBody Map<String, Object> requestBody) {
        String url = (String) requestBody.get("url");

        // Validate null or empty
        if (url == null || url == "") {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }
        Boolean isSaved = civitaiSQL_Service.find_one_from_models_urls_table(url);
        if (isSaved != null) {
            Map<String, Boolean> payload = new HashMap<>();
            payload.put("isSaved", isSaved);

            return ResponseEntity.ok().body(CustomResponse.success("Model retrieval successful", payload));
        } else {
            return ResponseEntity.ok().body(CustomResponse.failure("Model not found in the Database"));
        }
    }

    @PostMapping(path = "/check-quantity-of-url-in-database-by-url")
    public ResponseEntity<CustomResponse<Map<String, Long>>> checkQuantityOfUrlInDatabaseByUrl(
            @RequestBody Map<String, Object> requestBody) {
        String url = (String) requestBody.get("url");

        // Validate null or empty
        if (url == null || url == "") {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }
        Long quantity = civitaiSQL_Service.find_quantity_from_models_urls_table(url);

        Map<String, Long> payload = new HashMap<>();
        payload.put("quantity", quantity);

        return ResponseEntity.ok().body(CustomResponse.success("Model retrieval successful", payload));
    }

    @PostMapping(path = "/check-quantity-of-url-in-database-by-modelID")
    public ResponseEntity<CustomResponse<Map<String, Long>>> checkQuantityOfUrlInDatabaseByModelID(
            @RequestBody Map<String, Object> requestBody) {
        String url = (String) requestBody.get("url");

        // Validate null or empty
        if (url == null || url == "") {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }
        Long quantity = civitaiSQL_Service.find_quantity_from_models_table(url);

        Map<String, Long> payload = new HashMap<>();
        payload.put("quantity", quantity);

        return ResponseEntity.ok().body(CustomResponse.success("Model retrieval successful", payload));
    }

    @PostMapping(path = "/check-if-model-update-avaliable")
    public ResponseEntity<CustomResponse<Map<String, Boolean>>> checkIfUpdateAvaliable(
            @RequestBody Map<String, Object> requestBody) {
        String url = (String) requestBody.get("url");

        // Validate null or empty
        if (url == null || url == "") {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        Optional<List<String>> versionListOptional = civitaiSQL_Service
                .find_List_of_Version_Number_from_model_tables_by_Url(url);

        String modelID = url.replaceAll(".*/models/(\\d+).*", "$1");
        Optional<Map<String, Object>> modelOptional = civitai_Service
                .findModelByModelID(modelID);

        //GET LATEST FROM CIVITAI
        Map<String, Object> model = modelOptional.get();
        String latestVersionNumber = null;

        //Retriving the version list 
        Optional<List<Map<String, Object>>> modelVersionList = Optional
                .ofNullable(model)
                .map(map -> (List<Map<String, Object>>) map
                        .get("modelVersions"))
                .filter(list -> !list.isEmpty());

        if (modelOptional.isPresent()) {

            //For Early Access
            Boolean isEarlyAccess = false;
            try {
                String availability = modelVersionList
                        .map(list -> list.get(0))
                        .filter(firstObject -> firstObject.containsKey("availability"))
                        .map(firstObject -> firstObject.get("availability").toString())
                        .orElse(null);

                if ("EarlyAccess".equalsIgnoreCase(availability)) {
                    isEarlyAccess = true;
                } else {
                    isEarlyAccess = false;
                }
            } catch (Exception e) {
                isEarlyAccess = false;
            }

            //For Version Number
            Boolean isUpdateAvaliable = false;

            try {
                URI uri = new URI(url);
                String query = uri.getQuery();
                if (query != null && query.contains("modelVersionId")) {
                    String[] queryParams = query.split("&");
                    for (String param : queryParams) {
                        if (param.startsWith("modelVersionId=")) {
                            latestVersionNumber = param
                                    .substring("modelVersionId=".length());
                        }
                    }
                } else {
                    latestVersionNumber = modelVersionList
                            .map(list -> list.stream()
                                    .map(map -> map.get("id")) // Extract the id object
                                    .filter(Objects::nonNull) // Ensure it's not null
                                    .map(Object::toString) // Convert to String
                                    .map(Integer::valueOf) // Convert to Integer for comparison
                                    .max(Integer::compareTo) // Get the maximum ID
                                    .map(String::valueOf) // Convert back to String
                                    .orElse(null))
                            .orElse(null);

                }
            } catch (Exception e) {
                latestVersionNumber = null;
            }

            //GET LATEST FROM DB

            if (versionListOptional.isPresent() && latestVersionNumber != null) {
                List<String> entityList = versionListOptional.get();
                OptionalInt maxOptional = entityList.stream()
                        .mapToInt(Integer::parseInt)
                        .max();
                int maxVersionNumber = maxOptional.getAsInt();

                if (Integer.parseInt(latestVersionNumber) > maxVersionNumber) {
                    isUpdateAvaliable = true;
                }
            }

            Map<String, Boolean> payload = new HashMap<>();
            payload.put("isUpdateAvaliable", isUpdateAvaliable);
            payload.put("isEarlyAccess", isEarlyAccess);

            return ResponseEntity.ok().body(CustomResponse.success("Model retrieval successful", payload));
        } else {
            return ResponseEntity.ok().body(CustomResponse.failure("Failed calling Civitai API"));
        }
    }

    @PostMapping(path = "/find-list-of-models-dto-from-all-table-by-modelID")
    public ResponseEntity<CustomResponse<Map<String, List<Models_DTO>>>> findListofModelsDTOFromAllTableByModelID(
            @RequestBody Map<String, Object> requestBody) {
        String modelID = (String) requestBody.get("modelID");

        // Validate null or empty
        if (modelID == null || modelID == "") {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        Optional<List<Models_DTO>> entityOptional = civitaiSQL_Service
                .find_List_of_models_DTO_from_all_tables_by_modelID(modelID);

        if (entityOptional.isPresent()) {
            List<Models_DTO> entity = entityOptional.get();

            Map<String, List<Models_DTO>> payload = new HashMap<>();
            payload.put("modelsList", entity);

            return ResponseEntity.ok().body(CustomResponse.success("Model retrieval successful", payload));
        } else {
            return ResponseEntity.ok().body(CustomResponse.failure("Model not found in the Database"));
        }
    }

    @PostMapping(path = "/find-list-of-models-dto-from-all-table-by-name")
    public ResponseEntity<CustomResponse<Map<String, List<Models_DTO>>>> findListofModelsDTOfromAllTableByName(
            @RequestBody Map<String, Object> requestBody) {
        String name = ((String) requestBody.get("name")).toLowerCase();

        // Validate null or empty
        if (name == null || name == "") {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        List<Models_DTO> combinedList = new ArrayList<>();

        Optional<List<Models_DTO>> nameEntityOptional = civitaiSQL_Service
                .find_List_of_models_DTO_from_all_tables_by_alike_name(name);
        if (nameEntityOptional.isPresent()) {
            List<Models_DTO> entityList = nameEntityOptional.get();
            combinedList.addAll(entityList);
            //System.out.println("name: " + entityList.size());
        }

        Optional<List<Models_DTO>> tagsEntityOptional = civitaiSQL_Service
                .find_List_of_models_DTO_from_all_tables_by_alike_tags(name);
        if (tagsEntityOptional.isPresent()) {
            List<Models_DTO> entityList = tagsEntityOptional.get();
            combinedList.addAll(entityList);
            //System.out.println("tags: " + entityList.size());
        }

        Optional<List<Models_DTO>> triggerWordsEntityOptional = civitaiSQL_Service
                .find_List_of_models_DTO_from_all_tables_by_alike_triggerWords(name);
        if (triggerWordsEntityOptional.isPresent()) {
            List<Models_DTO> entityList = triggerWordsEntityOptional.get();
            combinedList.addAll(entityList);
            //System.out.println("triggerWords: " + entityList.size());
        }

        Optional<List<Models_DTO>> urlEntityOptional = civitaiSQL_Service
                .find_List_of_models_DTO_from_all_tables_by_alike_url(name);
        if (urlEntityOptional.isPresent()) {
            List<Models_DTO> entityList = urlEntityOptional.get();
            combinedList.addAll(entityList);
            //System.out.println("url: " + entityList.size());
        }

        List<Models_DTO> distinctList = combinedList.stream()
                .collect(Collectors.toMap(
                        Models_DTO::getId, // Assuming Models_DTO has getId() method
                        Function.identity(),
                        (existing, replacement) -> existing // Merge function, keep the existing element
                ))
                .values()
                .stream()
                .collect(Collectors.toList());

        if (!distinctList.isEmpty()) {

            Map<String, List<Models_DTO>> payload = new HashMap<>();
            payload.put("modelsList", distinctList);

            return ResponseEntity.ok().body(CustomResponse.success("Model retrieval successful", payload));
        } else {
            return ResponseEntity.ok().body(CustomResponse.failure("No Model found in the database"));
        }
    }

    @PostMapping(path = "/find-list-of-models-dto-from-all-table-by-tagsList")
    @SuppressWarnings("unchecked")
    public ResponseEntity<CustomResponse<Map<String, List<Models_DTO>>>> findListofModelsDTOfromAllTableByTagsList(
            @RequestBody Map<String, Object> requestBody) {

        List<String> tagsList = (List<String>) requestBody.get("tagsList");

        // Validate null or empty
        if (tagsList == null || tagsList.isEmpty()) {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        Optional<List<Models_DTO>> tagsListEntityOptional = civitaiSQL_Service
                .find_List_of_models_DTO_from_all_tables_by_alike_tagsList(tagsList);

        if (tagsListEntityOptional.isPresent()) {

            List<Models_DTO> entityList = tagsListEntityOptional.get();

            Map<String, List<Models_DTO>> payload = new HashMap<>();
            payload.put("modelsList", entityList);

            return ResponseEntity.ok().body(CustomResponse.success("Model retrieval successful", payload));
        } else {
            return ResponseEntity.ok().body(CustomResponse.failure("No Model found in the database"));
        }
    }

    //tempermonkey use only
    @CrossOrigin(origins = "https://civitai.com")
    @PostMapping(path = "/find-list-of-models-dto-from-all-table-by-tagsList-tampermonkey")
    @SuppressWarnings("unchecked")
    public ResponseEntity<CustomResponse<Map<String, List<Models_DTO>>>> findListofModelsDTOfromAllTableByTagsListForTamperMonkey(
            @RequestBody Map<String, Object> requestBody) {

        List<String> tagsList = (List<String>) requestBody.get("tagsList");

        // Validate null or empty
        if (tagsList == null || tagsList.isEmpty()) {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        Optional<List<Models_DTO>> tagsListEntityOptional = civitaiSQL_Service
                .find_List_of_models_DTO_from_all_tables_by_alike_tagsList(tagsList);

        if (tagsListEntityOptional.isPresent()) {

            List<Models_DTO> entityList = tagsListEntityOptional.get();

            Map<String, List<Models_DTO>> payload = new HashMap<>();
            payload.put("modelsList", entityList);

            return ResponseEntity.ok().body(CustomResponse.success("Model retrieval successful", payload));
        } else {
            return ResponseEntity.ok().body(CustomResponse.failure("No Model found in the database"));
        }
    }

    @PostMapping("/create-record-to-all-tables")
    public ResponseEntity<CustomResponse<String>> createRecordToAllTables(
            @RequestBody Map<String, Object> requestBody) {

        String category = (String) requestBody.get("category");
        String url = (String) requestBody.get("url");

        // Validate null or empty
        if (category == null || category.isEmpty() || url == null || url == "") {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        //Fetch the Civitai Model Data then create a new Models_DTO
        Optional<Models_DTO> entityOptional = civitaiSQL_Service.create_models_DTO_by_Url(url, category);

        if (entityOptional.isPresent()) {
            Models_DTO models_DTO = entityOptional.get();
            civitaiSQL_Service.create_record_to_all_tables(models_DTO);

            System.out.println(models_DTO.getName() + " has been added into the database.");

            return ResponseEntity.ok().body(CustomResponse.success("Model created successfully"));
        } else {
            return ResponseEntity.ok()
                    .body(CustomResponse.failure("Failed to create the model record in the database"));
        }

    }

    @PostMapping("/update-local-path")
    public ResponseEntity<CustomResponse<String>> updateLocalPath(
            @RequestBody Map<String, Object> requestBody) {

        List<Map<String, Object>> fileArray = (List<Map<String, Object>>) requestBody.get("fileArray");
        String localPath = (String) requestBody.get("localPath");

        int updatedCount = civitaiSQL_Service.updateLocalPath(fileArray, localPath);

        return ResponseEntity.ok().body(CustomResponse.success("Model Updated successfully"));
    }

    @PostMapping("/scan-local-files")
    public ResponseEntity<CustomResponse<Map<String, List<Models_DTO>>>> searchModels(
            @RequestBody Map<String, Object> requestBody) {
        // Expecting the requestBody to contain "compositeList", a list of maps each with "modelID" and "versionID"
        List<Map<String, String>> compositeList = (List<Map<String, String>>) requestBody.get("compositeList");
        Optional<List<Models_DTO>> modelsOptional = civitaiSQL_Service
                .findListOfModelsDTOByModelAndVersion(compositeList);
        if (modelsOptional.isPresent()) {
            List<Models_DTO> entityList = modelsOptional.get();

            Map<String, List<Models_DTO>> payload = new HashMap<>();
            payload.put("modelsList", entityList);

            return ResponseEntity.ok().body(CustomResponse.success("Model retrieval successful", payload));
        } else {
            return ResponseEntity.ok().body(CustomResponse.failure("No Model found in the database"));
        }
    }

    @PostMapping("/update-record-to-all-tables")
    public ResponseEntity<CustomResponse<String>> updateRecordToAllTables(
            @RequestBody Map<String, Object> requestBody) {

        String category = (String) requestBody.get("category");
        String url = (String) requestBody.get("url");
        Integer id = (Integer) requestBody.get("id");

        // Validate null or empty
        if (category == null || category.isEmpty() || url == null || url == "" || id == null) {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        //Fetch the Civitai Model Data then create a new Models_DTO
        Optional<Models_DTO> newUpdateEntityOptional = civitaiSQL_Service.create_models_DTO_by_Url(url, category);

        //Find if the model exists in the database
        Optional<Models_Table_Entity> mySQLEntityOptional = civitaiSQL_Service.find_one_from_models_table(id);

        if (newUpdateEntityOptional.isPresent() && mySQLEntityOptional.isPresent()) {
            Models_DTO models_DTO = newUpdateEntityOptional.get();

            civitaiSQL_Service.update_record_to_all_tables_by_id(models_DTO, id);

            System.out.println(models_DTO.getName() + " has been updated.");

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

        // Validate null or empty
        if (id == null) {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        //Find if the model exists in the database
        Optional<Models_Table_Entity> entityOptional = civitaiSQL_Service.find_one_from_models_table(id);

        if (entityOptional.isPresent()) {

            civitaiSQL_Service.delete_record_to_all_table_by_id(id);

            System.out.println(entityOptional.get().getName() + " has been deleted.");

            return ResponseEntity.ok().body(CustomResponse.success("Model Deleted successfully"));
        } else {
            return ResponseEntity.ok()
                    .body(CustomResponse.failure("Model not found in the database"));
        }
    }

    @PostMapping("/update-record-by-model-and-version")
    public ResponseEntity<CustomResponse<String>> updateRecordByModelAndVersion(
            @RequestBody Map<String, Object> requestBody) {
        try {
            // Retrieve required parameters from the request body
            String modelId = (String) requestBody.get("modelId"); // corresponds to modelNumber
            String versionId = (String) requestBody.get("versionId"); // corresponds to versionNumber
            List<String> fieldsToUpdate = (List<String>) requestBody.get("fieldsToUpdate");

            // Validate required input parameters
            if (modelId == null || modelId.isEmpty() ||
                    versionId == null || versionId.isEmpty() ||
                    fieldsToUpdate == null || fieldsToUpdate.isEmpty()) {
                return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
            }

            // Build the DTO based on only the fields specified in fieldsToUpdate.
            Models_DTO models_DTO = new Models_DTO();
            if (fieldsToUpdate.contains("name") && requestBody.get("name") != null) {
                models_DTO.setName((String) requestBody.get("name"));
            }
            if (fieldsToUpdate.contains("mainModelName") && requestBody.get("mainModelName") != null) {
                models_DTO.setMainModelName((String) requestBody.get("mainModelName"));
            }
            if (fieldsToUpdate.contains("tags") && requestBody.get("tags") != null) {
                models_DTO.setTags((List<String>) requestBody.get("tags"));
            }
            if (fieldsToUpdate.contains("localTags") && requestBody.get("localTags") != null) {
                models_DTO.setLocalTags((List<String>) requestBody.get("localTags"));
            }
            if (fieldsToUpdate.contains("aliases") && requestBody.get("aliases") != null) {
                models_DTO.setAliases((List<String>) requestBody.get("aliases"));
            }
            if (fieldsToUpdate.contains("versionNumber") && requestBody.get("versionNumber") != null) {
                models_DTO.setVersionNumber((String) requestBody.get("versionNumber"));
            }
            if (fieldsToUpdate.contains("modelNumber") && requestBody.get("modelNumber") != null) {
                models_DTO.setModelNumber((String) requestBody.get("modelNumber"));
            }
            if (fieldsToUpdate.contains("triggerWords") && requestBody.get("triggerWords") != null) {
                models_DTO.setTriggerWords((List<String>) requestBody.get("triggerWords"));
            }
            if (fieldsToUpdate.contains("description") && requestBody.get("description") != null) {
                models_DTO.setDescription((String) requestBody.get("description"));
            }
            if (fieldsToUpdate.contains("type") && requestBody.get("type") != null) {
                models_DTO.setType((String) requestBody.get("type"));
            }
            if (fieldsToUpdate.contains("stats") && requestBody.get("stats") != null) {
                Object statsObj = requestBody.get("stats");
                String statsString;
                if (statsObj instanceof String) {
                    statsString = (String) statsObj;
                } else {
                    // Convert the LinkedHashMap (or any object) to a JSON string.
                    ObjectMapper objectMapper = new ObjectMapper();
                    try {
                        statsString = objectMapper.writeValueAsString(statsObj);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Error serializing stats field", e);
                    }
                }
                models_DTO.setStats(statsString);
            }
            if (fieldsToUpdate.contains("localPath") && requestBody.get("localPath") != null) {
                models_DTO.setLocalPath((String) requestBody.get("localPath"));
            }
            if (fieldsToUpdate.contains("uploaded") && requestBody.get("uploaded") != null) {
                // Assuming uploaded is passed as a String in ISO format (yyyy-MM-dd)
                models_DTO.setUploaded(LocalDate.parse((String) requestBody.get("uploaded")));
            }
            if (fieldsToUpdate.contains("baseModel") && requestBody.get("baseModel") != null) {
                models_DTO.setBaseModel((String) requestBody.get("baseModel"));
            }
            if (fieldsToUpdate.contains("hash") && requestBody.get("hash") != null) {
                models_DTO.setHash((String) requestBody.get("hash"));
            }
            if (fieldsToUpdate.contains("usageTips") && requestBody.get("usageTips") != null) {
                models_DTO.setUsageTips((String) requestBody.get("usageTips"));
            }
            if (fieldsToUpdate.contains("creatorName") && requestBody.get("creatorName") != null) {
                models_DTO.setCreatorName((String) requestBody.get("creatorName"));
            }
            if (fieldsToUpdate.contains("nsfw") && requestBody.get("nsfw") != null) {
                models_DTO.setNsfw((Boolean) requestBody.get("nsfw"));
            }
            if (fieldsToUpdate.contains("flag") && requestBody.get("flag") != null) {
                models_DTO.setFlag((Boolean) requestBody.get("flag"));
            }
            if (fieldsToUpdate.contains("urlAccessable") && requestBody.get("urlAccessable") != null) {
                models_DTO.setUrlAccessable((Boolean) requestBody.get("urlAccessable"));
            }
            if (fieldsToUpdate.contains("imageUrls") && requestBody.get("imageUrls") != null) {
                models_DTO.setImageUrls((List<Map<String, Object>>) requestBody.get("imageUrls"));
            }

            System.out.println("HELLO");
            System.out.println(models_DTO);

            // Look up the record using modelId and versionId (i.e. modelNumber and versionNumber)
            Optional<Models_Table_Entity> mySQLEntityOptional = civitaiSQL_Service
                    .find_one_from_models_table_by_model_and_version(modelId, versionId);

            if (mySQLEntityOptional.isPresent()) {
                // Retrieve the primary key from the found record and update all related tables.
                Integer id = mySQLEntityOptional.get().getId();
                civitaiSQL_Service.update_record_to_all_tables_by_model_and_version(models_DTO, id, fieldsToUpdate);
                System.out.println(
                        "Record with modelId " + modelId + " and versionId " + versionId + " has been updated.");
                return ResponseEntity.ok().body(CustomResponse.success("Model updated successfully"));
            } else {
                return ResponseEntity.ok().body(CustomResponse.failure("Model not found in the database"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CustomResponse.failure("An error occurred: " + e.getMessage()));
        }
    }

    @PostMapping("/find-virtual-files")
    public ResponseEntity<CustomResponse<List<Map<String, Object>>>> findVirtualFiles(
            @RequestBody Map<String, String> requestBody) {

        String path = requestBody.get("path");
        if (path == null || path.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(CustomResponse.failure("Provided path is empty or null"));
        }

        Optional<List<Map<String, Object>>> filesOptional = civitaiSQL_Service.findVirtualFiles(path);

        return filesOptional
                .map(files -> ResponseEntity.ok(CustomResponse.success("Virtual files retrieved successfully", files)))
                .orElseGet(() -> ResponseEntity.ok(CustomResponse.failure("No virtual files found")));
    }

    @PostMapping("/find-virtual-directories")
    public ResponseEntity<CustomResponse<List<Map<String, String>>>> findVirtualDirectories(
            @RequestBody Map<String, String> requestBody) {

        String path = requestBody.get("path");
        if (path == null || path.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(CustomResponse.failure("Provided path is empty or null"));
        }

        Optional<List<Map<String, String>>> directoriesOptional = civitaiSQL_Service
                .findVirtualDirectoriesWithDrive(path);

        return directoriesOptional
                .map(dirs -> ResponseEntity
                        .ok(CustomResponse.success("Virtual directories retrieved successfully", dirs)))
                .orElseGet(() -> ResponseEntity.ok(CustomResponse.failure("No virtual directories found")));
    }

}
