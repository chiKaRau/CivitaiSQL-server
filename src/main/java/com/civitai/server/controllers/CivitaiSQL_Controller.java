package com.civitai.server.controllers;

import java.io.File;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

import com.civitai.server.models.dto.FullModelRecordDTO;
import com.civitai.server.models.dto.Models_DTO;
import com.civitai.server.models.dto.Tables_DTO;
import com.civitai.server.models.entities.civitaiSQL.Models_Table_Entity;
import com.civitai.server.models.entities.civitaiSQL.Models_Urls_Table_Entity;
import com.civitai.server.models.entities.civitaiSQL.Recycle_Table_Entity;
import com.civitai.server.models.entities.civitaiSQL.VisitedPath_Table_Entity;
import com.civitai.server.services.CivitaiSQL_Service;
import com.civitai.server.services.Civitai_Service;
import com.civitai.server.services.File_Service;
import com.civitai.server.utils.CustomResponse;
import com.civitai.server.utils.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

@RestController
@RequestMapping("/api")
public class CivitaiSQL_Controller {
    // This is where we setup api endpoint or route

    private CivitaiSQL_Service civitaiSQL_Service;
    private Civitai_Service civitai_Service;
    private File_Service fileService;

    @Autowired
    public CivitaiSQL_Controller(CivitaiSQL_Service civitaiSQL_Service, Civitai_Service civitai_Service,
            File_Service fileService) {
        this.civitaiSQL_Service = civitaiSQL_Service;
        this.civitai_Service = civitai_Service;
        this.fileService = fileService;
    }

    // Testing Api Route
    @GetMapping(path = "/testing")
    public ResponseEntity<?> findTesting() {

        return ResponseEntity.ok().body("Testing now");
    }

    // Single Table
    @GetMapping(path = "/verify-connecting-database")
    public ResponseEntity<CustomResponse<Map<String, List<Models_Table_Entity>>>> verifyConnectingDatabase() {
        return ResponseEntity.ok().body(CustomResponse.success("Model retrieval successful"));
    }

    // Single Table
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

    // All Table
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

        // GET LATEST FROM CIVITAI
        Map<String, Object> model = modelOptional.get();
        String latestVersionNumber = null;

        // Retriving the version list
        Optional<List<Map<String, Object>>> modelVersionList = Optional
                .ofNullable(model)
                .map(map -> (List<Map<String, Object>>) map
                        .get("modelVersions"))
                .filter(list -> !list.isEmpty());

        if (modelOptional.isPresent()) {

            // For Early Access
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

            // For Version Number
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

            // GET LATEST FROM DB

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
            // System.out.println("name: " + entityList.size());
        }

        Optional<List<Models_DTO>> tagsEntityOptional = civitaiSQL_Service
                .find_List_of_models_DTO_from_all_tables_by_alike_tags(name);
        if (tagsEntityOptional.isPresent()) {
            List<Models_DTO> entityList = tagsEntityOptional.get();
            combinedList.addAll(entityList);
            // System.out.println("tags: " + entityList.size());
        }

        Optional<List<Models_DTO>> triggerWordsEntityOptional = civitaiSQL_Service
                .find_List_of_models_DTO_from_all_tables_by_alike_triggerWords(name);
        if (triggerWordsEntityOptional.isPresent()) {
            List<Models_DTO> entityList = triggerWordsEntityOptional.get();
            combinedList.addAll(entityList);
            // System.out.println("triggerWords: " + entityList.size());
        }

