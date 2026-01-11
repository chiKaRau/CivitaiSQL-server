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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.civitai.server.models.dto.FullModelRecordDTO;
import com.civitai.server.models.dto.Models_DTO;
import com.civitai.server.models.dto.PageResponse;
import com.civitai.server.models.dto.Tables_DTO;
import com.civitai.server.models.dto.TagCountDTO;
import com.civitai.server.models.dto.TopTagsRequest;
import com.civitai.server.models.entities.civitaiSQL.Models_Offline_Table_Entity;
import com.civitai.server.models.entities.civitaiSQL.Models_Table_Entity;
import com.civitai.server.models.entities.civitaiSQL.Recycle_Table_Entity;
import com.civitai.server.models.entities.civitaiSQL.VisitedPath_Table_Entity;
import com.civitai.server.services.CivitaiSQL_Service;
import com.civitai.server.services.Civitai_Service;
import com.civitai.server.services.File_Service;
import com.civitai.server.services.Gemini_Service;
import com.civitai.server.services.Jikan_Service;
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
    private Gemini_Service gemini_Service;
    private Jikan_Service jikan_Service;

    @Autowired
    public CivitaiSQL_Controller(CivitaiSQL_Service civitaiSQL_Service, Civitai_Service civitai_Service,
            Gemini_Service gemini_Service, Jikan_Service jikan_Service,
            File_Service fileService) {
        this.civitaiSQL_Service = civitaiSQL_Service;
        this.civitai_Service = civitai_Service;
        this.fileService = fileService;
        this.gemini_Service = gemini_Service;
        this.jikan_Service = jikan_Service;
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
    @CrossOrigin(origins = "*")
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
    public ResponseEntity<CustomResponse<PageResponse<Map<String, Object>>>> findVirtualFilesPaged(
            @RequestBody Map<String, Object> body) {

        String path = (String) body.get("path");
        if (path == null || path.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Provided path is empty or null"));
        }

        int page = body.get("page") != null ? ((Number) body.get("page")).intValue() : 0;
        int size = body.get("size") != null ? ((Number) body.get("size")).intValue() : 100;
        String sortKey = (String) body.getOrDefault("sortKey", "name");
        String sortDir = (String) body.getOrDefault("sortDir", "asc");
        String q = (String) body.get("q"); // 👈 optional

        // 🔍 Debug input
        System.out.println("[POST /find-virtual-files] INPUT");
        System.out.println("  path   = " + path);
        System.out.println("  page   = " + page);
        System.out.println("  size   = " + size);
        System.out.println("  sortKey= " + sortKey);
        System.out.println("  sortDir= " + sortDir);
        System.out.println("  q      = " + (q == null ? "<null>" : ('"' + q + '"')));

        PageResponse<Map<String, Object>> resp = civitaiSQL_Service
                .findVirtualFilesPaged(path, page, size, sortKey, sortDir, q); // 👈 pass q

        System.out.println("[/find-virtual-files] OUT total=" + resp.totalElements
                + " page=" + resp.page + " size=" + resp.size
                + " returned=" + (resp.content == null ? 0 : resp.content.size()));

        if (resp.content == null || resp.content.isEmpty()) {
            return ResponseEntity.ok(CustomResponse.failure("No virtual files found"));
        }
        return ResponseEntity.ok(CustomResponse.success("Virtual files retrieved successfully", resp));
    }

    @PostMapping("/find-virtual-directories")
    public ResponseEntity<CustomResponse<List<Map<String, Object>>>> findVirtualDirectories(
            @RequestBody Map<String, Object> body) {

        String path = (String) body.get("path");
        if (path == null || path.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(CustomResponse.failure("Provided path is empty or null"));
        }

        // optional; default to "name"/"asc"
        String sortKey = String.valueOf(body.getOrDefault("sortKey", "name"));
        String sortDir = String.valueOf(body.getOrDefault("sortDir", "asc"));

        var dirs = civitaiSQL_Service.findVirtualDirectoriesWithDrive(path, sortKey, sortDir);
        if (dirs.isEmpty()) {
            return ResponseEntity.ok(CustomResponse.failure("No virtual directories found"));
        }
        return ResponseEntity.ok(CustomResponse.success("Virtual directories retrieved successfully", dirs));
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

        // NEW: Hold flag
        Boolean hold = null;
        Object holdObj = modelObject.get("hold");
        if (holdObj instanceof Boolean) {
            hold = (Boolean) holdObj;
        } else if (holdObj != null) {
            // in case it comes as "true"/"false" string
            hold = Boolean.valueOf(String.valueOf(holdObj));
        }

        // NEW: Download priority (1..10)
        Integer downloadPriority = null;
        Object dpObj = modelObject.get("downloadPriority");
        if (dpObj instanceof Number) {
            downloadPriority = ((Number) dpObj).intValue();
        } else if (dpObj != null) {
            try {
                downloadPriority = Integer.parseInt(String.valueOf(dpObj).trim());
            } catch (NumberFormatException ignore) {
                // leave null -> will fall back to default in service
            }
        }

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
                // 1) Fetch the full model using the MODEL ID
                Optional<Map<String, Object>> modelOptional = civitai_Service.findModelByModelID(civitaiModelID);
                if (!modelOptional.isPresent()) {
                    throw new Exception("Model not found for modelID=" + civitaiModelID);
                }
                Map<String, Object> fetchedModel = modelOptional.get(); // <-- renamed

                // 2) Pull out the modelVersions array from the fetched model
                Object versionsRaw = fetchedModel.get("modelVersions"); // <-- use fetchedModel
                if (!(versionsRaw instanceof List)) {
                    throw new Exception("Model has no modelVersions array (modelID=" + civitaiModelID + ")");
                }
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> modelVersions = (List<Map<String, Object>>) versionsRaw;

                if (modelVersions.isEmpty()) {
                    throw new Exception("Model has an empty modelVersions array (modelID=" + civitaiModelID + ")");
                }

                // 3) Find the specific version whose id matches civitaiVersionID
                Map<String, Object> modelVersionObject = modelVersions.stream()
                        .filter(v -> civitaiVersionID.equals(String.valueOf(v.get("id"))))
                        .findFirst()
                        .orElseThrow(() -> new Exception(
                                "Version " + civitaiVersionID + " not found in model " + civitaiModelID));

                // Ensure the version map is mutable
                if (!(modelVersionObject instanceof java.util.LinkedHashMap)) {
                    try {
                        modelVersionObject.put("_check_mutable", Boolean.TRUE);
                        modelVersionObject.remove("_check_mutable");
                    } catch (UnsupportedOperationException uoe) {
                        modelVersionObject = new java.util.LinkedHashMap<>(modelVersionObject);
                    }
                }

                // --- EARLY ACCESS PATCH: add earlyAccessEndsAt next to availability ---
                Object availabilityVal = modelVersionObject.get("availability");
                String availability = availabilityVal != null ? String.valueOf(availabilityVal) : null;

                if ("EarlyAccess".equalsIgnoreCase(availability)) {
                    try {
                        // Build the endpoint for the version details
                        String versionUrl = "https://civitai.com/api/v1/model-versions/" + civitaiVersionID;

                        // A tiny RestTemplate with short timeouts so it won't hang your whole request
                        org.springframework.http.client.SimpleClientHttpRequestFactory rf = new org.springframework.http.client.SimpleClientHttpRequestFactory();
                        rf.setConnectTimeout(4000);
                        rf.setReadTimeout(4000);

                        org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate(
                                rf);

                        @SuppressWarnings("unchecked")
                        Map<String, Object> versionPayload = rt.getForObject(versionUrl, Map.class);

                        if (versionPayload != null && versionPayload.get("earlyAccessEndsAt") != null) {
                            // Add as a sibling of "availability"
                            modelVersionObject.put("earlyAccessEndsAt", versionPayload.get("earlyAccessEndsAt"));
                        } else {
                            // If you prefer always having the key present for EA, keep null; else skip this
                            // put.
                            modelVersionObject.put("earlyAccessEndsAt", null);
                        }
                    } catch (Exception eaEx) {
                        // Don't fail the whole flow just because this call failed; log and continue
                        System.err.println("Failed to fetch earlyAccessEndsAt for version " + civitaiVersionID
                                + ": " + eaEx.getMessage());
                        // Optional: still add the key so downstream knows it's EarlyAccess but unknown
                        // end date
                        modelVersionObject.put("earlyAccessEndsAt", null);
                    }
                }
                // --- END EARLY ACCESS PATCH ---

                // Build compact model summary from the top-level model
                Map<String, Object> modelSummary = new java.util.LinkedHashMap<>();
                modelSummary.put("id", fetchedModel.get("id")); // optional, handy later
                modelSummary.put("poi", fetchedModel.get("poi"));
                modelSummary.put("name", fetchedModel.get("name"));
                modelSummary.put("nsfw", fetchedModel.get("nsfw"));
                modelSummary.put("type", fetchedModel.get("type"));

                // Attach at the SAME LEVEL on the version object
                modelVersionObject.put("model", modelSummary);

                // Copy top-level creator onto the version object as a sibling of "model"
                Object creatorObj = fetchedModel.get("creator");
                if (creatorObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> creatorMap = (Map<String, Object>) creatorObj;
                    modelVersionObject.put("creator", creatorMap);
                } else {
                    // Still create the key for consistency, even if null
                    modelVersionObject.put("creator", null);
                }

                // Add modelId at the same level (prefer top-level fetchedModel.id; fallback to
                // civitaiModelID)
                Object modelIdValue = fetchedModel.get("id"); // could be Integer/Long/String depending on parser
                if (modelIdValue == null) {
                    // Fallback: try to parse civitaiModelID into a number; if it fails, keep it as
                    // String
                    try {
                        modelIdValue = Long.valueOf(civitaiModelID);
                    } catch (NumberFormatException nfe) {
                        modelIdValue = civitaiModelID; // keep as String
                    }
                }

                // If you don't want to overwrite an existing key, use putIfAbsent; otherwise
                // use put
                if (modelVersionObject instanceof java.util.concurrent.ConcurrentMap) {
                    ((java.util.concurrent.ConcurrentMap<String, Object>) modelVersionObject)
                            .putIfAbsent("modelId", modelIdValue);
                } else {
                    // Safe "put if absent" emulation for regular maps
                    if (!modelVersionObject.containsKey("modelId")) {
                        modelVersionObject.put("modelId", modelIdValue);
                    }
                }

                // 4) Proceed exactly like before using the selected version object
                String[] imageUrlsArray = JsonUtils.extractImageUrls(modelVersionObject);

                civitaiSQL_Service.update_offline_download_list(
                        civitaiFileName,
                        civitaiModelFileList,
                        downloadFilePath,
                        modelVersionObject, // <- still the version object
                        civitaiModelID,
                        civitaiVersionID,
                        civitaiUrl,
                        (String) modelVersionObject.get("baseModel"),
                        imageUrlsArray,
                        selectedCategory,
                        civitaiTags,
                        isModifyMode,
                        hold,
                        downloadPriority);

                fileService.update_folder_list(downloadFilePath);

                System.out.println("Updated the offline List for: "
                        + civitaiModelID + "_" + civitaiVersionID + "_" + civitaiFileName);
                System.out.println("URL: " + civitaiUrl);

                success = true;
                break;
            } catch (Exception ex) {
                lastException = ex;
                System.err.println("Attempt " + attempt + " failed for "
                        + civitaiModelID + "_" + civitaiVersionID + "_" + civitaiFileName
                        + " | reason: " + ex.getMessage());
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
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

    @CrossOrigin(origins = "*")
    @PostMapping(path = "/get-offline-record-by-model-and-version")
    public ResponseEntity<CustomResponse<Models_Offline_Table_Entity>> getOfflineRecordByModelAndVersionEndpoint(
            @RequestBody Map<String, Object> requestBody) {

        String modelNumber = requestBody.get("modelNumber") != null ? requestBody.get("modelNumber").toString() : null;
        String versionNumber = requestBody.get("versionNumber") != null ? requestBody.get("versionNumber").toString()
                : null;

        if (modelNumber == null || modelNumber.isBlank() || versionNumber == null || versionNumber.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(CustomResponse.failure("Invalid input: modelNumber/versionNumber required"));
        }

        Optional<Models_Offline_Table_Entity> recordOpt = civitaiSQL_Service
                .getOfflineRecordByModelAndVersion(modelNumber, versionNumber);

        if (recordOpt.isPresent()) {
            return ResponseEntity.ok(CustomResponse.success("Record retrieved", recordOpt.get()));
        } else {
            // Keep your existing “success with null” pattern; switch to 404 if you prefer.
            return ResponseEntity.ok(CustomResponse.success("No record found for given model/version", null));
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

    @PostMapping("/get-top-tags")
    public ResponseEntity<CustomResponse<PageResponse<TagCountDTO>>> getTopTags(@RequestBody TopTagsRequest req) {
        var result = civitaiSQL_Service.get_top_tags_page(req);
        return ResponseEntity.ok(CustomResponse.success("OK", result));
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/get_offline_download_list-in-page")
    public ResponseEntity<CustomResponse<PageResponse<java.util.Map<String, Object>>>> getOfflineDownloadListInPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "false") boolean filterEmptyBaseModel,
            @RequestParam(name = "prefix", required = false) java.util.List<String> prefixes,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "op", defaultValue = "contains") String op,
            @RequestParam(name = "status", defaultValue = "both") String status,
            @RequestParam(name = "includeHold", defaultValue = "true") boolean includeHold,
            @RequestParam(name = "includeEarlyAccess", defaultValue = "true") boolean includeEarlyAccess,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir) {

        final int p = Math.max(0, page);
        final int s = Math.min(Math.max(1, size), 500);

        System.out.println("---- GET /get-offline-download-list-paged ----");
        System.out.println("page = " + page + " (normalized=" + p + ")");
        System.out.println("size = " + size + " (normalized=" + s + ")");
        System.out.println("filterEmptyBaseModel = " + filterEmptyBaseModel);
        System.out.println("op = " + op);
        System.out.println("status = " + status);
        System.out.println("includeHold = " + includeHold);
        System.out.println("includeEarlyAccess = " + includeEarlyAccess);
        System.out.println("sortDir = " + sortDir);
        System.out.println("search = " + (search == null ? "<null>" : ('"' + search + '"')));

        if (prefixes == null) {
            System.out.println("prefixes = <null>");
        } else {
            System.out.println("prefixes.size = " + prefixes.size());
            int cap = Math.min(prefixes.size(), 20); // avoid dumping huge lists
            System.out.println("prefixes(first " + cap + ") = " + prefixes.subList(0, cap));
            if (prefixes.size() > cap) {
                System.out.println("prefixes(remaining) = " + (prefixes.size() - cap) + " more …");
            }
        }

        var result = civitaiSQL_Service.get_offline_download_list_paged(
                page,
                size,
                filterEmptyBaseModel,
                prefixes,
                search,
                op,
                status,
                includeHold,
                includeEarlyAccess,
                sortDir);
        return ResponseEntity.ok().body(CustomResponse.success("OK", result));
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/get_pending_from_offline_download_list-in-page")
    public ResponseEntity<CustomResponse<PageResponse<java.util.Map<String, Object>>>> getPendingFromOfflineDownloadListInPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        final int p = Math.max(0, page);
        final int s = Math.min(Math.max(1, size), 500);

        // --- Preset values for "Pending" view ---
        final boolean filterEmptyBaseModel = false;
        final List<String> prefixes = Arrays.asList(
                "/@scan@/ACG/Pending",
                "/@scan@/ACG/Pending/");
        final String search = null;
        final String op = "contains";
        final String status = "pending";
        final boolean includeHold = false;
        final boolean includeEarlyAccess = false;
        final String sortDir = "desc";

        System.out.println("---- GET /get_pending_from_offline_download_list-in-page ----");
        System.out.println("page = " + page + " (normalized=" + p + ")");
        System.out.println("size = " + size + " (normalized=" + s + ")");
        System.out.println("filterEmptyBaseModel = " + filterEmptyBaseModel);
        System.out.println("op = " + op);
        System.out.println("status = " + status);
        System.out.println("includeHold = " + includeHold);
        System.out.println("includeEarlyAccess = " + includeEarlyAccess);
        System.out.println("sortDir = " + sortDir);
        System.out.println("search = <null>");
        System.out.println("prefixes.size = " + prefixes.size());
        System.out.println("prefixes = " + prefixes);

        var result = civitaiSQL_Service.get_offline_download_list_paged(
                p,
                s,
                filterEmptyBaseModel,
                prefixes,
                search,
                op,
                status,
                includeHold,
                includeEarlyAccess,
                sortDir);

        return ResponseEntity.ok().body(CustomResponse.success("OK", result));
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/run_pending_from_offline_download_list-ai_suggestion")
    public ResponseEntity<CustomResponse<List<String>>> getPendingFromOfflineDownloadListAiSuggestion(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

            final int p = Math.max(0, page);
            final int s = Math.min(Math.max(1, size), 500);

            // preset (your "pending" filters)
            final boolean filterEmptyBaseModel = false;
            final java.util.List<String> prefixes = java.util.Arrays.asList(
                    "/@scan@/ACG/Pending",
                    "/@scan@/ACG/Pending/");
            final String search = null;
            final String op = "contains";
            final String status = "pending";
            final boolean includeHold = false;
            final boolean includeEarlyAccess = false;
            final String sortDir = "desc";

            // 1) Fetch
            PageResponse<java.util.Map<String, Object>> pageResult = civitaiSQL_Service
                    .get_offline_download_list_paged_aiSuggestedArtworkTitleEmpty(
                            p, s,
                            filterEmptyBaseModel,
                            prefixes,
                            search,
                            op,
                            status,
                            includeHold,
                            includeEarlyAccess,
                            sortDir);

            // 2) Convert to JsonNode so we can read "content" without relying on getters
            com.fasterxml.jackson.databind.JsonNode pageNode = mapper.valueToTree(pageResult);

            // try root.content first, then payload.content fallback
            com.fasterxml.jackson.databind.JsonNode contentNode = pageNode.path("content");
            if (!contentNode.isArray()) {
                contentNode = pageNode.path("payload").path("content");
            }

            if (!contentNode.isArray() || contentNode.size() == 0) {
                return ResponseEntity.ok(CustomResponse.success("OK", java.util.List.of()));
            }

            // 3) Build Gemini inputs (+ keep a map id -> characterName so we can merge
            // later)
            java.util.List<com.civitai.server.services.impl.Gemini_Service_impl.CharacterMatchInput> inputs = new java.util.ArrayList<>();
            java.util.Map<String, String> idToCharacterName = new java.util.LinkedHashMap<>();

            for (int i = 0; i < contentNode.size(); i++) {
                com.fasterxml.jackson.databind.JsonNode row = contentNode.get(i);

                String civitaiVersionID = row.path("civitaiVersionID").asText(null);
                String civitaiUrl = row.path("civitaiUrl").asText(null);
                String civitaiFileName = row.path("civitaiFileName").asText(null);

                String modelName = row.path("modelVersionObject").path("model").path("name").asText(null);
                String versionName = row.path("modelVersionObject").path("name").asText(null); // v1.0

                // characterName = modelName + " - " + versionName
                String a = modelName == null ? "" : modelName.trim();
                String b = versionName == null ? "" : versionName.trim();
                String characterName;
                if (a.isBlank() && b.isBlank())
                    characterName = null;
                else if (a.isBlank())
                    characterName = b;
                else if (b.isBlank())
                    characterName = a;
                else
                    characterName = a + " - " + b;

                if (characterName == null || characterName.isBlank())
                    continue;

                // tags: civitaiTags (len >= 2, distinct, limit 25)
                java.util.LinkedHashSet<String> dedupe = new java.util.LinkedHashSet<>();
                com.fasterxml.jackson.databind.JsonNode tagsNode = row.path("civitaiTags");
                if (tagsNode.isArray()) {
                    for (int t = 0; t < tagsNode.size(); t++) {
                        com.fasterxml.jackson.databind.JsonNode tagNode = tagsNode.get(t);
                        if (tagNode != null && tagNode.isTextual()) {
                            String tag = tagNode.asText("").trim();
                            if (tag.length() >= 2)
                                dedupe.add(tag);
                            if (dedupe.size() >= 25)
                                break;
                        }
                    }
                }
                java.util.List<String> tags = new java.util.ArrayList<>(dedupe);

                // id: prefer civitaiVersionID, else fallback to index
                String id = (civitaiVersionID != null && !civitaiVersionID.trim().isBlank())
                        ? civitaiVersionID.trim()
                        : String.valueOf(i);

                // trimToNull for filename/url
                civitaiFileName = (civitaiFileName == null || civitaiFileName.trim().isBlank()) ? null
                        : civitaiFileName.trim();
                civitaiUrl = (civitaiUrl == null || civitaiUrl.trim().isBlank()) ? null : civitaiUrl.trim();

                com.civitai.server.services.impl.Gemini_Service_impl.CharacterMatchInput in = new com.civitai.server.services.impl.Gemini_Service_impl.CharacterMatchInput();
                in.setId(id);
                in.setCharacterName(characterName);
                in.setTags(tags);
                in.setCivitaiFileName(civitaiFileName);
                in.setCivitaiUrl(civitaiUrl);

                inputs.add(in);
                idToCharacterName.putIfAbsent(id, characterName);
            }

            if (inputs.isEmpty()) {
                return ResponseEntity.ok(CustomResponse.success("OK", java.util.List.of()));
            }

            // 4) Call Gemini once (batch)
            java.util.List<com.civitai.server.services.impl.Gemini_Service_impl.CharacterTitleMatch> aiSuggestion = gemini_Service
                    .matchCharactersToTitles(inputs);

            // 5) Jikan normalization
            java.util.List<com.civitai.server.services.impl.Gemini_Service_impl.CharacterTitleMatch> normalized = jikan_Service
                    .normalizeTitles(aiSuggestion);

            // 6) folders list
            java.util.List<String> foldersList = fileService.get_folders_list();

            // Build folder index ONCE (so we don't rebuild for every match)
            java.util.List<String> f_paths = new java.util.ArrayList<>();
            java.util.List<String> f_normNames = new java.util.ArrayList<>();
            java.util.List<String> f_normPaths = new java.util.ArrayList<>();
            java.util.List<java.util.Set<String>> f_nameBigrams = new java.util.ArrayList<>();
            java.util.List<java.util.Set<String>> f_nameTokens = new java.util.ArrayList<>();

            for (String path : foldersList) {
                if (path == null || path.isBlank())
                    continue;

                String pth = path.trim();
                String tmp = pth;
                if (tmp.endsWith("/"))
                    tmp = tmp.substring(0, tmp.length() - 1);

                int idx = tmp.lastIndexOf('/');
                String base = (idx >= 0) ? tmp.substring(idx + 1) : tmp;

                String nn = norm(base);
                String np = norm(pth);

                f_paths.add(pth);
                f_normNames.add(nn);
                f_normPaths.add(np);
                f_nameBigrams.add(bigrams(nn));
                f_nameTokens.add(tokens(nn));
            }

            // 7) Merge by id
            java.util.Map<String, String> aiTitleById = new java.util.HashMap<>();
            if (aiSuggestion != null) {
                for (var x : aiSuggestion) {
                    if (x == null)
                        continue;
                    String id = x.getId();
                    String title = x.getTitle();
                    if (id == null || id.isBlank())
                        continue;
                    if (title == null || title.isBlank())
                        continue;
                    aiTitleById.put(id.trim(), title.trim());
                }
            }

            java.util.Map<String, String> normTitleById = new java.util.HashMap<>();
            if (normalized != null) {
                for (var x : normalized) {
                    if (x == null)
                        continue;
                    String id = x.getId();
                    String title = x.getTitle();
                    if (id == null || id.isBlank())
                        continue;
                    if (title == null || title.isBlank())
                        continue;
                    normTitleById.put(id.trim(), title.trim());
                }
            }

            // 8) Build final DTO list (+ fuzzy folder suggestions)
            final int suggestLimit = 5;
            final double suggestThreshold = 0.35;

            java.util.List<com.civitai.server.models.dto.PendingAiSuggestionDTO> out = new java.util.ArrayList<>();

            for (var e : idToCharacterName.entrySet()) {
                String id = e.getKey();
                String characterName = e.getValue();

                String aiTitle = aiTitleById.get(id);
                String jikanTitle = normTitleById.get(id);

                com.civitai.server.models.dto.PendingAiSuggestionDTO dto = new com.civitai.server.models.dto.PendingAiSuggestionDTO();

                dto.setCivitaiVersionID(id);
                dto.setCharacterName(characterName);

                dto.setAiSuggestedArtworkTitle(aiTitle);
                dto.setJikanNormalizedArtworkTitle(jikanTitle);

                dto.setAiSuggestedDownloadFilePath(
                        pickValidPaths(topFolderMatches(aiTitle, foldersList, suggestLimit, suggestThreshold),
                                suggestLimit));

                dto.setJikanSuggestedDownloadFilePath(
                        pickValidPaths(topFolderMatches(jikanTitle, foldersList, suggestLimit, suggestThreshold),
                                suggestLimit));

                dto.setLocalSuggestedDownloadFilePath(
                        pickValidPaths(topFolderMatchesWithIndex(characterName, suggestLimit, 0.45,
                                f_paths, f_normNames, f_normPaths, f_nameBigrams, f_nameTokens), suggestLimit));

                out.add(dto);
            }

            int updated = civitaiSQL_Service.updatePendingAiSuggestions(out);
            System.out.println("Updated rows: " + updated);

            // Return only IDs (distinct, stable order)
            java.util.List<String> processedIds = out.stream()
                    .map(com.civitai.server.models.dto.PendingAiSuggestionDTO::getCivitaiVersionID)
                    .filter(java.util.Objects::nonNull)
                    .map(String::trim)
                    .filter(x -> !x.isBlank())
                    .distinct()
                    .toList();

            System.out.println(processedIds);

            return ResponseEntity.ok(CustomResponse.success("OK", processedIds));

        } catch (Exception ex) {
            System.err.println("Unexpected error occurred: " + ex.getMessage());
            ex.printStackTrace();
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CustomResponse.failure("Unexpected error: " + ex.getMessage()));
        }
    }

    /*
     * =========================
     * Helper methods (same file)
     * =========================
     */

    private static java.util.List<String> pickValidPaths(java.util.List<String> paths, int limit) {
        if (paths == null || paths.isEmpty())
            return java.util.List.of();

        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        java.util.LinkedHashSet<String> dedupe = new java.util.LinkedHashSet<>();

        for (String p : paths) {
            if (p == null)
                continue;
            String x = p.trim();
            if (x.isBlank())
                continue;

            // must start with '/'
            if (!x.startsWith("/"))
                continue;

            // must NOT contain update segment
            if (x.contains("/Update/") || x.contains("@scan@/Update/"))
                continue;

            // de-dupe while preserving order
            if (dedupe.add(x)) {
                out.add(x);
                if (out.size() >= Math.max(1, limit))
                    break;
            }
        }
        return out;
    }

    private static java.util.List<String> topFolderMatches(String term, java.util.List<String> foldersList,
            int limit, double threshold) {
        if (term == null || term.trim().isBlank())
            return java.util.List.of();
        if (foldersList == null || foldersList.isEmpty())
            return java.util.List.of();

        String normTerm = norm(term);
        if (normTerm.isBlank())
            return java.util.List.of();

        java.util.Set<String> termBigrams = bigrams(normTerm);
        java.util.Set<String> termTokens = tokens(normTerm);

        // Pre-index folders (local, per-request)
        java.util.List<String> paths = new java.util.ArrayList<>();
        java.util.List<String> normNames = new java.util.ArrayList<>();
        java.util.List<String> normPaths = new java.util.ArrayList<>();
        java.util.List<java.util.Set<String>> nameBigrams = new java.util.ArrayList<>();
        java.util.List<java.util.Set<String>> nameTokens = new java.util.ArrayList<>();

        for (String path : foldersList) {
            if (path == null || path.isBlank())
                continue;

            String p = path.trim();
            String base = p;
            String tmp = p;
            if (tmp.endsWith("/"))
                tmp = tmp.substring(0, tmp.length() - 1);
            int idx = tmp.lastIndexOf('/');
            if (idx >= 0)
                base = tmp.substring(idx + 1);
            else
                base = tmp;

            String nn = norm(base);
            String np = norm(p);

            paths.add(p);
            normNames.add(nn);
            normPaths.add(np);
            nameBigrams.add(bigrams(nn));
            nameTokens.add(tokens(nn));
        }

        int n = paths.size();
        double[] scores = new double[n];
        java.util.List<Integer> idxs = new java.util.ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            String nn = normNames.get(i);
            String np = normPaths.get(i);

            boolean containsName = !nn.isBlank() && nn.contains(normTerm);
            boolean containsPath = !np.isBlank() && np.contains(normTerm);

            double dice = diceCoefficient(termBigrams, nameBigrams.get(i));
            double jac = jaccard(termTokens, nameTokens.get(i));

            // weighted blend (tweak to taste)
            double score = 0.75 * dice + 0.25 * jac;

            if (containsName)
                score = Math.min(1.0, score + 0.15);
            else if (containsPath)
                score = Math.min(1.0, score + 0.08);

            scores[i] = score;
            if (score >= threshold)
                idxs.add(i);
        }

        idxs.sort((a, b) -> Double.compare(scores[b], scores[a]));

        java.util.List<String> out = new java.util.ArrayList<>();
        for (int k = 0; k < idxs.size() && out.size() < Math.max(1, limit); k++) {
            out.add(paths.get(idxs.get(k)));
        }
        return out;
    }

    private static String norm(String s) {
        if (s == null)
            return "";
        String x = s.trim().toLowerCase(java.util.Locale.ROOT);

        // strip accents
        x = java.text.Normalizer.normalize(x, java.text.Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "");

        // non-alnum -> space (":" "-" "/" etc)
        x = x.replaceAll("[^a-z0-9]+", " ");

        // collapse spaces
        x = x.replaceAll("\\s+", " ").trim();
        return x;
    }

    private static java.util.Set<String> tokens(String s) {
        if (s == null || s.isBlank())
            return java.util.Set.of();
        String[] parts = s.split(" ");
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<>();
        for (String p : parts) {
            if (p.length() < 2)
                continue; // your rule
            out.add(p);
        }
        return out;
    }

    private static java.util.Set<String> bigrams(String s) {
        if (s == null)
            return java.util.Set.of();
        String x = s.replace(" ", "");
        if (x.length() < 2)
            return java.util.Set.of();

        java.util.HashSet<String> out = new java.util.HashSet<>();
        for (int i = 0; i < x.length() - 1; i++) {
            out.add(x.substring(i, i + 2));
        }
        return out;
    }

    private static double diceCoefficient(java.util.Set<String> a, java.util.Set<String> b) {
        if (a.isEmpty() || b.isEmpty())
            return 0.0;
        int inter = 0;
        if (a.size() > b.size()) {
            java.util.Set<String> tmp = a;
            a = b;
            b = tmp;
        }
        for (String x : a)
            if (b.contains(x))
                inter++;
        return (2.0 * inter) / (a.size() + b.size());
    }

    private static double jaccard(java.util.Set<String> a, java.util.Set<String> b) {
        if (a.isEmpty() || b.isEmpty())
            return 0.0;
        int inter = 0;
        if (a.size() > b.size()) {
            java.util.Set<String> tmp = a;
            a = b;
            b = tmp;
        }
        for (String x : a)
            if (b.contains(x))
                inter++;
        int union = (a.size() + b.size() - inter);
        return union == 0 ? 0.0 : (inter * 1.0 / union);
    }

    private static java.util.List<String> topFolderMatchesWithIndex(
            String term, int limit, double threshold,
            java.util.List<String> paths,
            java.util.List<String> normNames,
            java.util.List<String> normPaths,
            java.util.List<java.util.Set<String>> nameBigrams,
            java.util.List<java.util.Set<String>> nameTokens) {
        if (term == null || term.trim().isBlank())
            return java.util.List.of();
        if (paths == null || paths.isEmpty())
            return java.util.List.of();

        String normTerm = norm(term);
        if (normTerm.isBlank())
            return java.util.List.of();

        java.util.Set<String> termBigrams = bigrams(normTerm);
        java.util.Set<String> termTokens = tokens(normTerm);

        int n = paths.size();
        double[] scores = new double[n];
        java.util.List<Integer> idxs = new java.util.ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            String nn = normNames.get(i);
            String np = normPaths.get(i);

            boolean containsName = !nn.isBlank() && nn.contains(normTerm);
            boolean containsPath = !np.isBlank() && np.contains(normTerm);

            double dice = diceCoefficient(termBigrams, nameBigrams.get(i));
            double jac = jaccard(termTokens, nameTokens.get(i));

            double score = 0.75 * dice + 0.25 * jac;

            if (containsName)
                score = Math.min(1.0, score + 0.15);
            else if (containsPath)
                score = Math.min(1.0, score + 0.08);

            scores[i] = score;
            if (score >= threshold)
                idxs.add(i);
        }

        idxs.sort((a, b) -> Double.compare(scores[b], scores[a]));

        java.util.List<String> out = new java.util.ArrayList<>();
        for (int k = 0; k < idxs.size() && out.size() < Math.max(1, limit); k++) {
            out.add(paths.get(idxs.get(k)));
        }
        return out;
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/get_offline_download_list_hold")
    public ResponseEntity<CustomResponse<java.util.List<java.util.Map<String, Object>>>> getOfflineDownloadListHold() {

        var result = civitaiSQL_Service.get_offline_download_list_hold();
        return ResponseEntity.ok().body(CustomResponse.success("OK", result));
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/get_offline_download_list_early_access_active")
    public ResponseEntity<CustomResponse<java.util.List<java.util.Map<String, Object>>>> getOfflineDownloadListEarlyAccessActive() {

        var result = civitaiSQL_Service.get_offline_download_list_early_access_active();
        return ResponseEntity.ok().body(CustomResponse.success("OK", result));
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

    // @GetMapping("/get_error_model_list")
    // public ResponseEntity<CustomResponse<Map<String, List<String>>>>
    // getErrorModelList() {
    // List<String> errorModelList = civitaiSQL_Service.get_error_model_list();

    // if (!errorModelList.isEmpty()) {
    // Map<String, List<String>> payload = new HashMap<>();
    // payload.put("errorModelList", errorModelList);

    // return ResponseEntity.ok().body(CustomResponse.success("ErrorModelList
    // retrieval successful", payload));
    // } else {
    // return ResponseEntity.ok().body(CustomResponse.failure("No Model found in the
    // database"));
    // }
    // }

    @CrossOrigin(origins = "*")
    @GetMapping("/get_error_model_list")
    public ResponseEntity<CustomResponse<java.util.List<java.util.Map<String, Object>>>> getErrorModelList() {

        var result = civitaiSQL_Service.get_error_model_list();
        return ResponseEntity.ok().body(CustomResponse.success("OK", result));
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

    @CrossOrigin(origins = "*")
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

    @PostMapping("/update-myrating-by-modelId-and-versionId")
    public ResponseEntity<CustomResponse<Map<String, Object>>> updateMyRatingByModelIdAndVersionId(
            @RequestBody Map<String, Object> body) {

        // accept either (modelID, versionID) or (modelNumber, versionNumber)
        String modelNumber = (String) (body.get("modelNumber") != null ? body.get("modelNumber") : body.get("modelID"));
        String versionNumber = (String) (body.get("versionNumber") != null ? body.get("versionNumber")
                : body.get("versionID"));
        Integer rating = toIntOrNull(body.get("rating"));

        if (modelNumber == null || versionNumber == null || rating == null) {
            return ResponseEntity.badRequest()
                    .body(CustomResponse
                            .failure("Required: modelID/modelNumber, versionID/versionNumber, rating(0..20)"));
        }

        try {
            Models_Table_Entity updated = civitaiSQL_Service
                    .updateMyRatingByModelAndVersion(modelNumber, versionNumber, rating);

            Map<String, Object> payload = new HashMap<>();
            payload.put("id", updated.getId());
            payload.put("modelNumber", updated.getModelNumber());
            payload.put("versionNumber", updated.getVersionNumber());
            payload.put("myRating", updated.getMyRating());
            payload.put("updatedAt", updated.getUpdatedAt());

            return ResponseEntity.ok(CustomResponse.success("Rating updated", payload));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(CustomResponse.failure(ex.getMessage()));
        } catch (NoSuchElementException ex) {
            return ResponseEntity.ok().body(CustomResponse.failure(ex.getMessage())); // 200 + failure msg (matches your
                                                                                      // pattern)
        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                    .body(CustomResponse.failure("Update failed: " + ex.getMessage()));
        }
    }

    private static Integer toIntOrNull(Object v) {
        if (v instanceof Number)
            return ((Number) v).intValue();
        try {
            return v != null ? Integer.parseInt(v.toString()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * PATCH-like update: apply only non-null fields/sections of FullModelRecordDTO.
     * Target is located by dto.model.modelNumber + dto.model.versionNumber.
     */
    @PutMapping("/update-full-record-by-modelID-and-version")
    public ResponseEntity<CustomResponse<FullModelRecordDTO>> updateFullRecord(
            @RequestBody FullModelRecordDTO dto) {

        if (dto == null || dto.getModel() == null ||
                dto.getModel().getModelNumber() == null || dto.getModel().getModelNumber().isBlank() ||
                dto.getModel().getVersionNumber() == null || dto.getModel().getVersionNumber().isBlank()) {

            return ResponseEntity.badRequest()
                    .body(CustomResponse
                            .failure("Invalid input: model.modelNumber and model.versionNumber are required."));
        }

        try {
            FullModelRecordDTO updated = civitaiSQL_Service.updateFullByModelAndVersion(dto);
            return ResponseEntity.ok(CustomResponse.success("Model update successful", updated));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(CustomResponse.failure(ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                    .body(CustomResponse.failure("Update failed: " + ex.getMessage()));
        }
    }

    @PostMapping(path = "/update-hold-from-offline-download_list")
    public ResponseEntity<CustomResponse<String>> UpdateHoldFromOfflineDownloadList(
            @RequestBody Map<String, Object> requestBody) {

        String modelNumber = requestBody.get("modelNumber") != null ? requestBody.get("modelNumber").toString() : null;
        String versionNumber = requestBody.get("versionNumber") != null ? requestBody.get("versionNumber").toString()
                : null;
        Object holdObj = requestBody.get("hold");

        System.out.println("Updating " + modelNumber + "_" + versionNumber + " for hold");

        if (modelNumber == null || modelNumber.isBlank() || versionNumber == null || versionNumber.isBlank()
                || holdObj == null) {
            return ResponseEntity.badRequest()
                    .body(CustomResponse.failure("Invalid input: modelNumber, versionNumber and hold are required"));
        }

        boolean hold = (holdObj instanceof Boolean)
                ? (Boolean) holdObj
                : Boolean.parseBoolean(holdObj.toString());

        boolean updated = civitaiSQL_Service.update_hold_from_offline_download_list(modelNumber, versionNumber, hold);
        if (!updated) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(CustomResponse.failure("Record not found or IDs invalid"));
        }
        return ResponseEntity.ok(CustomResponse.success("Hold updated"));
    }

    @PostMapping(path = "/update-download-priority-from-offline-download_list")
    public ResponseEntity<CustomResponse<String>> UpdateDownloadPriorityFromOfflineDownloadList(
            @RequestBody Map<String, Object> requestBody) {

        String modelNumber = requestBody.get("modelNumber") != null ? requestBody.get("modelNumber").toString() : null;
        String versionNumber = requestBody.get("versionNumber") != null ? requestBody.get("versionNumber").toString()
                : null;
        Object priorityObj = requestBody.get("downloadPriority");

        System.out.println("Updating " + modelNumber + "_" + versionNumber + " for Download Priority");

        if (modelNumber == null || modelNumber.isBlank() || versionNumber == null || versionNumber.isBlank()
                || priorityObj == null) {
            return ResponseEntity.badRequest()
                    .body(CustomResponse
                            .failure("Invalid input: modelNumber, versionNumber and downloadPriority are required"));
        }

        int requestedPriority;
        try {
            requestedPriority = (priorityObj instanceof Number)
                    ? ((Number) priorityObj).intValue()
                    : Integer.parseInt(priorityObj.toString());
        } catch (NumberFormatException nfe) {
            return ResponseEntity.badRequest()
                    .body(CustomResponse.failure("downloadPriority must be an integer"));
        }

        boolean updated = civitaiSQL_Service.update_download_priority_from_offline_download_list(
                modelNumber, versionNumber, requestedPriority);

        if (!updated) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(CustomResponse.failure("Record not found or IDs invalid"));
        }
        return ResponseEntity.ok(CustomResponse.success("Download priority updated"));
    }

    @PostMapping(path = "/update-download-file-path-from-offline-download_list")
    public ResponseEntity<CustomResponse<String>> updateDownloadFilePathFromOfflineDownloadList(
            @RequestBody Map<String, Object> requestBody) {

        String modelNumber = requestBody.get("modelNumber") != null
                ? requestBody.get("modelNumber").toString()
                : null;

        String versionNumber = requestBody.get("versionNumber") != null
                ? requestBody.get("versionNumber").toString()
                : null;

        String downloadFilePath = requestBody.get("downloadFilePath") != null
                ? requestBody.get("downloadFilePath").toString()
                : null;

        if (modelNumber == null || modelNumber.isBlank()
                || versionNumber == null || versionNumber.isBlank()
                || downloadFilePath == null || downloadFilePath.isBlank()) {

            return ResponseEntity.badRequest()
                    .body(CustomResponse.failure(
                            "Invalid input: modelNumber, versionNumber and downloadFilePath are required"));
        }

        boolean updated = civitaiSQL_Service.update_downloadFilePath_from_offline_download_list(
                modelNumber, versionNumber, downloadFilePath);

        if (!updated) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(CustomResponse.failure("Record not found or IDs invalid"));
        }

        return ResponseEntity.ok(CustomResponse.success("downloadFilePath updated"));
    }

    @PostMapping("/bulk-patch-offline-download_list")
    public ResponseEntity<CustomResponse<Map<String, Object>>> bulkPatchOfflineDownloadList(
            @RequestBody Map<String, Object> body) {

        Map<String, Object> result = civitaiSQL_Service.bulkPatchOfflineDownloadList(body);

        // result contains: requested, updated
        return ResponseEntity.ok(CustomResponse.success("bulk patch done", result));
    }

}