        Optional<List<Models_DTO>> urlEntityOptional = civitaiSQL_Service
                .find_List_of_models_DTO_from_all_tables_by_alike_url(name);
        if (urlEntityOptional.isPresent()) {
            List<Models_DTO> entityList = urlEntityOptional.get();
            combinedList.addAll(entityList);
            // System.out.println("url: " + entityList.size());
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

    // tempermonkey use only
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
        String downloadFilePath = (String) requestBody.get("downloadFilePath");

        if (downloadFilePath == "") {
            downloadFilePath = null;
        } else {
            downloadFilePath = ("F:\\Coding Apps\\CivitaiSQL Server\\server\\files\\download" + File.separator
                    + downloadFilePath.replaceFirst("^/", "").replace("/", File.separator))
                    .replaceAll(java.util.regex.Pattern.quote(File.separator) + "$", "");

            // Convert the file path to a URL format (using forward slashes)
            String fileUrl = downloadFilePath.replace("\\", "/");
            // Create an OSC 8 hyperlink for clickable output.
            String clickableDownloadPath = "\033]8;;file:///" + fileUrl + "\033\\"
                    + downloadFilePath
                    + "\033]8;;\033\\";

            System.out.println("downloadFilePath: " + clickableDownloadPath);

        }

        // Validate null or empty
        if (category == null || category.isEmpty() || url == null || url == "") {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        // Fetch the Civitai Model Data then create a new Models_DTO
        Optional<Models_DTO> entityOptional = civitaiSQL_Service.create_models_DTO_by_Url(url, category,
                downloadFilePath);

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

    @PostMapping("/create-record-to-all-tables-in-custom")
    public ResponseEntity<CustomResponse<String>> createRecordToAllTablesInCustom(
            @RequestBody Models_DTO modelsDTO) {

        // Validate required fields
        if (modelsDTO.getName() == null || modelsDTO.getName().isEmpty()
                || modelsDTO.getMainModelName() == null || modelsDTO.getMainModelName().isEmpty()
                || modelsDTO.getUrl() == null || modelsDTO.getUrl().isEmpty()
                || modelsDTO.getCategory() == null || modelsDTO.getCategory().isEmpty()
                || modelsDTO.getVersionNumber() == null || modelsDTO.getVersionNumber().isEmpty()
                || modelsDTO.getModelNumber() == null || modelsDTO.getModelNumber().isEmpty()
                || modelsDTO.getType() == null || modelsDTO.getType().isEmpty()
                || modelsDTO.getBaseModel() == null || modelsDTO.getBaseModel().isEmpty()
                || modelsDTO.getImageUrls() == null || modelsDTO.getImageUrls().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(CustomResponse.failure("Missing required fields in request body."));
        }

        // Normalize download path if provided
        String localPath = modelsDTO.getLocalPath();
        if (localPath != null && !localPath.isEmpty()) {
            String normalized = localPath.replaceFirst("^/", "");
            String[] parts = normalized.split("/");
            String base = "F:" + File.separator + "Coding Apps" + File.separator
                    + "CivitaiSQL Server" + File.separator + "server" + File.separator
                    + "files" + File.separator + "download";
            for (String p : parts) {
                base = base + File.separator + p;
            }
            modelsDTO.setLocalPath(base);
            System.out.println("downloadFilePath: " + base);
        }

        // Delegate to service to insert into all tables
        civitaiSQL_Service.create_record_to_all_tables(modelsDTO);

        return ResponseEntity.ok(CustomResponse.success("Model created successfully"));
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
        // Expecting the requestBody to contain "compositeList", a list of maps each
        // with "modelID" and "versionID"
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

        // Fetch the Civitai Model Data then create a new Models_DTO
        Optional<Models_DTO> newUpdateEntityOptional = civitaiSQL_Service.create_models_DTO_by_Url(url, category, null);

        // Find if the model exists in the database
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

        // Find if the model exists in the database
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

    @PostMapping("/delete-record-by-model-version")
    public ResponseEntity<CustomResponse<String>> deleteRecordByModelAndVersion(
            @RequestBody Map<String, Object> requestBody) {

        String modelNumber = (String) requestBody.get("model_number"); // or "modelNumber" if you prefer camelCase in
                                                                       // JSON
        String versionNumber = (String) requestBody.get("version_number");

        if (modelNumber == null || modelNumber.isBlank() ||
                versionNumber == null || versionNumber.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(CustomResponse.failure("Invalid input: model_number and version_number are required"));
        }

        Optional<Models_Table_Entity> entityOpt = civitaiSQL_Service
                .find_one_from_models_table_by_model_number_and_version_number(modelNumber,
                        versionNumber);

        if (entityOpt.isEmpty()) {
            return ResponseEntity.ok()
                    .body(CustomResponse.failure("Model not found in the database"));
        }

        Models_Table_Entity entity = entityOpt.get();
        civitaiSQL_Service.delete_record_to_all_table_by_id(entity.getId());

        String name = (entity.getName() != null && !entity.getName().isBlank()) ? entity.getName() : "(no-name)";
        System.out.println(
                "Deleted model: name=\"" + name + "\", model_number=" + modelNumber +
                        ", version_number=" + versionNumber + ", id=" + entity.getId());

        return ResponseEntity.ok().body(CustomResponse.success("Model deleted successfully"));
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

            System.out.println((List<String>) requestBody.get("localTags"));

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

            // Look up the record using modelId and versionId (i.e. modelNumber and
            // versionNumber)
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

    @SuppressWarnings("unchecked")
    @PostMapping("/add-offline-download-file-into-offline-download-list")
    public ResponseEntity<CustomResponse<String>> addOfflineDownloadFileIntoOfflineDownloadList(
            @RequestBody Map<String, Object> requestBody) {

        // Extract the input parameters
        Map<String, Object> modelObject = (Map<String, Object>) requestBody.get("modelObject");
        if (modelObject == null) {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        String civitaiFileName = (String) modelObject.get("civitaiFileName");
        List<Map<String, Object>> civitaiModelFileList = (List<Map<String, Object>>) modelObject
                .get("civitaiModelFileList");
        String downloadFilePath = (String) modelObject.get("downloadFilePath");
        String civitaiUrl = (String) modelObject.get("civitaiUrl");
        String civitaiModelID = (String) modelObject.get("civitaiModelID");
        String civitaiVersionID = (String) modelObject.get("civitaiVersionID");
        String selectedCategory = (String) modelObject.get("selectedCategory");
        Boolean isModifyMode = (Boolean) requestBody.get("isModifyMode");
        List<String> civitaiTags = modelObject.get("civitaiTags") != null
                ? (List<String>) modelObject.get("civitaiTags")
                : new ArrayList<>();

        // Validate required parameters (using isEmpty() for String checks)
        if (civitaiUrl == null || civitaiUrl.isEmpty() ||
                downloadFilePath == null || downloadFilePath.isEmpty() ||
                civitaiModelID == null || civitaiModelID.isEmpty() ||
                civitaiVersionID == null || civitaiVersionID.isEmpty() ||
                selectedCategory == null || selectedCategory.isEmpty() ||
                civitaiModelFileList == null || civitaiModelFileList.isEmpty()) {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        // Retry mechanism: try up to 5 times with 1-second delay between attempts.
        final int maxAttempts = 5;
        boolean success = false;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                // Attempt to find the model by version ID.
                Optional<Map<String, Object>> modelVersionOptional = civitai_Service
                        .findModelByVersionID(civitaiVersionID);
                if (!modelVersionOptional.isPresent()) {
                    throw new Exception("Model version not found");
                }
                Map<String, Object> modelVersionObject = modelVersionOptional.get();

                // Extract image URLs from the model version object.
                String[] imageUrlsArray = JsonUtils.extractImageUrls(modelVersionObject);

                // Update the offline download list.
                civitaiSQL_Service.update_offline_download_list(
                        civitaiFileName,
                        civitaiModelFileList,
                        downloadFilePath,
                        modelVersionObject,
                        civitaiModelID,
                        civitaiVersionID,
                        civitaiUrl,
                        (String) modelVersionObject.get("baseModel"),
                        imageUrlsArray,
                        selectedCategory,
                        civitaiTags,
                        isModifyMode);

                // Update the folder list.
                fileService.update_folder_list(downloadFilePath);

                // Log success
                System.out.println("Updated the offline List for: "
                        + civitaiModelID + "_" + civitaiVersionID + "_" + civitaiFileName);
                System.out.println("URL: " + civitaiUrl);

                // Mark the attempt as successful and exit the loop.
                success = true;
                break;
            } catch (Exception ex) {
                lastException = ex;
                System.err.println("Attempt " + attempt + " failed for " + civitaiModelID + "_" + civitaiVersionID + "_"
                        + civitaiFileName);

                // Wait 1 second before trying again (unless this was the last attempt)
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(1000); // Delay in milliseconds
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        // Optionally break out if the thread is interrupted
                        break;
                    }
                }
            }
        }

        if (!success) {
            // Log final error message
            System.err.println("Error - Failed Updating the offline List for: "
                    + civitaiModelID + "_" + civitaiVersionID + "_" + civitaiFileName);
            System.err.println("URL: " + civitaiUrl);
            System.err
                    .println("Final error: " + (lastException != null ? lastException.getMessage() : "Unknown error"));
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        return ResponseEntity.ok().body(CustomResponse.success("Success download file"));
    }

    @PostMapping(path = "/check-quantity-of-offlinedownload-list")
    public ResponseEntity<CustomResponse<Map<String, Long>>> CheckQuantityOfOfflineDownloadList(
            @RequestBody Map<String, Object> requestBody) {
        String url = (String) requestBody.get("url");
        String modelID = url.replaceAll(".*/models/(\\d+).*", "$1");

        // Validate null or empty
        if (url == null || url == "") {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }
        Long quantity = civitaiSQL_Service.checkQuantityOfOfflineDownloadList(modelID);

        Map<String, Long> payload = new HashMap<>();
        payload.put("quantity", quantity);

        return ResponseEntity.ok().body(CustomResponse.success("Model retrieval successful", payload));
    }

    @CrossOrigin(origins = "*")
    @PostMapping(path = "/find-version-numbers-for-offlinedownloadlist-tampermonkey")
    public ResponseEntity<CustomResponse<Map<String, List<String>>>> findVersionNumbersForOfflineDownloadList(
            @RequestBody Map<String, Object> requestBody) {

        String modelNumber = (String) requestBody.get("modelNumber");
        List<String> versionNumbers = ((List<?>) requestBody.get("versionNumbers")).stream()
                .map(Object::toString) // Ensures each element is treated as a String
                .collect(Collectors.toList());
        // Validate null or empty
        if (modelNumber == null || modelNumber.isEmpty() || versionNumbers == null || versionNumbers.isEmpty()) {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }
        Optional<List<String>> versionExistenceMapOptional = civitaiSQL_Service.getCivitaiVersionIds(modelNumber);

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

    @SuppressWarnings("unchecked")
    @PostMapping("/remove-offline-download-file-into-offline-download-list")
    public ResponseEntity<CustomResponse<String>> removeOfflineDownloadFileIntoOfflineDownloadList(
            @RequestBody Map<String, Object> requestBody) {

        Map<String, Object> modelObject = (Map<String, Object>) requestBody.get("modelObject");
        String civitaiModelID = (String) modelObject.get("civitaiModelID");
        String civitaiVersionID = (String) modelObject.get("civitaiVersionID");
        // Validate null or empty
        if (civitaiModelID == null || civitaiModelID == "" ||
                civitaiVersionID == null || civitaiVersionID == "") {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        try {
            civitaiSQL_Service.remove_from_offline_download_list(civitaiModelID, civitaiVersionID);
            return ResponseEntity.ok()
                    .body(CustomResponse.success("Success remove download file from offline download list"));

        } catch (Exception ex) {
            System.err.println("An error occurred: " + ex.getMessage());
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }
    }

    @CrossOrigin(origins = "*")
    @PostMapping(path = "/search_offline_downloads")
    public ResponseEntity<CustomResponse<Map<String, Object>>> searchOfflineDownloads(
            @RequestBody Map<String, Object> requestBody) {

        List<String> keywords = (List<String>) requestBody.get("keywords");

        try {
            // Call the service method and get the matching entries
            List<Map<String, Object>> filteredList = civitaiSQL_Service.searchOfflineDownloads(keywords);

            if (filteredList.isEmpty()) {
                // Return a "failure" response if nothing found
                return ResponseEntity.ok()
                        .body(CustomResponse.failure("No offline downloads found matching the given keywords."));
            }

            // Otherwise, wrap the results in your preferred response format
            Map<String, Object> payload = new HashMap<>();
            payload.put("offlineDownloadList", filteredList);

            return ResponseEntity.ok()
                    .body(CustomResponse.success("Search results found", payload));
        } catch (Exception ex) {
            System.out.println(ex);
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

    }

    @CrossOrigin(origins = "*")
    @GetMapping("/get_offline_download_list")
    public ResponseEntity<CustomResponse<Map<String, List<Map<String, Object>>>>> getOfflineDownloadList() {
        List<Map<String, Object>> offlineDownloadList = civitaiSQL_Service.get_offline_download_list();

        if (!offlineDownloadList.isEmpty()) {
            Map<String, List<Map<String, Object>>> payload = new HashMap<>();
            payload.put("offlineDownloadList", offlineDownloadList);

            return ResponseEntity.ok()
                    .body(CustomResponse.success("Offline Download List retrieval successful", payload));
        } else {
            return ResponseEntity.ok().body(CustomResponse.failure("No offline downloads found"));
        }
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/remove-from-error-model-list")
    public ResponseEntity<CustomResponse<String>> removeFromErrorModelList(
            @RequestBody Map<String, Object> requestBody) {

        Map<String, Object> modelObject = (Map<String, Object>) requestBody.get("modelObject");
        String civitaiModelID = (String) modelObject.get("civitaiModelID");
        String civitaiVersionID = (String) modelObject.get("civitaiVersionID");
        // Validate null or empty
        if (civitaiModelID == null || civitaiModelID == "" ||
                civitaiVersionID == null || civitaiVersionID == "") {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        System.out.println(civitaiModelID + "  " + civitaiVersionID);

        try {
            civitaiSQL_Service.update_error_model_offline_list(civitaiModelID, civitaiVersionID, false);
            return ResponseEntity.ok()
                    .body(CustomResponse.success("Success remove download file from offline download list"));

        } catch (Exception ex) {
            System.err.println("An error occurred: " + ex.getMessage());
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }
    }

    @GetMapping("/get_error_model_list")
    public ResponseEntity<CustomResponse<Map<String, List<String>>>> getErrorModelList() {
        List<String> errorModelList = civitaiSQL_Service.get_error_model_list();

        if (!errorModelList.isEmpty()) {
            Map<String, List<String>> payload = new HashMap<>();
            payload.put("errorModelList", errorModelList);

            return ResponseEntity.ok().body(CustomResponse.success("ErrorModelList retrieval successful", payload));
        } else {
            return ResponseEntity.ok().body(CustomResponse.failure("No Model found in the database"));
        }
    }

    @GetMapping("/get_creator_url_list")
    public ResponseEntity<CustomResponse<Map<String, List<Map<String, Object>>>>> getCreatorUrlList() {
        List<Map<String, Object>> creatorUrlList = civitaiSQL_Service.get_creator_url_list();

        Map<String, List<Map<String, Object>>> payload = new HashMap<>();
        payload.put("creatorUrlList", creatorUrlList);

        return ResponseEntity.ok().body(CustomResponse.success("creatorUrlList retrieval successful", payload));
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/update_creator_url_list")
    public ResponseEntity<CustomResponse<String>> updateCreatorUrlList(
            @RequestBody Map<String, Object> requestBody) {

        String creatorUrl = (String) requestBody.get("creatorUrl");
        String status = (String) requestBody.get("status");
        Boolean lastChecked = (Boolean) requestBody.get("lastChecked");
        String rating = (String) requestBody.getOrDefault("rating", "N/A");
        // Validate null or empty
        if (creatorUrl == null || creatorUrl == "") {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        try {

            civitaiSQL_Service.update_creator_url_list(creatorUrl, status, lastChecked, rating);

            return ResponseEntity.ok()
                    .body(CustomResponse.success("Success updating creator url list"));

        } catch (Exception ex) {
            System.err.println("Error - " + creatorUrl + " : "
                    + ex.getMessage());
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/remove_from_creator_url_list")
    public ResponseEntity<CustomResponse<String>> removeFromCreatorUrlList(
            @RequestBody Map<String, Object> requestBody) {

        String creatorUrl = (String) requestBody.get("creatorUrl");

        // Validate null or empty
        if (creatorUrl == null || creatorUrl == "") {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        try {

            civitaiSQL_Service.remove_creator_url(creatorUrl);

            return ResponseEntity.ok()
                    .body(CustomResponse.success("Success removing creator url from list"));

        } catch (Exception ex) {
            System.err.println("Error - " + creatorUrl + " : "
                    + ex.getMessage());
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }
    }

    @PostMapping(path = "/find-full-record-from-all-tables-by-modelID-and-version")
    public ResponseEntity<CustomResponse<FullModelRecordDTO>> findFullRecordByModelIdAndVersion(
            @RequestBody Map<String, Object> requestBody) {

        String modelID = (String) requestBody.get("modelID");
        String versionID = (String) requestBody.get("versionID");

        if (modelID == null || modelID.trim().isEmpty()
                || versionID == null || versionID.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        Optional<FullModelRecordDTO> fullOpt = civitaiSQL_Service.findFullByModelAndVersion(modelID, versionID);

        if (!fullOpt.isPresent()) {
            return ResponseEntity.ok().body(CustomResponse.failure("Model not found in the Database"));
        }

        Map<String, List<FullModelRecordDTO>> payload = new HashMap<>();
        payload.put("payload", Collections.singletonList(fullOpt.get()));

        return ResponseEntity.ok()
                .body(CustomResponse.success("Model retrieval successful", fullOpt.get()));
    }

    @PostMapping(path = "/path-visited")
    public ResponseEntity<CustomResponse<Void>> pathVisited(
            @RequestBody Map<String, Object> requestBody) {

        String rawPath = (String) requestBody.get("path");
        if (rawPath == null || rawPath.isBlank()) {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        try {
            // Normalize to backslashes for Windows-style paths
            String normalized = rawPath.replace('/', '\\').trim();

            // Extract drive
            String drive = null;
            if (normalized.startsWith("\\\\")) {
                drive = "\\\\";
            } else if (normalized.length() >= 2 && normalized.charAt(1) == ':') {
                drive = String.valueOf(Character.toUpperCase(normalized.charAt(0)));
            } else if (normalized.startsWith("/")) {
                drive = "/"; // POSIX
            }

            // Extract parentPath (keep trailing backslash if present)
            int lastSep = Math.max(normalized.lastIndexOf('\\'), normalized.lastIndexOf('/'));
            String parentPath = (lastSep > 0) ? normalized.substring(0, lastSep + 1) : null;

            civitaiSQL_Service.pathVisited(normalized, parentPath, drive);

            return ResponseEntity.ok().body(CustomResponse.success("Success download file"));

        } catch (Exception e) {
            System.out.println(e);
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));

        }
    }

    @PostMapping("/get-visited-paths-children")
    public ResponseEntity<CustomResponse<Map<String, List<Map<String, Object>>>>> getChildren(
            @RequestBody Map<String, Object> requestBody) {

        String parentPath = (String) requestBody.get("parentPath");
        if (parentPath == null || parentPath.isBlank()) {
            return ResponseEntity.badRequest().body(CustomResponse.failure("parent is required"));
        }

        try {
            // Normalize to match how you stored it (Windows-style + trailing backslash)
            String normalizedParent = parentPath.replace('/', '\\').trim();
            if (!normalizedParent.endsWith("\\") && !normalizedParent.endsWith("/")) {
                normalizedParent = normalizedParent + "\\";
            }

            List<VisitedPath_Table_Entity> children = civitaiSQL_Service.getChildren(normalizedParent);

            // Convert entities to a list of maps (no DTOs)
            List<Map<String, Object>> rows = children.stream().map(v -> {
                Map<String, Object> m = new HashMap<>();
                m.put("path", v.getPath());
                m.put("parentPath", v.getParentPath());
                m.put("drive", v.getDrive());
                m.put("accessCount", v.getAccessCount());
                m.put("firstAccessedAt", v.getFirstAccessedAt());
                m.put("lastAccessedAt", v.getLastAccessedAt());
                return m;
            }).toList();

            Map<String, List<Map<String, Object>>> payload = new HashMap<>();
            payload.put("payload", rows);

            return ResponseEntity.ok().body(
                    CustomResponse.success("visited paths retrieval successful", payload));

        } catch (Exception e) {
            e.printStackTrace(); // so you actually see the error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CustomResponse.failure("Unexpected error: " + e.getMessage()));
        }
    }

    @PostMapping("/add-recycle-record")
    public ResponseEntity<CustomResponse<Map<String, Object>>> addRecycleRecord(
            @RequestBody Map<String, Object> requestBody) {
        try {
            // --- inline parsing/validation ---
            String typeRaw = requestBody.get("type") == null ? null : String.valueOf(requestBody.get("type"));
            String originalPath = requestBody.get("originalPath") == null ? null
                    : String.valueOf(requestBody.get("originalPath"));
            String deletedFromPath = requestBody.get("deletedFromPath") == null ? null
                    : String.valueOf(requestBody.get("deletedFromPath"));

            if (typeRaw == null || typeRaw.isBlank()) {
                return ResponseEntity.badRequest().body(CustomResponse.failure("type is required"));
            }
            if (originalPath == null || originalPath.isBlank()) {
                return ResponseEntity.badRequest().body(CustomResponse.failure("originalPath is required"));
            }

            Recycle_Table_Entity.RecordType type;
            try {
                type = Recycle_Table_Entity.RecordType.valueOf(typeRaw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest().body(CustomResponse.failure("type must be 'set' or 'directory'"));
            }

            LocalDateTime deletedDate = null;
            Object dd = requestBody.get("deletedDate");
            if (dd instanceof String s) {
                try {
                    deletedDate = OffsetDateTime.parse(s).toLocalDateTime();
                } catch (DateTimeParseException ignore1) {
                    try {
                        deletedDate = LocalDateTime.parse(s);
                    } catch (DateTimeParseException ignore2) {
                        /* keep null */ }
                }
            } else if (dd instanceof Number n) {
                deletedDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(n.longValue()), ZoneId.systemDefault());
            }

            List<String> files = new ArrayList<>();
            Object filesObj = requestBody.get("files");
            if (filesObj instanceof List<?> raw) {
                for (Object it : raw)
                    if (it != null)
                        files.add(String.valueOf(it));
            } else if (filesObj instanceof String s && !s.isBlank()) {
                files.add(s);
            }

            // --- build entity & save ---
            Recycle_Table_Entity entity = Recycle_Table_Entity.builder()
                    .type(type)
                    .originalPath(originalPath)
                    .deletedFromPath(deletedFromPath)
                    .deletedDate(deletedDate) // service will default to now if null
                    .files(files)
                    .build();

            Recycle_Table_Entity saved = civitaiSQL_Service.add_to_recycle(entity);

            // --- payload ---
            Map<String, Object> row = new HashMap<>();
            row.put("id", saved.getId());
            row.put("type", saved.getType() != null ? saved.getType().name().toLowerCase(Locale.ROOT) : null);
            row.put("originalPath", saved.getOriginalPath());
            row.put("deletedFromPath", saved.getDeletedFromPath());
            row.put("deletedDate", saved.getDeletedDate());
            row.put("files", saved.getFiles() != null ? saved.getFiles() : List.of());

            Map<String, Object> payload = new HashMap<>();
            payload.put("payload", row);
            return ResponseEntity.ok(CustomResponse.success("recycle record created", payload));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CustomResponse.failure("Unexpected error: " + e.getMessage()));
        }
    }

    // POST /api/delete-recycle-record
    @PostMapping("/delete-recycle-record")
    public ResponseEntity<CustomResponse<Map<String, Object>>> deleteRecycleRecord(
            @RequestBody Map<String, Object> requestBody) {
        try {
            String id = requestBody.get("id") == null ? null : String.valueOf(requestBody.get("id"));
            if (id == null || id.isBlank()) {
                return ResponseEntity.badRequest().body(CustomResponse.failure("id is required"));
            }

            boolean ok = civitaiSQL_Service.delete_from_recycle(id);
            if (!ok) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(CustomResponse.failure("record not found: " + id));
            }

            Map<String, Object> result = new HashMap<>();
            result.put("deleted", true);
            result.put("id", id);

            Map<String, Object> payload = new HashMap<>();
            payload.put("payload", result);

            return ResponseEntity.ok(CustomResponse.success("recycle record deleted", payload));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CustomResponse.failure("Unexpected error: " + e.getMessage()));
        }
    }

    // GET /api/get-recyclelist
    @GetMapping("/get-recyclelist")
    public ResponseEntity<CustomResponse<Map<String, Object>>> getRecycleList() {
        try {
            List<Recycle_Table_Entity> rows = civitaiSQL_Service.fetch_recycle();

            List<Map<String, Object>> mapped = new ArrayList<>(rows.size());
            for (Recycle_Table_Entity e : rows) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", e.getId());
                m.put("type", e.getType() != null ? e.getType().name().toLowerCase(Locale.ROOT) : null);
                m.put("originalPath", e.getOriginalPath());
                m.put("deletedFromPath", e.getDeletedFromPath());
                m.put("deletedDate", e.getDeletedDate());
                m.put("files", e.getFiles() != null ? e.getFiles() : List.of());
                mapped.add(m);
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("payload", mapped);

            return ResponseEntity.ok(CustomResponse.success("recycle list retrieved", payload));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CustomResponse.failure("Unexpected error: " + e.getMessage()));
        }
    }

}
