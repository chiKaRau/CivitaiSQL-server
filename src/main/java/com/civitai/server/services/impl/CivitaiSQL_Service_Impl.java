package com.civitai.server.services.impl;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.civitai.server.exception.CustomDatabaseException;
import com.civitai.server.exception.CustomException;
import com.civitai.server.models.dto.FullModelRecordDTO;
import com.civitai.server.models.dto.Models_DTO;
import com.civitai.server.models.dto.PageResponse;
import com.civitai.server.models.dto.Tables_DTO;
import com.civitai.server.models.dto.TagCountDTO;
import com.civitai.server.models.dto.TopTagsRequest;
import com.civitai.server.models.entities.civitaiSQL.Creator_Table_Entity;
import com.civitai.server.models.entities.civitaiSQL.Models_Descriptions_Table_Entity;
import com.civitai.server.models.entities.civitaiSQL.Models_Details_Table_Entity;
import com.civitai.server.models.entities.civitaiSQL.Models_Images_Table_Entity;
import com.civitai.server.models.entities.civitaiSQL.Models_Offline_Table_Entity;
import com.civitai.server.models.entities.civitaiSQL.Models_Table_Entity;
import com.civitai.server.models.entities.civitaiSQL.Models_Urls_Table_Entity;
import com.civitai.server.models.entities.civitaiSQL.Recycle_Table_Entity;
import com.civitai.server.models.entities.civitaiSQL.VisitedPath_Table_Entity;
import com.civitai.server.repositories.civitaiSQL.Creator_Table_Repository;
import com.civitai.server.repositories.civitaiSQL.Models_Descriptions_Table_Repository;
import com.civitai.server.repositories.civitaiSQL.Models_Details_Table_Repository;
import com.civitai.server.repositories.civitaiSQL.Models_Images_Table_Repository;
import com.civitai.server.repositories.civitaiSQL.Models_Offline_Table_Repository;
import com.civitai.server.repositories.civitaiSQL.Models_Table_Repository;
import com.civitai.server.repositories.civitaiSQL.Models_Table_Repository_Specification;
import com.civitai.server.repositories.civitaiSQL.Models_Urls_Table_Repository;
import com.civitai.server.repositories.civitaiSQL.Recycle_Table_Repository;
import com.civitai.server.repositories.civitaiSQL.VisitedPath_Table_Repository;
import com.civitai.server.repositories.civitaiSQL.impl.ModelsRepositoryFTS;
import com.civitai.server.services.CivitaiSQL_Service;
import com.civitai.server.services.Civitai_Service;
import com.civitai.server.specification.civitaiSQL.Models_Table_Specification;
import com.civitai.server.utils.CustomResponse;
import com.civitai.server.utils.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import org.springframework.transaction.annotation.Transactional;

@Service
public class CivitaiSQL_Service_Impl implements CivitaiSQL_Service {
        // This is where we implement our business logic
        // of how to interact with our database
        // such as what kind of method to query our database
        // we can setup a findAll to find all data
        // we can setup a findByID to find a single data

        private final Models_Table_Repository models_Table_Repository;
        private final Models_Descriptions_Table_Repository models_Descriptions_Table_Repository;
        private final Models_Urls_Table_Repository models_Urls_Table_Repository;
        private final Models_Details_Table_Repository models_Details_Table_Repository;
        private final Models_Images_Table_Repository models_Images_Table_Repository;
        private final Models_Table_Repository_Specification models_Table_Repository_Specification;
        private final Models_Offline_Table_Repository models_Offline_Table_Repository;
        private final Creator_Table_Repository creator_Table_Repository;
        private final VisitedPath_Table_Repository visitedPath_Table_Repository;
        private final ModelsRepositoryFTS modelsRepositoryFTS;
        private final Recycle_Table_Repository recycle_Table_Repository;
        private final ObjectMapper objectMapper;
        private final Civitai_Service civitai_Service;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        private static final Logger log = LoggerFactory.getLogger(CivitaiSQL_Service_Impl.class);

        @PostConstruct
        public void civitaiSQL_Service_Startup() {
                // System.out.println("civitaiSQL_Service_Startup");
                // updateMainModelName2();
        }

        public void updateMainModelName2() {

                // this one would find the database dto then call the civitai api
                // and update the MainModelName then save it

                // Step 1: Get all model_number and ids where mainModelName is null, limited to
                // the first 1000 records
                List<Object[]> results = models_Table_Repository.findModelNumberAndIdWhereMainModelNameIsNull();

                // Process only the first 1000 records
                int limit = 2000;
                for (int i = 0; i < results.size() && i < limit; i++) {
                        Object[] row = results.get(i);
                        String modelNumber = (String) row[0];
                        Integer id = (Integer) row[1];

                        try {
                                // Call the external service and update the model
                                Optional<Models_DTO> entityOptional = find_one_models_DTO_from_all_tables_by_id(id);

                                Optional<Map<String, Object>> modelOptional = civitai_Service
                                                .findModelByModelID(modelNumber);

                                if (entityOptional != null && entityOptional.isPresent() && modelOptional != null
                                                && modelOptional.isPresent()) {
                                        Models_DTO entity = entityOptional.get();
                                        Map<String, Object> model = modelOptional.get();
                                        entity.setMainModelName(Optional.ofNullable((String) model.get("name"))
                                                        .orElse(null));
                                        updateModelsTable(entity, id);
                                        System.out.println((i + 1) + " # - id " + id + " # : " + entity.getName()
                                                        + " has been updated.");

                                }
                        } catch (Exception e) {

                                // Call the external service and update the model
                                Optional<Models_DTO> entityOptional = find_one_models_DTO_from_all_tables_by_id(id);

                                if (entityOptional != null && entityOptional.isPresent()) {
                                        Models_DTO entity = entityOptional.get();
                                        entity.setUrlAccessable(false);
                                        updateModelsTable(entity, id);
                                        System.out.println((i + 1) + " # - id " + id + " # : " + entity.getName()
                                                        + ", the urlAccessable has set to false");

                                }

                                // Handle any other unexpected errors
                                System.err.println("An unexpected error occurred: on models_Table id: " + id);
                                limit++;
                        }

                        // Step 4: Add a 3-second delay before processing the next record
                        try {
                                Thread.sleep(3000); // 3000 milliseconds = 3 seconds
                        } catch (InterruptedException e) {
                                Thread.currentThread().interrupt(); // Restore interrupted status
                                throw new RuntimeException("Thread was interrupted", e);
                        }
                }

                System.out.println("updateMainModelName - Complete");

        }

        public void updateMainModelName1() {

                // this one would find the database id then call the civitai api
                // then create a new dto
                // and update the MainModelName only then save it

                // Step 1: Get all model_number and ids where mainModelName is null, limited to
                // the first 1000 records
                List<Object[]> results = models_Table_Repository.findModelNumberAndIdWhereMainModelNameIsNull();

                // Process only the first 1000 records
                int limit = 10;
                for (int i = 0; i < results.size() && i < limit; i++) {
                        Object[] row = results.get(i);
                        String modelNumber = (String) row[0];
                        Integer id = (Integer) row[1];
                        String category = (String) row[2];
                        String url = "https://civitai.com/models/" + modelNumber;

                        try {
                                // Call the external service and update the model
                                Optional<Models_DTO> newUpdateEntityOptional = create_models_DTO_by_Url(url, category,
                                                null);

                                if (newUpdateEntityOptional != null && newUpdateEntityOptional.isPresent()) {
                                        Models_DTO models_DTO = newUpdateEntityOptional.get();
                                        updateModelsTableByField(models_DTO, id, "main_model_name");
                                        System.out.println(i + " # - id " + id + " # :" + models_DTO.getName()
                                                        + " has been updated.");
                                }
                        } catch (HttpClientErrorException e) {
                                // Handle specific 404 Not Found error
                                if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                                        System.err.println("Model not found for ID " + modelNumber + ". Skipping...");
                                } else {
                                        // Handle other HTTP errors
                                        System.err.println("HTTP error occurred on models_Table id: " + id);
                                }
                                limit++;
                        } catch (Exception e) {
                                // Handle any other unexpected errors
                                System.err.println("An unexpected error occurred: on models_Table id: " + id);
                                limit++;
                        }

                        // Step 4: Add a 3-second delay before processing the next record
                        try {
                                Thread.sleep(3000); // 3000 milliseconds = 3 seconds
                        } catch (InterruptedException e) {
                                Thread.currentThread().interrupt(); // Restore interrupted status
                                throw new RuntimeException("Thread was interrupted", e);
                        }
                }

                System.out.println("updateMainModelName - Complete");

        }

        // CRUD, CREATE, READ, UPDATE, DELETE
        public CivitaiSQL_Service_Impl(Models_Table_Repository models_Table_Repository,
                        Models_Descriptions_Table_Repository models_Descriptions_Table_Repository,
                        Models_Urls_Table_Repository models_Urls_Table_Repository,
                        Models_Details_Table_Repository models_Details_Table_Repository,
                        Models_Images_Table_Repository models_Images_Table_Repository,
                        Models_Offline_Table_Repository models_Offline_Table_Repository,
                        Creator_Table_Repository creator_Table_Repository,
                        Models_Table_Repository_Specification models_Table_Repository_Specification,
                        VisitedPath_Table_Repository visitedPath_Table_Repository,
                        Recycle_Table_Repository recycle_Table_Repository,
                        ModelsRepositoryFTS modelsRepositoryFTS,
                        ObjectMapper objectMapper,
                        Civitai_Service civitai_Service) {
                this.models_Table_Repository = models_Table_Repository;
                this.models_Descriptions_Table_Repository = models_Descriptions_Table_Repository;
                this.models_Urls_Table_Repository = models_Urls_Table_Repository;
                this.models_Details_Table_Repository = models_Details_Table_Repository;
                this.models_Images_Table_Repository = models_Images_Table_Repository;
                this.models_Offline_Table_Repository = models_Offline_Table_Repository;
                this.creator_Table_Repository = creator_Table_Repository;
                this.models_Table_Repository_Specification = models_Table_Repository_Specification;
                this.visitedPath_Table_Repository = visitedPath_Table_Repository;
                this.recycle_Table_Repository = recycle_Table_Repository;
                this.civitai_Service = civitai_Service;
                this.objectMapper = objectMapper;
                this.modelsRepositoryFTS = modelsRepositoryFTS;
        }

        @Override
        public Optional<List<String>> find_all_categories() {
                List<String> categories = models_Table_Repository.findAllCategories();

                if (categories != null) {

                        return Optional.of(categories);
                } else {
                        return Optional.empty();
                }

                // return Optional.ofNullable(categories); above if code is equipviemnt with
                // this
        }

        @Override
        public Optional<List<Models_Table_Entity>> find_all_from_models_table() {
                try {

                        List<Models_Table_Entity> entities = StreamSupport.stream(models_Table_Repository
                                        .findAll()
                                        .spliterator(),
                                        false)
                                        .collect(Collectors.toList());

                        return entities.isEmpty() ? Optional.empty() : Optional.of(entities);
                } catch (DataAccessException e) {
                        // Log and handle database-related exceptions
                        log.error("Database error while finding all records from models_table", e);
                        throw new CustomDatabaseException("An unexpected database error occurred", e);
                } catch (Exception e) {
                        // Log and handle other types of exceptions
                        log.error("Unexpected error while finding all records from models_table", e);
                        throw new CustomException("An unexpected error occurred", e);
                        // Alternatively, return a fallback response for less critical errors
                        // return Collections.emptyList();
                }
        }

        @Override
        public Optional<Map<String, List<Models_DTO>>> find_lastest_three_models_DTO_in_each_category_from_all_table() {
                try {
                        List<String> categories = models_Table_Repository.findAllCategories();

                        if (categories != null) {
                                Map<String, List<Models_DTO>> latestModelsDTOList = categories.stream()
                                                .collect(Collectors.toMap(
                                                                category -> category,
                                                                category -> models_Table_Repository
                                                                                .findLastThreeAddedRecordsID(category,
                                                                                                PageRequest.of(0, 3))
                                                                                .stream()
                                                                                .map(id -> find_one_models_DTO_from_all_tables_by_id(
                                                                                                id))
                                                                                .filter(Optional::isPresent)
                                                                                .map(Optional::get)
                                                                                .collect(Collectors.toList())));

                                return latestModelsDTOList.isEmpty() ? Optional.empty()
                                                : Optional.of(latestModelsDTOList);
                        }

                        return Optional.empty();

                } catch (DataAccessException e) {
                        log.error("Database error while finding the record from models_table", e);
                        throw new CustomDatabaseException("An unexpected database error occurred", e);
                } catch (Exception e) {
                        log.error("Unexpected error while finding the record from models_table", e);
                        throw new CustomException("An unexpected error occurred", e);
                }
        }

        /*
         * @Override
         * public Optional<Map<String, List<Models_DTO>>>
         * find_lastest_three_models_DTO_in_each_category_from_all_table() {
         * try {
         * 
         * List<String> categories = models_Table_Repository.findAllCategories();
         * 
         * Map<String, List<Models_DTO>> latestModelsDTOList = new HashMap<>();
         * 
         * PageRequest pageable = PageRequest.of(0, 3);
         * 
         * //Check if the category list is exist
         * if (categories != null) {
         * //Map through each category
         * for (String category : categories) {
         * //Define an empty list for every loop
         * List<Models_DTO> modelsDTOList = new ArrayList<>();
         * 
         * //Retrieve a list of id which contains the latest records for each category
         * List<Integer> idList = models_Table_Repository
         * .findLastThreeAddedRecordsID(category, pageable);
         * 
         * //Map through each id and retrive its Models_DTO
         * for (Integer id : idList) {
         * Optional<Models_DTO> entityOptional = find_one_models_DTO_from_all_tables(
         * id);
         * if (entityOptional.isPresent()) {
         * Models_DTO entity = entityOptional.get();
         * //Add the Models_DTO into the list
         * modelsDTOList.add(entity);
         * }
         * }
         * 
         * //add the Models_DTO list into the hashObject
         * latestModelsDTOList.put(category, modelsDTOList);
         * 
         * }
         * }
         * 
         * return latestModelsDTOList.isEmpty() ? Optional.empty() :
         * Optional.of(latestModelsDTOList);
         * 
         * } catch (DataAccessException e) {
         * // Log and handle database-related exceptions
         * log.error("Database error while finding the record from models_table", e);
         * throw new CustomDatabaseException("An unexpected database error occurred",
         * e);
         * } catch (Exception e) {
         * // Log and handle other types of exceptions
         * log.error("Unexpected error while finding the record from models_table", e);
         * throw new CustomException("An unexpected error occurred", e);
         * }
         * }
         */

        @Override
        public Optional<Models_Table_Entity> find_one_from_models_table(Integer id) {
                try {
                        Optional<Models_Table_Entity> entityOptional = models_Table_Repository.findById(id);

                        return entityOptional.isPresent() ? entityOptional : Optional.empty();

                } catch (DataAccessException e) {
                        // Log and handle database-related exceptions
                        log.error("Database error while finding the record from models_table", e);
                        throw new CustomDatabaseException("An unexpected database error occurred", e);
                } catch (Exception e) {
                        // Log and handle other types of exceptions
                        log.error("Unexpected error while finding the record from models_table", e);
                        throw new CustomException("An unexpected error occurred", e);
                }
        }

        @Override
        public Boolean find_one_from_models_urls_table(String url) {
                try {
                        Models_Urls_Table_Entity entity = models_Urls_Table_Repository.findByUrl(url);
                        if (entity != null) {
                                return true;
                        } else {
                                return false;
                        }
                } catch (DataAccessException e) {
                        // Log and handle database-related exceptions
                        log.error("Database error while finding the record from models_table", e);
                        throw new CustomDatabaseException("An unexpected database error occurred", e);
                } catch (Exception e) {
                        // Log and handle other types of exceptions
                        log.error("Unexpected error while finding the record from models_table", e);
                        throw new CustomException("An unexpected error occurred", e);
                }
        }

        @Override
        public Long find_quantity_from_models_urls_table(String url) {
                try {
                        return models_Urls_Table_Repository.findQuantityByUrl(String.join("/", url.split("/", 6)));

                } catch (DataAccessException e) {
                        // Log and handle database-related exceptions
                        log.error("Database error while finding the record from models_table", e);
                        throw new CustomDatabaseException("An unexpected database error occurred", e);
                } catch (Exception e) {
                        // Log and handle other types of exceptions
                        log.error("Unexpected error while finding the record from models_table", e);
                        throw new CustomException("An unexpected error occurred", e);
                }
        }

        @Override
        public Long find_quantity_from_models_table(String url) {
                try {
                        String modelID = url.replaceAll(".*/models/(\\d+).*", "$1");

                        return models_Table_Repository.findQuantityByModelID(modelID);

                } catch (DataAccessException e) {
                        // Log and handle database-related exceptions
                        log.error("Database error while finding the record from models_table", e);
                        throw new CustomDatabaseException("An unexpected database error occurred", e);
                } catch (Exception e) {
                        // Log and handle other types of exceptions
                        log.error("Unexpected error while finding the record from models_table", e);
                        throw new CustomException("An unexpected error occurred", e);
                }
        }

        @Override
        public Optional<List<Tables_DTO>> find_all_from_all_tables() {
                try {
                        Iterable<Models_Table_Entity> entities = models_Table_Repository.findAll();

                        if (entities != null) {
                                List<Tables_DTO> entitiesList = StreamSupport.stream(entities.spliterator(), false)
                                                .map(model -> {
                                                        Tables_DTO tables_DTO = new Tables_DTO();
                                                        tables_DTO.setModels_Table_Entitiy(model);
                                                        tables_DTO.setModels_Descriptions_Table_Entity(
                                                                        models_Descriptions_Table_Repository
                                                                                        .findById(model.getId())
                                                                                        .orElse(null));
                                                        tables_DTO.setModels_Urls_Table_Entity(
                                                                        models_Urls_Table_Repository
                                                                                        .findById(model.getId())
                                                                                        .orElse(null));
                                                        tables_DTO.setModels_Details_Table_Entity(
                                                                        models_Details_Table_Repository
                                                                                        .findById(model.getId())
                                                                                        .orElse(null));
                                                        tables_DTO.setModels_Images_Table_Entity(
                                                                        models_Images_Table_Repository
                                                                                        .findById(model.getId())
                                                                                        .orElse(null));
                                                        return tables_DTO;
                                                }).collect((Collectors.toList()));

                                return Optional.of(entitiesList);
                        } else {
                                return Optional.empty();
                        }
                } catch (DataAccessException e) {
                        // Log and handle database-related exceptions
                        log.error("Database error while finding all records from all tables", e);
                        throw new CustomDatabaseException("An unexpected database error occurred", e);
                } catch (Exception e) {
                        // Log and handle other types of exceptions
                        log.error("Unexpected error while finding all records from all tables", e);
                        throw new CustomException("An unexpected error occurred", e);
                        // Alternatively, return a fallback response for less critical errors
                        // return Collections.emptyList();
                }
        }

        @Override
        public Optional<Tables_DTO> find_one_tables_DTO_from_all_tables(Integer id) {
                try {
                        Optional<Models_Table_Entity> entityOptional = models_Table_Repository.findById(id);
                        if (entityOptional.isPresent()) {
                                Models_Table_Entity model = entityOptional.get();

                                Tables_DTO tables_DTO = new Tables_DTO();
                                tables_DTO.setModels_Table_Entitiy(model);
                                tables_DTO.setModels_Descriptions_Table_Entity(
                                                models_Descriptions_Table_Repository
                                                                .findById(model.getId())
                                                                .orElse(null));
                                tables_DTO.setModels_Urls_Table_Entity(
                                                models_Urls_Table_Repository.findById(model.getId())
                                                                .orElse(null));
                                tables_DTO.setModels_Details_Table_Entity(
                                                models_Details_Table_Repository.findById(model.getId())
                                                                .orElse(null));
                                tables_DTO.setModels_Images_Table_Entity(
                                                models_Images_Table_Repository.findById(model.getId())
                                                                .orElse(null));
                                return Optional.of(tables_DTO);
                        } else {
                                // If the model is not present, return an empty Optional
                                return Optional.empty();
                        }

                } catch (DataAccessException e) {
                        // Log and handle database-related exceptions
                        log.error("Database error while finding entities", e);
                        throw new CustomDatabaseException("An unexpected database error occurred", e);
                } catch (Exception e) {
                        // Log and handle other types of exceptions
                        log.error("Unexpected error while finding entities", e);
                        throw new CustomException("An unexpected error occurred", e);
                }
        }

        @Override
        public Optional<Models_DTO> find_one_models_DTO_from_all_tables_by_id(Integer id) {
                try {
                        Optional<Models_Table_Entity> entityOptional = models_Table_Repository.findById(id);
                        if (entityOptional.isPresent()) {
                                Models_Table_Entity entity = entityOptional.get();

                                Models_DTO models_DTO = convertToDTO(entity);

                                return Optional.of(models_DTO);
                        } else {
                                // If the model is not present, return an empty Optional
                                return Optional.empty();
                        }

                } catch (DataAccessException e) {
                        // Log and handle database-related exceptions
                        log.error("Database error while finding the record from all table", e);
                        throw new CustomDatabaseException("An unexpected database error occurred", e);
                } catch (Exception e) {
                        // Log and handle other types of exceptions
                        log.error("Unexpected error while finding the record from all table", e);
                        throw new CustomException("An unexpected error occurred", e);
                }

        }

        @Override
        public Optional<Models_DTO> find_one_models_DTO_from_all_tables_by_url(String url) {

                Models_Urls_Table_Entity entity = models_Urls_Table_Repository.findByUrl(url);
                if (entity != null) {

                        Models_DTO models_DTO = convertToDTO(
                                        models_Table_Repository.findById(entity.getId()).orElse(null));

                        return Optional.of(models_DTO);
                } else {
                        return Optional.empty();

                }
        }

        @Override
        public Optional<List<Models_DTO>> find_List_of_models_DTO_from_all_tables_by_modelID(String modelID) {

                List<Models_Table_Entity> entityList = models_Table_Repository.findByModelNumber(modelID);

                if (entityList != null && !entityList.isEmpty()) {
                        // Use stream and map to convert each entity to DTO
                        List<Models_DTO> modelsDTOList = entityList.stream()
                                        .map(this::convertToDTO)
                                        .collect(Collectors.toList());

                        return Optional.of(modelsDTOList);
                } else {
                        return Optional.empty();
                }

        }

        @Override
        public Optional<List<String>> find_List_of_Version_Number_from_model_tables_by_Url(String url) {
                String modelID = url.replaceAll(".*/models/(\\d+).*", "$1");

                List<String> entityList = models_Table_Repository.findListofVersionNumberByModelNumber(modelID);

                if (entityList != null && !entityList.isEmpty()) {
                        return Optional.of(entityList);
                } else {
                        return Optional.empty();
                }
        }

        @Override
        public Optional<List<Models_DTO>> find_List_of_models_DTO_from_all_tables_by_alike_url(String name) {
                List<Models_Urls_Table_Entity> entityList = models_Urls_Table_Repository.findAlikeUrl(name);

                if (entityList != null && !entityList.isEmpty()) {
                        // Use stream and map to convert each entity to DTO
                        List<Models_DTO> modelsDTOList = entityList.stream()
                                        .map(element -> {
                                                Optional<Models_Table_Entity> entityOptional = models_Table_Repository
                                                                .findById(element.getId());

                                                return entityOptional.isPresent() ? convertToDTO(entityOptional.get())
                                                                : null;

                                        })
                                        .collect(Collectors.toList());

                        return Optional.of(modelsDTOList);
                } else {
                        return Optional.empty();
                }
        }

        @Override
        public Optional<List<Models_DTO>> find_List_of_models_DTO_from_all_tables_by_alike_tags(String name) {
                List<Models_Table_Entity> entityList = models_Table_Repository.findAlikeTags(name);

                if (entityList != null && !entityList.isEmpty()) {
                        // Use stream and map to convert each entity to DTO
                        List<Models_DTO> modelsDTOList = entityList.stream()
                                        .map(this::convertToDTO)
                                        .collect(Collectors.toList());

                        return Optional.of(modelsDTOList);
                } else {
                        return Optional.empty();
                }
        }

        @Override
        public Optional<List<Models_DTO>> find_List_of_models_DTO_from_all_tables_by_alike_triggerWords(String name) {
                List<Models_Table_Entity> entityList = models_Table_Repository.findAlikeTriggerWords(name);

                if (entityList != null && !entityList.isEmpty()) {
                        // Use stream and map to convert each entity to DTO
                        List<Models_DTO> modelsDTOList = entityList.stream()
                                        .map(this::convertToDTO)
                                        .collect(Collectors.toList());

                        return Optional.of(modelsDTOList);
                } else {
                        return Optional.empty();
                }
        }

        @Override
        public Optional<List<Models_DTO>> find_List_of_models_DTO_from_all_tables_by_alike_name(String name) {
                List<Models_Table_Entity> entityList = models_Table_Repository.findAlikeName(name);

                if (entityList != null && !entityList.isEmpty()) {
                        // Use stream and map to convert each entity to DTO
                        List<Models_DTO> modelsDTOList = entityList.stream()
                                        .map(this::convertToDTO)
                                        .collect(Collectors.toList());

                        return Optional.of(modelsDTOList);
                } else {
                        return Optional.empty();
                }
        }

        // Put near your service class
        private String buildBooleanQueryForLikeEquivalence(List<String> keywords) {
                // Emulate old LIKE '%kw%' feel:
                // - AND semantics with '+' prefix
                // - Phrase support when a kw has spaces
                // - Suffix wildcard * for broader match (MySQL FTS: only suffix allowed)
                // - Skip wildcard for very short tokens (<3) due to ft_min_token_size default
                return keywords.stream()
                                .map(k -> {
                                        String kw = k.trim();
                                        boolean phrase = kw.contains(" ");
                                        String term = phrase ? "\"" + kw + "\"" : kw;
                                        // add * only for single-word terms with length >=3
                                        if (!phrase && kw.length() >= 3)
                                                term = term + "*";
                                        return "+" + term;
                                })
                                .reduce((a, b) -> a + " " + b)
                                .orElse("");
        }

        boolean useFts = Boolean.parseBoolean(System.getenv().getOrDefault("SEARCH_MODE_FTS", "true"));

        @Override
        public Optional<List<Models_DTO>> find_List_of_models_DTO_from_all_tables_by_alike_tagsList(
                        List<String> tagsList) {
                List<Models_Table_Entity> entityList;

                if (useFts) {
                        String booleanQuery = buildBooleanQueryForLikeEquivalence(tagsList);

                        // 1) Fast path: FTS
                        List<Models_Table_Entity> fts = modelsRepositoryFTS.searchAllByBooleanFTS(booleanQuery);

                        // 2) Fallback/top-up: old Specification (to keep behavior identical)
                        // Use it only if FTS is empty OR you want to preserve any edge cases (short
                        // tokens, non-token substrings).
                        if (fts.isEmpty()) {
                                Specification<Models_Table_Entity> spec = Models_Table_Specification
                                                .findByTagsList(tagsList);
                                entityList = models_Table_Repository_Specification.findAll(spec);
                        } else {
                                entityList = fts;
                        }
                } else {
                        // Original behavior
                        Specification<Models_Table_Entity> spec = Models_Table_Specification.findByTagsList(tagsList);
                        entityList = models_Table_Repository_Specification.findAll(spec);
                }

                if (entityList.isEmpty())
                        return Optional.empty();

                return Optional.of(entityList.stream().map(this::convertToDTO).collect(Collectors.toList()));
        }

        @Override
        public Optional<List<String>> find_version_numbers_for_model(String modelNumber, List<String> versionNumbers) {
                List<String> existingVersionNumbersList = new ArrayList<>();

                try {
                        // Retrieve all models with the specified model number
                        List<Models_Table_Entity> entities = models_Table_Repository.findByModelNumber(modelNumber);
                        if (entities.isEmpty()) {
                                // If no models found, return an empty Optional
                                return Optional.empty();
                        }

                        // Create a set of existing version numbers for the given model number
                        Set<String> existingVersionNumbers = entities.stream()
                                        .map(Models_Table_Entity::getVersionNumber)
                                        .collect(Collectors.toSet());

                        // Add each version number in the request that exists in the existing version
                        // numbers
                        for (String versionNumber : versionNumbers) {
                                if (existingVersionNumbers.contains(versionNumber)) {
                                        existingVersionNumbersList.add(versionNumber);
                                }
                        }
                } catch (DataAccessException e) {
                        log.error("Database error while checking version numbers for model", e);
                        throw new CustomDatabaseException("An unexpected database error occurred", e);
                } catch (Exception e) {
                        log.error("Unexpected error while checking version numbers for model", e);
                        throw new CustomException("An unexpected error occurred", e);
                }

                return Optional.of(existingVersionNumbersList);
        }

        // Transcation Actions
        @Override
        @Transactional
        public void create_record_to_all_tables(Models_DTO dto) {
                try {
                        // Step 1: Save the Models_Table_Entities entity to generate the ID
                        Models_Table_Entity models_Table_Entities = Models_Table_Entity.builder()
                                        .name(dto.getName())
                                        .mainModelName(dto.getMainModelName())
                                        .tags(JsonUtils.convertObjectToString(dto.getTags()))
                                        .category(dto.getCategory())
                                        .versionNumber(dto.getVersionNumber())
                                        .modelNumber(dto.getModelNumber())
                                        .localPath(dto.getLocalPath())
                                        .triggerWords(JsonUtils.convertObjectToString(dto.getTriggerWords()))
                                        .nsfw(dto.getNsfw())
                                        .flag(dto.getFlag())
                                        .urlAccessable(dto.getUrlAccessable())
                                        .build();

                        models_Table_Entities = models_Table_Repository.save(models_Table_Entities);

                        // Step 2: Use the generated ID to set the foreign key in other entities
                        Models_Urls_Table_Entity models_Urls_Table_Entities = Models_Urls_Table_Entity.builder()
                                        .id(models_Table_Entities.getId()) // Set the foreign key
                                        .url(dto.getUrl())
                                        // set other fields as needed
                                        .build();

                        Models_Descriptions_Table_Entity models_Descriptions_Table_Entities = Models_Descriptions_Table_Entity
                                        .builder()
                                        .id(models_Table_Entities.getId()) // Set the foreign key
                                        .description(dto.getDescription())
                                        // set other fields as needed
                                        .build();

                        Models_Details_Table_Entity models_Details_Table_Entities = Models_Details_Table_Entity
                                        .builder()
                                        .id(models_Table_Entities.getId()) // Set the foreign key
                                        .type(dto.getType())
                                        .stats(dto.getStats())
                                        .uploaded(dto.getUploaded()) // Parse as LocalDate
                                        // and add the
                                        // time component
                                        .baseModel(dto.getBaseModel())
                                        .hash(dto.getHash())
                                        .usageTips(dto.getUsageTips())
                                        .creatorName(dto.getCreatorName())
                                        // set other fields as needed
                                        .build();

                        Models_Images_Table_Entity models_Images_Table_Entities = Models_Images_Table_Entity
                                        .builder()
                                        .id(models_Table_Entities.getId()) // Set the foreign key
                                        .imageUrls(JsonUtils.convertObjectToString(dto.getImageUrls()))
                                        .build();

                        // Step 3: Save the child entities
                        models_Urls_Table_Repository.save(models_Urls_Table_Entities);
                        models_Descriptions_Table_Repository.save(models_Descriptions_Table_Entities);
                        models_Details_Table_Repository.save(models_Details_Table_Entities);
                        models_Images_Table_Repository.save(models_Images_Table_Entities);

                } catch (Exception e) {
                        // Log the exception for internal debugging
                        log.error("Error while createing a record into all table", e);
                        // Throw a custom exception for critical errors
                        throw new CustomException("An unexpected error occurred", e);
                        // Alternatively, return a fallback response for less critical errors
                        // return Collections.emptyList();
                }
        }

        @Override
        @Transactional
        public void update_record_to_all_tables_by_id(Models_DTO dto, Integer id) {
                try {

                        updateModelsTable(dto, id);
                        updateModelsUrlsTable(dto, id);
                        updateModelsDescriptionTable(dto, id);
                        updateModelsDetailsTable(dto, id);
                        updateModelsImagesTable(dto, id);

                } catch (Exception e) {
                        // Log the exception for internal debugging
                        log.error("Error while createing a record into all table", e);
                        // Throw a custom exception for critical errors
                        throw new CustomException("An unexpected error occurred", e);
                        // Alternatively, return a fallback response for less critical errors
                        // return Collections.emptyList();
                }
        }

        @Override
        @Transactional
        public void delete_record_to_all_table_by_id(Integer id) {
                try {

                        // Delete records from tables with no foreign key constraints
                        models_Urls_Table_Repository.deleteById(id);
                        models_Descriptions_Table_Repository.deleteById(id);
                        models_Images_Table_Repository.deleteById(id);
                        models_Details_Table_Repository.deleteById(id);

                        // Delete the record from the main table (Models_Table) last
                        models_Table_Repository.deleteById(id);

                } catch (Exception e) {
                        // Log the exception for internal debugging
                        log.error("Error while deleting a record into all table", e);
                        // Throw a custom exception for critical errors
                        throw new CustomException("An unexpected error occurred", e);
                        // Alternatively, return a fallback response for less critical errors
                        // return Collections.emptyList();
                }
        }

        @Override
        @Transactional
        public void delete_latest_record_from_all_tables() {
                try {
                        // Step 1: Find the latest Models_Table_Entities
                        Models_Table_Entity model = models_Table_Repository
                                        .findTopByOrderByIdDesc();
                        if (model != null) {
                                // Step 2: Delete the child entities based on the latest Models_Table_Entities
                                // ID
                                models_Urls_Table_Repository.deleteById(model.getId());
                                models_Descriptions_Table_Repository.deleteById(model.getId());
                                models_Details_Table_Repository.deleteById(model.getId());
                                models_Images_Table_Repository.deleteById(model.getId());

                                // Step 3: Delete the latest Models_Table_Entities
                                models_Table_Repository.delete(model);
                        }
                } catch (Exception e) {
                        // Log the exception for internal debugging
                        log.error("Error while deleting a record into all table", e);
                        // Throw a custom exception for critical errors
                        throw new CustomException("An unexpected error occurred", e);
                        // Alternatively, return a fallback response for less critical errors
                        // return Collections.emptyList();
                }

        }

        @Override
        @Transactional
        public void delete_every_record_from_all_tables() {
                try {
                        // Delete all rows in each table
                        models_Urls_Table_Repository.deleteAll();
                        models_Descriptions_Table_Repository.deleteAll();
                        models_Details_Table_Repository.deleteAll();
                        models_Images_Table_Repository.deleteAll();

                        // Delete all rows in the main table
                        models_Table_Repository.deleteAll();
                } catch (Exception e) {
                        // Log the exception for internal debugging
                        log.error("rror while deleting all record into all table", e);
                        // Throw a custom exception for critical errors
                        throw new CustomException("An unexpected error occurred", e);
                        // Alternatively, return a fallback response for less critical errors
                        // return Collections.emptyList();
                }
        }

        @Override
        @Transactional
        public void create_dummy_record_to_all_tables() {
                try {
                        Models_DTO dto = new Models_DTO();
                        dto.setName("Dummy1");
                        dto.setTags(List.of("tag1", "tag2"));
                        dto.setCategory("Category2");
                        dto.setVersionNumber("2.0");
                        dto.setModelNumber("3.1");
                        dto.setTriggerWords(List.of("tag1", "tag2"));
                        dto.setUrl("www.dummy.com");
                        dto.setDescription("very very very long text");
                        dto.setType("Character");
                        dto.setStats("working");
                        dto.setUploaded(LocalDate.parse("2023-01-01", DateTimeFormatter.ISO_LOCAL_DATE));
                        dto.setBaseModel("SD1.5");
                        dto.setHash("1nk29j01");
                        dto.setUsageTips("awwaww");
                        dto.setCreatorName("jack");
                        dto.setNsfw(true);
                        dto.setImageUrls(List.of(
                                        Map.of("url", "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/cf92831a-1a9b-4cab-b4a9-995c90f0e3d4/width=450/3171199.jpeg",
                                                        "nsfw", "Soft", "width", 512, "height", 512)));
                        create_record_to_all_tables(dto);
                } catch (Exception e) {
                        // Log the exception for internal debugging
                        log.error("Error while creating a dummy record", e);
                        // Throw a custom exception for critical errors
                        throw new CustomException("An unexpected error occurred", e);
                        // Alternatively, return a fallback response for less critical errors
                        // return Collections.emptyList();
                }
        }

        // Utils
        @SuppressWarnings("unchecked")
        @Override
        public Models_DTO convertToDTO(Models_Table_Entity entity) {
                // Method to convert Models_Table_Entity to Models_DTO
                Tables_DTO tables_DTO = new Tables_DTO();

                tables_DTO.setModels_Table_Entitiy(entity);
                tables_DTO.setModels_Descriptions_Table_Entity(
                                models_Descriptions_Table_Repository
                                                .findById(entity.getId())
                                                .orElse(null));
                tables_DTO.setModels_Urls_Table_Entity(
                                models_Urls_Table_Repository.findById(entity.getId()).orElse(null));
                tables_DTO.setModels_Details_Table_Entity(
                                models_Details_Table_Repository.findById(entity.getId())
                                                .orElse(null));
                tables_DTO.setModels_Images_Table_Entity(
                                models_Images_Table_Repository.findById(entity.getId())
                                                .orElse(null));

                Models_DTO models_DTO = new Models_DTO();
                models_DTO.setId(tables_DTO.getModels_Table_Entitiy().getId());
                models_DTO.setName(tables_DTO.getModels_Table_Entitiy().getName());
                models_DTO.setMainModelName(tables_DTO.getModels_Table_Entitiy().getMainModelName());
                models_DTO.setMyRating(tables_DTO.getModels_Table_Entitiy().getMyRating());
                models_DTO.setTags(JsonUtils.convertStringToObject(
                                tables_DTO.getModels_Table_Entitiy().getTags(), List.class));
                models_DTO.setCategory(tables_DTO.getModels_Table_Entitiy().getCategory());
                models_DTO.setVersionNumber(tables_DTO.getModels_Table_Entitiy().getVersionNumber());
                models_DTO.setModelNumber(tables_DTO.getModels_Table_Entitiy().getModelNumber());
                models_DTO.setTriggerWords(JsonUtils.convertStringToObject(
                                tables_DTO.getModels_Table_Entitiy().getTriggerWords(), List.class));
                models_DTO.setLocalPath(tables_DTO.getModels_Table_Entitiy().getLocalPath());
                String rawLocalTags = tables_DTO.getModels_Table_Entitiy().getLocalTags();
                if (rawLocalTags != null) {
                        List<?> tempList = JsonUtils.convertStringToObject(rawLocalTags, List.class);
                        // Cast and filter if necessary, assuming all elements are strings:
                        List<String> localTags = tempList.stream()
                                        .map(Object::toString)
                                        .collect(Collectors.toList());
                        models_DTO.setLocalTags(localTags);
                } else {
                        models_DTO.setLocalTags(Collections.emptyList());
                }
                models_DTO.setUrl(tables_DTO.getModels_Urls_Table_Entity().getUrl());
                models_DTO.setDescription(
                                tables_DTO.getModels_Descriptions_Table_Entity().getDescription());
                models_DTO.setType(tables_DTO.getModels_Details_Table_Entity().getType());
                models_DTO.setStats(tables_DTO.getModels_Details_Table_Entity().getStats());
                models_DTO.setUploaded(tables_DTO.getModels_Details_Table_Entity().getUploaded());
                models_DTO.setBaseModel(tables_DTO.getModels_Details_Table_Entity().getBaseModel());
                models_DTO.setHash(tables_DTO.getModels_Details_Table_Entity().getHash());
                models_DTO.setUsageTips(tables_DTO.getModels_Details_Table_Entity().getUsageTips());
                models_DTO.setCreatorName(tables_DTO.getModels_Details_Table_Entity().getCreatorName());
                models_DTO.setNsfw(tables_DTO.getModels_Table_Entitiy().getNsfw());
                models_DTO.setImageUrls(JsonUtils.convertStringToObject(
                                tables_DTO.getModels_Images_Table_Entity().getImageUrls(), List.class));
                models_DTO.setFlag(tables_DTO.getModels_Table_Entitiy().getFlag());

                return models_DTO;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Optional<Models_DTO> create_models_DTO_by_Url(String url, String category, String downloadFilePath) {
                try {

                        String modelID = url.replaceAll(".*/models/(\\d+).*", "$1");

                        Optional<Map<String, Object>> modelOptional = civitai_Service.findModelByModelID(modelID);

                        String name = null, mainModelName = null, versionNumber = null, description = null, type = null,
                                        stats = null,
                                        hash = null, usageTips = null, creatorName = null,
                                        baseModel = null;

                        List<Map<String, Object>> images = new ArrayList<>();
                        List<String> tags = new ArrayList<>(), triggerWords = new ArrayList<>();

                        Boolean nsfw = false, flag = false;

                        LocalDate uploaded = null;

                        if (modelOptional.isPresent()) {
                                Map<String, Object> model = modelOptional.get();

                                // Retriving the version list
                                Optional<List<Map<String, Object>>> modelVersionList = Optional
                                                .ofNullable(model)
                                                .map(map -> (List<Map<String, Object>>) map.get("modelVersions"))
                                                .filter(list -> !list.isEmpty());

                                // For Version Number
                                URI uri = new URI(url);
                                String query = uri.getQuery();

                                if (query != null && query.contains("modelVersionId")) {
                                        String[] queryParams = query.split("&");
                                        for (String param : queryParams) {
                                                if (param.startsWith("modelVersionId=")) {
                                                        versionNumber = param.substring("modelVersionId=".length());
                                                }
                                        }
                                } else {
                                        versionNumber = modelVersionList.map(list -> list.get(0).get("id"))
                                                        .map(Object::toString)
                                                        .orElse(null);
                                }

                                // Retriving appropriate model from the version list
                                final String final_versionId = versionNumber;
                                List<Map<String, Object>> matchingVersionModel = Optional
                                                .ofNullable(modelVersionList.orElse(Collections.emptyList()))
                                                .orElse(Collections.emptyList())
                                                .stream()
                                                .filter(element -> {
                                                        Object idObject = element.get("id");
                                                        return final_versionId != null && final_versionId
                                                                        .equals(String.valueOf(idObject));
                                                })
                                                .collect(Collectors.toList());

                                // For main model Name
                                mainModelName = Optional.ofNullable((String) model.get("name"))
                                                .orElse(null);

                                // For NSFW
                                nsfw = Optional.ofNullable((Boolean) model.get("nsfw"))
                                                .orElse(null);

                                // For Tags
                                tags = Optional.ofNullable((List<String>) model.get("tags"))
                                                .orElse(null);

                                // For Trigger Words
                                triggerWords = matchingVersionModel.stream()
                                                .map(map -> (List<String>) map.get("trainedWords"))
                                                .filter(obj -> obj instanceof List) // Ensure it's an instance of List
                                                .map(obj -> (List<String>) obj) // Now we can safely cast
                                                .findFirst().orElse(null);

                                // For Description
                                description = Optional.ofNullable((String) model.get("description"))
                                                .orElse(null);

                                // For Type
                                type = Optional.ofNullable((String) model.get("type"))
                                                .orElse(null);

                                // For Stats
                                stats = matchingVersionModel.stream()
                                                .map(map -> (Map<String, Object>) map.get("stats"))
                                                .filter(data -> data != null)
                                                .findFirst()
                                                .map(data -> {
                                                        try {
                                                                return new ObjectMapper().writeValueAsString(data);
                                                        } catch (JsonProcessingException e) {
                                                                throw new RuntimeException(e);
                                                        }
                                                })
                                                .orElse(null);

                                // For Uploaded Date

                                String uploadedString = matchingVersionModel.stream()
                                                .map(map -> (String) map.get("createdAt"))
                                                .findFirst().orElse(null);

                                if (!uploadedString.endsWith("Z"))
                                        uploadedString += "Z"; // Ensure UTC format

                                uploaded = Instant.parse(uploadedString)
                                                .atZone(ZoneId.of("UTC"))
                                                .toLocalDate();

                                // For Model Name
                                name = matchingVersionModel.stream()
                                                .map(map -> (List<Map<String, Object>>) map.get("files"))
                                                .filter(files -> files != null)
                                                .flatMap(List::stream)
                                                .map(fileMap -> (String) fileMap.get("name"))
                                                .filter(modelName -> modelName != null)
                                                .findFirst()
                                                .map(modelName -> modelName.contains(".")
                                                                ? modelName.substring(0, modelName.indexOf('.'))
                                                                : modelName)
                                                .orElse(null);

                                // For Base Model
                                baseModel = matchingVersionModel.stream()
                                                .map(map -> (String) map.get("baseModel"))
                                                .findFirst().orElse(null);

                                // For Hash
                                hash = matchingVersionModel.stream()
                                                .map(map -> (List<Map<String, Object>>) map.get("files"))
                                                .filter(files -> files != null)
                                                .flatMap(List::stream)
                                                .map(file -> (Map<String, Object>) file.get("hashes"))
                                                .filter(hashes -> hashes != null)
                                                .map(hashes -> {
                                                        try {
                                                                // Convert the hashes map to a JSON string
                                                                return new ObjectMapper().writeValueAsString(hashes);
                                                        } catch (JsonProcessingException e) {
                                                                throw new RuntimeException(e);
                                                        }
                                                })
                                                .collect(Collectors.joining(","));

                                if (hash.length() > 1000) {
                                        hash = "hash too long; please check";
                                        flag = true;
                                }

                                // For Creator Name
                                creatorName = Optional.ofNullable((Map<String, Object>) model.get("creator"))
                                                .map(creator -> (String) creator.get("username"))
                                                .orElse(null);

                                // For Images Urls

                                List<String> imagesArray = matchingVersionModel.stream()
                                                .map(map -> (List<String>) map.get("images"))
                                                .findFirst().orElse(null);

                                // Iterate over each element in the 'images' list
                                for (Object element : imagesArray) {
                                        Map<String, Object> myHashObject = new HashMap<>();

                                        // Replace 'images' with 'element' to access the properties of each object
                                        myHashObject.put("url", (String) ((Map<String, Object>) element).get("url"));
                                        myHashObject.put("nsfw", (String) ((Map<String, Object>) element).get("nsfw"));
                                        myHashObject.put("width", (int) ((Map<String, Object>) element).get("width"));
                                        myHashObject.put("height", (int) ((Map<String, Object>) element).get("height"));

                                        // Add the map to the list
                                        images.add(myHashObject);
                                }

                                // Create a Models_DTO
                                Models_DTO dto = new Models_DTO();
                                dto.setUrl(url);
                                dto.setName(name);
                                dto.setMainModelName(mainModelName);
                                dto.setModelNumber(modelID);
                                dto.setVersionNumber(versionNumber);
                                dto.setCategory(category);
                                dto.setNsfw(nsfw);
                                dto.setFlag(flag);
                                dto.setTags(tags);
                                dto.setTriggerWords(triggerWords);
                                dto.setDescription(description);
                                dto.setLocalPath(downloadFilePath);
                                dto.setType(type);
                                dto.setStats(stats);
                                dto.setUploaded(uploaded);
                                dto.setBaseModel(baseModel);
                                dto.setHash(hash);
                                dto.setUsageTips(usageTips);
                                dto.setCreatorName(creatorName);
                                dto.setImageUrls(images);
                                dto.setUrlAccessable(true);

                                return Optional.of(dto);
                        } else {
                                return Optional.empty();
                        }

                } catch (Exception e) {
                        // Log the exception for internal debugging
                        log.error("Error while createing a record into all table", e);
                        // Throw a custom exception for critical errors
                        throw new CustomException("An unexpected error occurred", e);
                        // Alternatively, return a fallback response for less critical errors
                        // return Collections.emptyList();
                }
        }

        @Transactional
        private void updateModelsTable(Models_DTO dto, Integer id) {
                Optional<Models_Table_Entity> entityOptional = models_Table_Repository
                                .findById(id);
                if (entityOptional.isPresent()) {
                        Models_Table_Entity entity = entityOptional.get();
                        entity.setName(dto.getName());
                        entity.setMainModelName(dto.getMainModelName());
                        entity.setTags(JsonUtils.convertObjectToString(dto.getTags()));
                        entity.setCategory(dto.getCategory());
                        entity.setVersionNumber(dto.getVersionNumber());
                        entity.setModelNumber(dto.getModelNumber());
                        entity.setTriggerWords(JsonUtils.convertObjectToString(dto.getTriggerWords()));
                        entity.setNsfw(dto.getNsfw());
                        entity.setFlag(dto.getFlag());
                        entity.setUrlAccessable(dto.getUrlAccessable());
                        models_Table_Repository.save(entity);
                }
        }

        @Transactional
        private void updateModelsTableByField(Models_DTO dto, Integer id, String fieldToUpdate) {
                Optional<Models_Table_Entity> entityOptional = models_Table_Repository.findById(id);
                if (entityOptional.isPresent()) {
                        Models_Table_Entity entity = entityOptional.get();

                        // Check the fieldToUpdate and update accordingly
                        switch (fieldToUpdate) {
                                case "main_model_name":
                                        entity.setMainModelName(dto.getMainModelName());
                                        break;
                                case "name":
                                        entity.setName(dto.getName());
                                        break;
                                case "tags":
                                        entity.setTags(JsonUtils.convertObjectToString(dto.getTags()));
                                        break;
                                case "category":
                                        entity.setCategory(dto.getCategory());
                                        break;
                                case "version_number":
                                        entity.setVersionNumber(dto.getVersionNumber());
                                        break;
                                case "model_number":
                                        entity.setModelNumber(dto.getModelNumber());
                                        break;
                                case "trigger_words":
                                        entity.setTriggerWords(JsonUtils.convertObjectToString(dto.getTriggerWords()));
                                        break;
                                case "nsfw":
                                        entity.setNsfw(dto.getNsfw());
                                        break;
                                case "flag":
                                        entity.setFlag(dto.getFlag());
                                        break;
                                default:
                                        throw new IllegalArgumentException("Unknown field: " + fieldToUpdate);
                        }

                        // Save only if there's a change
                        models_Table_Repository.save(entity);
                }
        }

        @Transactional
        private void updateModelsUrlsTable(Models_DTO dto, Integer id) {
                Optional<Models_Urls_Table_Entity> entityOptional = models_Urls_Table_Repository
                                .findById(id);
                if (entityOptional.isPresent()) {
                        Models_Urls_Table_Entity entity = entityOptional.get();
                        entity.setUrl(dto.getUrl());
                        models_Urls_Table_Repository.save(entity);
                }
        }

        @Transactional
        private void updateModelsDescriptionTable(Models_DTO dto, Integer id) {

                Optional<Models_Descriptions_Table_Entity> entityOptional = models_Descriptions_Table_Repository
                                .findById(id);
                if (entityOptional.isPresent()) {
                        Models_Descriptions_Table_Entity entity = entityOptional
                                        .get();
                        entity.setDescription(dto.getDescription());
                        models_Descriptions_Table_Repository.save(entity);

                }
        }

        @Transactional
        private void updateModelsDetailsTable(Models_DTO dto, Integer id) {

                Optional<Models_Details_Table_Entity> entityOptional = models_Details_Table_Repository
                                .findById(id);
                if (entityOptional.isPresent()) {
                        Models_Details_Table_Entity entity = entityOptional
                                        .get();
                        entity.setType(dto.getType());
                        entity.setStats(dto.getStats());
                        entity.setUploaded(dto.getUploaded());
                        entity.setBaseModel(dto.getBaseModel());
                        entity.setHash(dto.getHash());
                        entity.setUsageTips(dto.getUsageTips());
                        entity.setCreatorName(dto.getCreatorName());
                        models_Details_Table_Repository.save(entity);
                }
        }

        @Transactional
        private void updateModelsImagesTable(Models_DTO dto, Integer id) {
                Optional<Models_Images_Table_Entity> entityOptional = models_Images_Table_Repository
                                .findById(id);
                if (entityOptional.isPresent()) {
                        Models_Images_Table_Entity entity = entityOptional
                                        .get();
                        entity.setImageUrls(JsonUtils.convertObjectToString(dto.getImageUrls()));
                        models_Images_Table_Repository.save(entity);
                }

        }

        @Override
        public Optional<String> findFirstImageUrlByModelNumberAndVersionNumber(String modelNumber,
                        String versionNumber) {
                try {
                        // Step 1: Get the entity for the given modelNumber
                        List<Models_Table_Entity> entityList = models_Table_Repository.findByModelNumber(modelNumber);

                        if (entityList == null || entityList.isEmpty()) {
                                return Optional.empty();
                        }

                        // Step 2: Filter by versionNumber
                        Models_Table_Entity matchingEntity = entityList.stream()
                                        .filter(e -> versionNumber.equals(e.getVersionNumber()))
                                        .findFirst()
                                        .orElse(null);

                        if (matchingEntity == null) {
                                return Optional.empty();
                        }

                        // Step 3: Get the corresponding record in Models_Images_Table_Entity
                        Optional<Models_Images_Table_Entity> imagesEntityOpt = models_Images_Table_Repository
                                        .findById(matchingEntity.getId());
                        if (!imagesEntityOpt.isPresent()) {
                                return Optional.empty();
                        }

                        Models_Images_Table_Entity imagesEntity = imagesEntityOpt.get();

                        // Step 4: Parse the JSON in imageUrls
                        List<Map<String, Object>> imageUrlsList = JsonUtils.convertStringToObject(
                                        imagesEntity.getImageUrls(), List.class);

                        // Step 5: Return the first image's "url"
                        if (imageUrlsList == null || imageUrlsList.isEmpty()) {
                                return Optional.empty();
                        }

                        // The JSON structure was something like:
                        // [
                        // {
                        // "url" : "https://.../image1.jpg",
                        // "nsfw": "Soft",
                        // "width": 512,
                        // "height": 512
                        // },
                        // ...
                        // ]
                        String firstImageUrl = (String) imageUrlsList.get(0).get("url");
                        return Optional.ofNullable(firstImageUrl);

                } catch (DataAccessException e) {
                        log.error("Database error while retrieving first image URL", e);
                        throw new CustomDatabaseException("An unexpected database error occurred", e);
                } catch (Exception e) {
                        log.error("Error while retrieving first image URL", e);
                        throw new CustomException("An unexpected error occurred", e);
                }
        }

        /**
         * Bulk updates the localPath for each record matching the modelID and versionID
         * pairs.
         *
         * @param fileArray a list of maps each containing "modelID" and "versionID"
         * @param localPath the new localPath value to set
         * @return the number of records updated
         */
        @Override
        public int updateLocalPath(List<Map<String, Object>> fileArray, String localPath) {
                List<Object[]> pairs = new ArrayList<>();
                for (Map<String, Object> file : fileArray) {
                        String modelID = (String) file.get("modelID");
                        String versionID = (String) file.get("versionID");
                        pairs.add(new Object[] { modelID, versionID });
                }
                return models_Table_Repository.updateLocalPathByPairsDynamic(pairs, localPath);
        }

        /**
         * Retrieves a list of Models_DTO by searching for records matching a list of
         * composite key pairs.
         *
         * @param compositeList a list of maps, each containing "modelID" and
         *                      "versionID"
         * @return an Optional list of Models_DTO
         */
        @Override
        @Transactional(readOnly = true)
        public Optional<List<Models_DTO>> findListOfModelsDTOByModelAndVersion(
                        List<Map<String, String>> compositeList) {
                if (compositeList == null || compositeList.isEmpty()) {
                        return Optional.empty();
                }
                List<Object[]> pairs = new ArrayList<>();
                for (Map<String, String> map : compositeList) {
                        String modelID = map.get("modelID");
                        String versionID = map.get("versionID");
                        pairs.add(new Object[] { modelID, versionID });
                }
                List<Models_Table_Entity> entityList = models_Table_Repository
                                .findByModelNumberAndVersionNumberIn(pairs);
                if (entityList == null || entityList.isEmpty()) {
                        return Optional.empty();
                }
                // Convert entities to DTOs. You may use your existing convertToDTO method.
                List<Models_DTO> modelsDTOList = entityList.stream()
                                .map(this::convertToDTO)
                                .collect(Collectors.toList());
                return Optional.of(modelsDTOList);
        }

        @Transactional
        @Override
        public void update_record_to_all_tables_by_model_and_version(Models_DTO dto, Integer id,
                        List<String> fieldsToUpdate) {
                try {
                        updateModelsTableByFields(dto, id, fieldsToUpdate);
                        updateModelsUrlsTableByFields(dto, id, fieldsToUpdate);
                        updateModelsDescriptionTableByFields(dto, id, fieldsToUpdate);
                        updateModelsDetailsTableByFields(dto, id, fieldsToUpdate);
                        updateModelsImagesTableByFields(dto, id, fieldsToUpdate);
                } catch (Exception e) {
                        log.error("Error while updating record into all tables by model and version", e);
                        throw new CustomException("An unexpected error occurred", e);
                }
        }

        @Transactional
        private void updateModelsTableByFields(Models_DTO dto, Integer id, List<String> fieldsToUpdate) {
                Optional<Models_Table_Entity> entityOptional = models_Table_Repository.findById(id);
                if (entityOptional.isPresent()) {
                        Models_Table_Entity entity = entityOptional.get();
                        if (fieldsToUpdate.contains("name")) {
                                entity.setName(dto.getName());
                        }
                        if (fieldsToUpdate.contains("mainModelName")) {
                                entity.setMainModelName(dto.getMainModelName());
                        }
                        if (fieldsToUpdate.contains("tags")) {
                                entity.setTags(JsonUtils.convertObjectToString(dto.getTags()));
                        }
                        if (fieldsToUpdate.contains("localTags")) {
                                entity.setLocalTags(JsonUtils.convertObjectToString(dto.getLocalTags()));
                        }
                        if (fieldsToUpdate.contains("aliases")) {
                                entity.setAliases(JsonUtils.convertObjectToString(dto.getAliases()));
                        }
                        if (fieldsToUpdate.contains("versionNumber")) {
                                entity.setVersionNumber(dto.getVersionNumber());
                        }
                        if (fieldsToUpdate.contains("modelNumber")) {
                                entity.setModelNumber(dto.getModelNumber());
                        }
                        if (fieldsToUpdate.contains("triggerWords")) {
                                entity.setTriggerWords(JsonUtils.convertObjectToString(dto.getTriggerWords()));
                        }
                        if (fieldsToUpdate.contains("nsfw")) {
                                entity.setNsfw(dto.getNsfw());
                        }
                        if (fieldsToUpdate.contains("flag")) {
                                entity.setFlag(dto.getFlag());
                        }
                        if (fieldsToUpdate.contains("localPath")) {
                                entity.setLocalPath(dto.getLocalPath());
                        }
                        if (fieldsToUpdate.contains("urlAccessable")) {
                                entity.setUrlAccessable(dto.getUrlAccessable());
                        }
                        models_Table_Repository.save(entity);
                }
        }

        @Transactional
        private void updateModelsUrlsTableByFields(Models_DTO dto, Integer id, List<String> fieldsToUpdate) {
                Optional<Models_Urls_Table_Entity> entityOptional = models_Urls_Table_Repository.findById(id);
                if (entityOptional.isPresent()) {
                        Models_Urls_Table_Entity entity = entityOptional.get();
                        // Update URL if specified (even though URL is ignored at the controller level,
                        // you can update it here if needed)
                        if (fieldsToUpdate.contains("url") && dto.getUrl() != null) {
                                entity.setUrl(dto.getUrl());
                        }
                        models_Urls_Table_Repository.save(entity);
                }
        }

        @Transactional
        private void updateModelsDescriptionTableByFields(Models_DTO dto, Integer id, List<String> fieldsToUpdate) {
                Optional<Models_Descriptions_Table_Entity> entityOptional = models_Descriptions_Table_Repository
                                .findById(id);
                if (entityOptional.isPresent()) {
                        Models_Descriptions_Table_Entity entity = entityOptional.get();
                        if (fieldsToUpdate.contains("description")) {
                                entity.setDescription(dto.getDescription());
                        }
                        models_Descriptions_Table_Repository.save(entity);
                }
        }

        @Transactional
        private void updateModelsDetailsTableByFields(Models_DTO dto, Integer id, List<String> fieldsToUpdate) {
                Optional<Models_Details_Table_Entity> entityOptional = models_Details_Table_Repository.findById(id);
                if (entityOptional.isPresent()) {
                        Models_Details_Table_Entity entity = entityOptional.get();
                        if (fieldsToUpdate.contains("type")) {
                                entity.setType(dto.getType());
                        }
                        if (fieldsToUpdate.contains("stats")) {
                                entity.setStats(dto.getStats());
                        }
                        if (fieldsToUpdate.contains("uploaded")) {
                                entity.setUploaded(dto.getUploaded());
                        }
                        if (fieldsToUpdate.contains("baseModel")) {
                                entity.setBaseModel(dto.getBaseModel());
                        }
                        if (fieldsToUpdate.contains("hash")) {
                                entity.setHash(dto.getHash());
                        }
                        if (fieldsToUpdate.contains("usageTips")) {
                                entity.setUsageTips(dto.getUsageTips());
                        }
                        if (fieldsToUpdate.contains("creatorName")) {
                                entity.setCreatorName(dto.getCreatorName());
                        }
                        models_Details_Table_Repository.save(entity);
                }
        }

        @Transactional
        private void updateModelsImagesTableByFields(Models_DTO dto, Integer id, List<String> fieldsToUpdate) {
                Optional<Models_Images_Table_Entity> entityOptional = models_Images_Table_Repository.findById(id);
                if (entityOptional.isPresent()) {
                        Models_Images_Table_Entity entity = entityOptional.get();
                        if (fieldsToUpdate.contains("imageUrls")) {
                                entity.setImageUrls(JsonUtils.convertObjectToString(dto.getImageUrls()));
                        }
                        models_Images_Table_Repository.save(entity);
                }
        }

        @Override
        public Optional<Models_Table_Entity> find_one_from_models_table_by_model_and_version(String modelNumber,
                        String versionNumber) {
                return models_Table_Repository.findByModelNumberAndVersionNumber(modelNumber, versionNumber);
        }

        @Override
        @Transactional(readOnly = true)
        public Optional<List<Map<String, Object>>> findVirtualFiles(String path) {
                // Remove trailing backslash if present
                String normalizedPath = path.endsWith("\\") ? path.substring(0, path.length() - 1) : path;

                List<Models_Table_Entity> entities = models_Table_Repository
                                .findVirtualFilesByExactPath(normalizedPath);
                if (entities.isEmpty()) {
                        return Optional.empty();
                }

                List<Map<String, Object>> result = new ArrayList<>();
                for (Models_Table_Entity entity : entities) {
                        Map<String, Object> map = new HashMap<>();
                        // Extract drive letter (assumes the first character of local_path is the drive
                        // letter)
                        String drive = (entity.getLocalPath() != null && !entity.getLocalPath().isEmpty())
                                        ? entity.getLocalPath().substring(0, 1)
                                        : "";
                        map.put("drive", drive);
                        map.put("model", convertToDTO(entity));
                        result.add(map);
                }

                return Optional.of(result);
        }

        @Override
        @Transactional(readOnly = true)
        public Optional<List<Map<String, String>>> findVirtualDirectoriesWithDrive(String path) {
                List<Object[]> results = models_Table_Repository.findVirtualDirectoriesWithDrive(path);
                if (results == null || results.isEmpty()) {
                        return Optional.empty();
                }

                List<Map<String, String>> directories = new ArrayList<>();
                for (Object[] row : results) {
                        Map<String, String> map = new HashMap<>();
                        // Expecting row[0] is drive, row[1] is directory
                        map.put("drive", row[0] != null ? row[0].toString() : "");
                        map.put("directory", row[1] != null ? row[1].toString() : "");
                        directories.add(map);
                }

                return Optional.of(directories);
        }

        @Override
        @Transactional()
        public Optional<Models_Table_Entity> find_one_from_models_table_by_model_number_and_version_number(
                        String modelNumber, String versionNumber) {
                try {
                        return models_Table_Repository.findByModelNumberAndVersionNumber(modelNumber, versionNumber);
                } catch (DataAccessException e) {
                        log.error("Database error while finding the record by model+version", e);
                        throw new CustomDatabaseException("An unexpected database error occurred", e);
                } catch (Exception e) {
                        log.error("Unexpected error while finding the record by model+version", e);
                        throw new CustomException("An unexpected error occurred", e);
                }
        }

        /**
         * SQL-backed replacement for update_offline_download_list (no file I/O).
         */
        @Override
        @Transactional(rollbackFor = Exception.class)
        public void update_offline_download_list(
                        String civitaiFileName,
                        List<Map<String, Object>> civitaiModelFileList,
                        String downloadFilePath,
                        Map<String, Object> modelVersionObject,
                        String civitaiModelID,
                        String civitaiVersionID,
                        String civitaiUrl,
                        String civitaiBaseModel,
                        String[] imageUrlsArray,
                        String selectedCategory,
                        List<String> civitaiTags,
                        Boolean isModifyMode) {

                // parse first so we can log them in catch blocks
                Long modelId = parseLongOrNull(civitaiModelID);
                Long versionId = parseLongOrNull(civitaiVersionID);

                // (optional) quick param dump
                System.out.println("=== update_offline_download_list() ===");
                System.out.println("modelId=" + modelId + ", versionId=" + versionId +
                                ", modify=" + isModifyMode + ", fileName=" + civitaiFileName);

                try {
                        log.info("handling update_offline_download_list (SQL)");

                        // find existing by (modelId, versionId)
                        Optional<Models_Offline_Table_Entity> existingOpt = models_Offline_Table_Repository
                                        .findFirstByCivitaiModelIDAndCivitaiVersionID(modelId, versionId);

                        if (existingOpt.isPresent()) {
                                if (Boolean.TRUE.equals(isModifyMode)) {
                                        // update existing (refresh all columns)
                                        Models_Offline_Table_Entity e = existingOpt.get();
                                        e.setCivitaiFileName(civitaiFileName);
                                        e.setCivitaiBaseModel(civitaiBaseModel);
                                        e.setSelectedCategory(selectedCategory);
                                        e.setDownloadFilePath(downloadFilePath);
                                        e.setCivitaiUrl(civitaiUrl);
                                        e.setCivitaiModelID(modelId);
                                        e.setCivitaiVersionID(versionId);
                                        e.setCivitaiModelFileList(toJsonOrNull(civitaiModelFileList));
                                        e.setModelVersionObject(toJsonOrNull(modelVersionObject));
                                        e.setImageUrlsArray(toJsonOrNull(imageUrlsArray));
                                        e.setCivitaiTags(toJsonOrNull(civitaiTags));
                                        models_Offline_Table_Repository.save(e);
                                }
                                // not modify mode -> no-op if exists
                                return;
                        }

                        // create new row
                        Models_Offline_Table_Entity e = Models_Offline_Table_Entity.builder()
                                        .civitaiFileName(civitaiFileName)
                                        .civitaiBaseModel(civitaiBaseModel)
                                        .selectedCategory(selectedCategory)
                                        .downloadFilePath(downloadFilePath)
                                        .civitaiUrl(civitaiUrl)
                                        .civitaiModelID(modelId)
                                        .isError(false)
                                        .civitaiVersionID(versionId)
                                        .civitaiModelFileList(toJsonOrNull(civitaiModelFileList))
                                        .modelVersionObject(toJsonOrNull(modelVersionObject))
                                        .imageUrlsArray(toJsonOrNull(imageUrlsArray))
                                        .civitaiTags(toJsonOrNull(civitaiTags))
                                        .build();
                        models_Offline_Table_Repository.save(e);

                } catch (Exception ex) {
                        log.error("Unexpected error updating offline list (modelId={}, versionId={}): {}",
                                        modelId, versionId, ex.getMessage(), ex);
                        throw new CustomException(
                                        "An unexpected error occurred while updating the offline download list.", ex);
                }
        }

        private Long parseLongOrNull(String s) {
                try {
                        return (s == null || s.isBlank()) ? null : Long.parseLong(s.trim());
                } catch (NumberFormatException ex) {
                        log.warn("Failed to parse Long from '{}'", s);
                        return null;
                }
        }

        private String toJsonOrNull(Object obj) {
                if (obj == null)
                        return null;
                try {
                        return objectMapper.writeValueAsString(obj);
                } catch (JsonProcessingException e) {
                        // choose: either throw your CustomException, or log and return null
                        log.error("JSON serialization failed", e);
                        return null;
                }
        }

        /** pretty JSON for console printing (falls back to String.valueOf on error) */
        private String toPrettyJson(Object obj) {
                if (obj == null)
                        return "null";
                try {
                        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
                } catch (Exception e) {
                        return String.valueOf(obj);
                }
        }

        @Override
        @Transactional(rollbackFor = Exception.class)
        public void remove_from_offline_download_list(String civitaiModelID, String civitaiVersionID) {
                // ---- print ALL params ----
                System.out.println("=== remove_from_offline_download_list() params ===");
                System.out.println("civitaiModelID   : " + civitaiModelID);
                System.out.println("civitaiVersionID : " + civitaiVersionID);
                System.out.println("==================================================");

                Long modelId = parseLongOrNull(civitaiModelID);
                Long versionId = parseLongOrNull(civitaiVersionID);

                if (modelId == null || versionId == null) {
                        System.out.println("Invalid IDs (null/NaN). Nothing removed.");
                        return; // or throw new CustomException("Invalid IDs");
                }

                try {
                        long deleted = models_Offline_Table_Repository
                                        .deleteByCivitaiModelIDAndCivitaiVersionID(modelId, versionId);

                        if (deleted > 0) {
                                System.out.println("Successfully removed " + deleted + " matching row(s).");
                        } else {
                                System.out.println("No matching entry found. No changes made.");
                        }

                } catch (Exception ex) {
                        log.error("Unexpected error removing offline list (modelId={}, versionId={}): {}",
                                        modelId, versionId, ex.getMessage(), ex);
                        throw new CustomException("Unexpected error while removing from the offline download list.",
                                        ex);
                }
        }

        @Override
        @Transactional(readOnly = true, rollbackFor = Exception.class)
        public long checkQuantityOfOfflineDownloadList(String civitaiModelID) {
                // System.out.println("=== checkQuantityOfOfflineDownloadList() ===");
                // System.out.println("civitaiModelID : " + civitaiModelID);
                // System.out.println("============================================");

                Long modelId = parseLongOrNull(civitaiModelID);
                if (modelId == null) {
                        System.err.println("Invalid civitaiModelID (null/NaN). Returning 0.");
                        return 0L;
                }

                try {
                        long count = models_Offline_Table_Repository.countByCivitaiModelID(modelId);
                        // System.out.println("Count for modelId " + modelId + " = " + count);
                        return count;

                        // If you wanted distinct versions instead:
                        // return
                        // models_Offline_Table_Repository.countDistinctVersionByModelId(modelId);

                } catch (Exception ex) {
                        log.error("Unexpected error counting offline rows (modelId={}): {}", modelId, ex.getMessage(),
                                        ex);
                        throw new CustomException("Unexpected error while counting the offline download list.", ex);
                }
        }

        @Override
        @Transactional(readOnly = true, rollbackFor = Exception.class)
        public List<Map<String, Object>> get_offline_download_list() {
                System.out.println("get_offline_download_list(): fetching from DB…");
                try {
                        // 1) fetch all rows
                        List<Models_Offline_Table_Entity> rows = models_Offline_Table_Repository.findAll();
                        System.out.println("DB returned rows: " + rows.size());

                        // 2) map each row to the same Map shape the JSON file used
                        List<Map<String, Object>> mapped = new ArrayList<>(rows.size());
                        for (Models_Offline_Table_Entity e : rows) {
                                Map<String, Object> m = new HashMap<>();
                                m.put("civitaiFileName", e.getCivitaiFileName());
                                m.put("downloadFilePath", e.getDownloadFilePath());
                                m.put("civitaiUrl", e.getCivitaiUrl());
                                m.put("civitaiBaseModel", e.getCivitaiBaseModel());
                                m.put("selectedCategory", e.getSelectedCategory());
                                // keep IDs as String to match old JSON structure
                                m.put("civitaiModelID", e.getCivitaiModelID() == null ? null
                                                : String.valueOf(e.getCivitaiModelID()));
                                m.put("civitaiVersionID", e.getCivitaiVersionID() == null ? null
                                                : String.valueOf(e.getCivitaiVersionID()));

                                // parse JSON columns (DB stores valid JSON; null-safe here)
                                if (e.getCivitaiModelFileList() != null && !e.getCivitaiModelFileList().isBlank()) {
                                        m.put("civitaiModelFileList", objectMapper.readValue(
                                                        e.getCivitaiModelFileList(),
                                                        new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {
                                                        }));
                                } else {
                                        m.put("civitaiModelFileList", null);
                                }

                                if (e.getModelVersionObject() != null && !e.getModelVersionObject().isBlank()) {
                                        m.put("modelVersionObject", objectMapper.readValue(
                                                        e.getModelVersionObject(),
                                                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                                                        }));
                                } else {
                                        m.put("modelVersionObject", null);
                                }

                                if (e.getCivitaiTags() != null && !e.getCivitaiTags().isBlank()) {
                                        m.put("civitaiTags", objectMapper.readValue(
                                                        e.getCivitaiTags(),
                                                        new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
                                                        }));
                                } else {
                                        m.put("civitaiTags", null);
                                }

                                if (e.getImageUrlsArray() != null && !e.getImageUrlsArray().isBlank()) {
                                        List<String> urls = objectMapper.readValue(
                                                        e.getImageUrlsArray(),
                                                        new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
                                                        });
                                        m.put("imageUrlsArray", urls.toArray(new String[0]));
                                } else {
                                        m.put("imageUrlsArray", null);
                                }

                                mapped.add(m);
                        }

                        // 3) apply your same optional filter (remove entries with empty
                        // civitaiBaseModel)
                        List<Map<String, Object>> filtered = mapped.stream()
                                        .filter(entry -> {
                                                Object baseModel = entry.get("civitaiBaseModel");
                                                return baseModel != null && !baseModel.toString().isEmpty();
                                        })
                                        .collect(java.util.stream.Collectors.toList());

                        System.out.println("Returning " + filtered.size() + " entries from DB.");
                        return filtered;

                } catch (org.springframework.dao.DataAccessException | jakarta.persistence.PersistenceException dbEx) {
                        log.error("DB error while retrieving offline download list: {}", dbEx.getMessage(), dbEx);
                        throw new CustomException("Database error while retrieving the offline download list.", dbEx);
                } catch (Exception ex) {
                        log.error("Unexpected error while retrieving offline download list: {}", ex.getMessage(), ex);
                        throw new CustomException(
                                        "An unexpected error occurred while retrieving the offline download list.", ex);
                }
        }

        @Override
        @Transactional(readOnly = true, rollbackFor = Exception.class)
        public List<Map<String, Object>> searchOfflineDownloads(List<String> keywords) {
                System.out.println("=== searchOfflineDownloads() ===");
                try {
                        // normalize keywords
                        List<String> kw = (keywords == null ? Collections.<String>emptyList() : keywords).stream()
                                        .filter(Objects::nonNull)
                                        .map(s -> s.trim().toLowerCase())
                                        .filter(s -> !s.isEmpty())
                                        .collect(Collectors.toList());

                        // 1) fetch all rows
                        List<Models_Offline_Table_Entity> rows = models_Offline_Table_Repository.findAll();

                        // 2) map each row to the same Map shape your JSON version returned
                        List<Map<String, Object>> mapped = new ArrayList<>(rows.size());
                        for (Models_Offline_Table_Entity e : rows) {
                                Map<String, Object> m = new HashMap<>();
                                m.put("civitaiFileName", e.getCivitaiFileName());
                                m.put("downloadFilePath", e.getDownloadFilePath());
                                m.put("civitaiUrl", e.getCivitaiUrl());
                                m.put("civitaiBaseModel", e.getCivitaiBaseModel());
                                m.put("selectedCategory", e.getSelectedCategory());
                                // keep IDs as String like your old file
                                m.put("civitaiModelID", e.getCivitaiModelID() == null ? null
                                                : String.valueOf(e.getCivitaiModelID()));
                                m.put("civitaiVersionID", e.getCivitaiVersionID() == null ? null
                                                : String.valueOf(e.getCivitaiVersionID()));

                                // parse JSON columns inline
                                List<Map<String, Object>> fileList = null;
                                if (e.getCivitaiModelFileList() != null && !e.getCivitaiModelFileList().isBlank()) {
                                        fileList = objectMapper.readValue(e.getCivitaiModelFileList(),
                                                        new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {
                                                        });
                                }
                                m.put("civitaiModelFileList", fileList);

                                Map<String, Object> mvo = null;
                                if (e.getModelVersionObject() != null && !e.getModelVersionObject().isBlank()) {
                                        mvo = objectMapper.readValue(e.getModelVersionObject(),
                                                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                                                        });
                                }
                                m.put("modelVersionObject", mvo);

                                List<String> tags = null;
                                if (e.getCivitaiTags() != null && !e.getCivitaiTags().isBlank()) {
                                        tags = objectMapper.readValue(e.getCivitaiTags(),
                                                        new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
                                                        });
                                }
                                m.put("civitaiTags", tags);

                                List<String> images = null;
                                if (e.getImageUrlsArray() != null && !e.getImageUrlsArray().isBlank()) {
                                        images = objectMapper.readValue(e.getImageUrlsArray(),
                                                        new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
                                                        });
                                }
                                m.put("imageUrlsArray", images == null ? null : images.toArray(new String[0]));

                                mapped.add(m);
                        }

                        // 3) no keywords -> return everything
                        if (kw.isEmpty())
                                return mapped;

                        // 4) filter: AND across keywords, OR across fields
                        List<Map<String, Object>> filtered = new ArrayList<>();
                        outer: for (Map<String, Object> entry : mapped) {
                                for (String k : kw) {
                                        if (k == null || k.isEmpty())
                                                continue;
                                        boolean found = false;

                                        // (A) civitaiFileName
                                        Object fn = entry.get("civitaiFileName");
                                        if (fn != null && fn.toString().toLowerCase().contains(k))
                                                found = true;

                                        // (B) modelVersionObject.name / modelVersionObject.model.name
                                        if (!found) {
                                                @SuppressWarnings("unchecked")
                                                Map<String, Object> mm = (Map<String, Object>) entry
                                                                .get("modelVersionObject");
                                                if (mm != null) {
                                                        Object verName = mm.get("name");
                                                        if (verName != null
                                                                        && verName.toString().toLowerCase().contains(k))
                                                                found = true;
                                                        if (!found) {
                                                                @SuppressWarnings("unchecked")
                                                                Map<String, Object> model = (Map<String, Object>) mm
                                                                                .get("model");
                                                                if (model != null) {
                                                                        Object modelName = model.get("name");
                                                                        if (modelName != null && modelName.toString()
                                                                                        .toLowerCase().contains(k))
                                                                                found = true;
                                                                }
                                                        }
                                                }
                                        }

                                        // (C) civitaiUrl
                                        if (!found) {
                                                Object url = entry.get("civitaiUrl");
                                                if (url != null && url.toString().toLowerCase().contains(k))
                                                        found = true;
                                        }

                                        // (D) civitaiTags
                                        if (!found) {
                                                @SuppressWarnings("unchecked")
                                                List<String> t = (List<String>) entry.get("civitaiTags");
                                                if (t != null) {
                                                        for (String tag : t) {
                                                                if (tag != null && tag.toLowerCase().contains(k)) {
                                                                        found = true;
                                                                        break;
                                                                }
                                                        }
                                                }
                                        }

                                        if (!found)
                                                continue outer; // this keyword not found -> reject entry
                                }
                                filtered.add(entry); // all keywords matched somewhere
                        }

                        return filtered;

                } catch (Exception ex) {
                        log.error("searchOfflineDownloads failed: {}", ex.getMessage(), ex);
                        throw new CustomException("Unexpected error while searching offline downloads.", ex);
                }
        }

        @Override
        @Transactional(readOnly = true, rollbackFor = Exception.class)
        public Optional<List<String>> getCivitaiVersionIds(String civitaiModelID) {
                System.out.println("=== getCivitaiVersionIds() ===");
                System.out.println("civitaiModelID : " + civitaiModelID);
                System.out.println("==============================");

                Long modelId = parseLongOrNull(civitaiModelID);
                if (modelId == null) {
                        System.err.println("Invalid civitaiModelID (null/NaN). Returning empty list.");
                        return Optional.of(Collections.emptyList());
                }

                try {
                        List<Long> ids = models_Offline_Table_Repository.findVersionIdsByModelId(modelId);

                        List<String> asStrings = (ids == null) ? Collections.emptyList()
                                        : ids.stream()
                                                        .filter(Objects::nonNull)
                                                        .map(String::valueOf)
                                                        .collect(Collectors.toList());

                        // If you want DISTINCT behavior instead, just add `.distinct()` in the stream
                        // above
                        // or switch the repository call to the distinct query.

                        return Optional.of(asStrings);

                } catch (Exception ex) {
                        log.error("Unexpected error fetching version IDs (modelId={}): {}", modelId, ex.getMessage(),
                                        ex);
                        throw new CustomException("Unexpected error while retrieving version IDs.", ex);
                }
        }

        @Override
        @Transactional(readOnly = true)
        public List<String> get_error_model_list() {
                List<Models_Offline_Table_Entity> rows = models_Offline_Table_Repository
                                .findAllByIsErrorTrueOrderByIdAsc();

                List<String> out = new ArrayList<>(rows.size());
                for (Models_Offline_Table_Entity e : rows) {
                        String modelId = e.getCivitaiModelID() == null ? "" : String.valueOf(e.getCivitaiModelID());
                        String versionId = e.getCivitaiVersionID() == null ? ""
                                        : String.valueOf(e.getCivitaiVersionID());
                        String fileName = e.getCivitaiFileName() == null ? "" : e.getCivitaiFileName();
                        out.add(modelId + "_" + versionId + "_" + fileName);
                }
                return out;
        }

        @Override
        @Transactional
        public void update_error_model_offline_list(String civitaiModelID, String civitaiVersionID, Boolean isError) {
                System.out.println("=== update_error_model_list ===");
                System.out.println("civitaiModelID   : " + civitaiModelID);
                System.out.println("civitaiVersionID : " + civitaiVersionID);
                System.out.println("isError          : " + isError);
                System.out.println("===============================");

                Long modelId = parseLongOrNull(civitaiModelID);
                Long versionId = parseLongOrNull(civitaiVersionID);
                if (modelId == null || versionId == null) {
                        log.warn("Invalid IDs; nothing updated. modelId={}, versionId={}", modelId, versionId);
                        return;
                }

                boolean flag = Boolean.TRUE.equals(isError); // default null -> false

                int updated = models_Offline_Table_Repository
                                .updateIsErrorByModelAndVersion(modelId, versionId, flag);

                if (updated > 0) {
                        log.info("is_error set to {} for modelId={}, versionId={}", flag, modelId, versionId);
                } else {
                        log.info("No matching row for modelId={}, versionId={} (nothing updated).", modelId, versionId);
                }
        }

        @Override
        @Transactional(readOnly = true)
        public List<Map<String, Object>> get_creator_url_list() {
                try {
                        // CHANGED: do NOT use updatedAt desc
                        List<Creator_Table_Entity> rows = creator_Table_Repository.findAllByOrderByIdAsc();

                        if (rows.isEmpty())
                                return Collections.emptyList();

                        List<Map<String, Object>> out = new ArrayList<>(rows.size());
                        for (Creator_Table_Entity e : rows) {
                                Map<String, Object> m = new LinkedHashMap<>();
                                m.put("creatorUrl", e.getCivitaiUrl());
                                m.put("lastChecked", e.getLastChecked());
                                if (e.getLastCheckedDate() != null) {
                                        m.put("lastCheckedDate", e.getLastCheckedDate()); // ← NEW
                                }
                                m.put("status", e.getStatus());
                                if (e.getRating() != null)
                                        m.put("rating", e.getRating());
                                if (e.getMessage() != null)
                                        m.put("message", e.getMessage());
                                out.add(m);
                        }
                        return out;
                } catch (Exception ex) {
                        log.error("Unexpected error while retrieving creator URL list (DB)", ex);
                        throw new CustomException("An unexpected error occurred", ex);
                }
        }

        @Override
        @Transactional
        public void update_creator_url_list(String creatorUrl, String status, Boolean lastChecked, String rating) {
                if (creatorUrl == null || creatorUrl.isBlank()) {
                        throw new CustomException("creatorUrl is required");
                }

                log.info("[creator-url] INPUT >> url='{}', status='{}', lastChecked={}, rating='{}'",
                                creatorUrl, status, lastChecked, rating);

                // Per-URL lock: serialize read/modify/save for the same URL
                synchronized (creatorUrl.intern()) {
                        try {
                                if (Boolean.TRUE.equals(lastChecked)) {
                                        int cleared = creator_Table_Repository.clearLastCheckedForAll();
                                        log.info("[creator-url] cleared lastChecked=true on {} rows", cleared);
                                }

                                var existing = creator_Table_Repository.findFirstByCivitaiUrl(creatorUrl);
                                Creator_Table_Entity e = existing
                                                .orElseGet(() -> Creator_Table_Entity.builder().civitaiUrl(creatorUrl)
                                                                .build());

                                boolean changed = false;

                                // 1) Status: update only if changed
                                if (status != null) {
                                        String cur = e.getStatus();
                                        if (cur == null || !cur.equalsIgnoreCase(status)) {
                                                log.info("[creator-url] status changed: '{}' -> '{}' (lastChecked={})",
                                                                cur, status, lastChecked);
                                                e.setStatus(status);
                                                e.setLastChecked(lastChecked);
                                                changed = true;
                                        } else {
                                                log.info("[creator-url] status unchanged ('{}')", status);
                                        }
                                }

                                // Always handle lastChecked true (and timestamp)
                                if (Boolean.TRUE.equals(lastChecked)) {
                                        e.setLastChecked(true);
                                        e.setLastCheckedDate(LocalDateTime.now());
                                        changed = true; // ensure we persist the timestamp
                                }

                                if (rating != null) {
                                        String curRating = e.getRating();
                                        if ("N/A".equalsIgnoreCase(rating)) {
                                                if (curRating == null) {
                                                        e.setRating("N/A");
                                                        changed = true;
                                                        log.info("[creator-url] rating set from <null> -> 'N/A'");
                                                } else {
                                                        log.info("[creator-url] incoming 'N/A' -> keep existing rating '{}'",
                                                                        curRating);
                                                }
                                        } else {
                                                if (curRating == null || !curRating.equalsIgnoreCase(rating)) {
                                                        log.info("[creator-url] rating changed: '{}' -> '{}'",
                                                                        curRating, rating);
                                                        e.setRating(rating);
                                                        changed = true;
                                                }
                                        }
                                }

                                if (changed || existing.isEmpty()) {
                                        creator_Table_Repository.save(e);
                                        log.info("[creator-url] SAVED id={} url='{}'", e.getId(), creatorUrl);
                                } else {
                                        log.info("[creator-url] NO CHANGE id={} url='{}' -> skip save", e.getId(),
                                                        creatorUrl);
                                }

                        } catch (Exception ex) {
                                log.error("[creator-url] update failed for {}: {}", creatorUrl, ex.getMessage(), ex);
                                throw new CustomException("Error updating creator URL list", ex);
                        }
                }
        }

        @Override
        @Transactional
        public void remove_creator_url(String creatorUrl) {
                if (creatorUrl == null || creatorUrl.isBlank()) {
                        throw new CustomException("creatorUrl is required");
                }

                try {
                        long deleted = creator_Table_Repository.deleteByCivitaiUrl(creatorUrl);
                        if (deleted > 0) {
                                log.info("[creator-url] deleted {} row(s) for url='{}'", deleted, creatorUrl);
                        } else {
                                log.info("[creator-url] no rows deleted for url='{}'", creatorUrl);
                        }
                } catch (Exception ex) {
                        log.error("[creator-url] delete failed for {}: {}", creatorUrl, ex.getMessage(), ex);
                        throw new CustomException("Error removing creator URL", ex);
                }
        }

        /**
         * Returns a single aggregated object containing rows from all 5 tables,
         * identified by (modelNumber, versionNumber).
         */
        @Override
        @Transactional(readOnly = true)
        public Optional<FullModelRecordDTO> findFullByModelAndVersion(String modelNumber, String versionNumber) {

                Optional<Models_Table_Entity> modelOpt = models_Table_Repository
                                .findByModelNumberAndVersionNumber(modelNumber, versionNumber);

                if (modelOpt.isEmpty()) {
                        return Optional.empty();
                }

                Models_Table_Entity model = modelOpt.get();
                int id = model.getId();

                // Each of these can be absent; fetch defensively.
                Optional<Models_Descriptions_Table_Entity> descriptionOpt = models_Descriptions_Table_Repository
                                .findById(id);

                Optional<Models_Urls_Table_Entity> urlOpt = models_Urls_Table_Repository.findById(id);

                Optional<Models_Details_Table_Entity> detailsOpt = models_Details_Table_Repository.findById(id);

                Optional<Models_Images_Table_Entity> imagesOpt = models_Images_Table_Repository.findById(id);

                FullModelRecordDTO dto = FullModelRecordDTO.builder()
                                .model(model)
                                .description(descriptionOpt.orElse(null))
                                .url(urlOpt.orElse(null))
                                .details(detailsOpt.orElse(null))
                                .images(imagesOpt.orElse(null))
                                .build();

                return Optional.of(dto);
        }

        /**
         * Record a visit:
         * - If row exists for {@code path} -> update last_accessed_at to NOW and
         * increment access_count.
         * - If row does not exist -> insert a new row (timestamps are DB-managed).
         *
         * @param path       canonical full path (normalized)
         * @param parentPath canonical parent (normalized)
         * @param drive      drive letter/root (e.g. "F", "G", "\\\\", "/") or null
         * @param context    fs | virtual
         */
        @Override
        @Transactional
        public void pathVisited(String path,
                        String parentPath,
                        String drive) {

                // 1) Fast-path: update if exists
                if (visitedPath_Table_Repository.existsByPath(path)) {
                        // touch() sets last_accessed_at = CURRENT_TIMESTAMP and access_count =
                        // access_count + 1
                        int updated = visitedPath_Table_Repository.touch(path);

                        // In rare race conditions, existsByPath could be true but the row just
                        // disappeared.
                        // Fallback to an upsert to be safe.
                        if (updated == 0) {
                                visitedPath_Table_Repository.upsertVisit(path, parentPath, drive);
                        }
                        return;
                }

                // 2) Insert new row (first_accessed_at/last_accessed_at are DB-managed)
                VisitedPath_Table_Entity entity = VisitedPath_Table_Entity.builder()
                                .path(path)
                                .parentPath(parentPath)
                                .drive(drive)
                                .accessCount(1)
                                .build();

                visitedPath_Table_Repository.save(entity);
        }

        @Override
        @Transactional(readOnly = true)
        public List<VisitedPath_Table_Entity> getChildren(String parentPath) {
                String normalized = normalizeParent(parentPath);
                return visitedPath_Table_Repository.findByParentPath(normalized);
        }

        private String normalizeParent(String p) {
                if (p == null)
                        return null;
                String s = p.replace('/', '\\').trim();
                if (!s.endsWith("\\") && !s.endsWith("/"))
                        s = s + "\\";
                return s;
        }

        @Override
        @Transactional // write txn (not readOnly)
        public Recycle_Table_Entity add_to_recycle(Recycle_Table_Entity e) {
                if (e.getType() == null) {
                        throw new IllegalArgumentException("type is required (SET or DIRECTORY)");
                }
                if (e.getOriginalPath() == null || e.getOriginalPath().isBlank()) {
                        throw new IllegalArgumentException("originalPath is required");
                }
                if (e.getDeletedDate() == null) {
                        e.setDeletedDate(java.time.LocalDateTime.now());
                }
                if (e.getFiles() == null) {
                        e.setFiles(new java.util.ArrayList<>());
                }
                return recycle_Table_Repository.save(e); // UUID id is generated by JPA
        }

        @Override
        @Transactional // write txn (not readOnly)
        public boolean delete_from_recycle(String id) {
                if (!recycle_Table_Repository.existsById(id))
                        return false;
                recycle_Table_Repository.deleteById(id);
                return true;
        }

        @Override
        @Transactional(readOnly = true)
        public java.util.List<Recycle_Table_Entity> fetch_recycle() {
                return recycle_Table_Repository.findAllByOrderByDeletedDateDesc();
        }

        @Override
        @Transactional(readOnly = true, rollbackFor = Exception.class)
        public Optional<Models_Offline_Table_Entity> getOfflineRecordByModelAndVersion(
                        String civitaiModelID, String civitaiVersionID) {

                System.out.println("=== getOfflineRecordByModelAndVersion() ===");
                System.out.println("civitaiModelID  : " + civitaiModelID);
                System.out.println("civitaiVersionID: " + civitaiVersionID);
                System.out.println("================================================");

                Long modelId = parseLongOrNull(civitaiModelID);
                Long versionId = parseLongOrNull(civitaiVersionID);
                if (modelId == null || versionId == null) {
                        System.err.println("Invalid modelId/versionId (null/NaN). Returning empty.");
                        return Optional.empty();
                }

                try {
                        return models_Offline_Table_Repository
                                        .findFirstByCivitaiModelIDAndCivitaiVersionID(modelId, versionId);
                } catch (Exception ex) {
                        log.error("Unexpected error fetching offline record (modelId={}, versionId={}): {}",
                                        modelId, versionId, ex.getMessage(), ex);
                        throw new CustomException("Unexpected error while retrieving the offline record.", ex);
                }
        }

        @Override
        @Transactional
        public Models_Table_Entity updateMyRatingByModelAndVersion(String modelNumber, String versionNumber,
                        int rating) {
                validateRating(rating);
                Models_Table_Entity entity = models_Table_Repository
                                .findByModelNumberAndVersionNumber(modelNumber, versionNumber)
                                .orElseThrow(() -> new NoSuchElementException(
                                                "Model not found for modelNumber=" + modelNumber + ", versionNumber="
                                                                + versionNumber));

                entity.setMyRating(rating);
                return models_Table_Repository.save(entity);
        }

        private static void validateRating(Integer rating) {
                if (rating == null || rating < 0 || rating > 20) {
                        throw new IllegalArgumentException("myRating must be between 0 and 20");
                }
        }

        // Keep these private helpers inside the service class
        private boolean isBlank(String s) {
                return s == null || s.trim().isEmpty();
        }

        private String requireValidJson(String raw, String fieldName) {
                try {
                        objectMapper.readTree(raw); // validate JSON
                        return raw;
                } catch (Exception e) {
                        throw new IllegalArgumentException(fieldName + " must be valid JSON.");
                }
        }

        /** Remove bidirectional links so Jackson won't recurse. */
        private void trimCycles(FullModelRecordDTO dto) {
                if (dto == null)
                        return;
                if (dto.getModel() != null) {
                        dto.getModel().setModelsUrlsTable(null);
                }
                if (dto.getUrl() != null) {
                        dto.getUrl().setModelsTableEntity(null);
                }
        }

        @Override
        @Transactional
        public FullModelRecordDTO updateFullByModelAndVersion(FullModelRecordDTO incoming) {
                String modelNumber = incoming.getModel().getModelNumber();
                String versionNumber = incoming.getModel().getVersionNumber();

                Models_Table_Entity model = models_Table_Repository
                                .findByModelNumberAndVersionNumber(modelNumber, versionNumber)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Target model not found by (modelNumber, versionNumber)."));

                final int id = model.getId();

                // -------- models_table (PATCH) --------
                Models_Table_Entity patch = incoming.getModel();
                if (patch != null) {
                        if (patch.getName() != null)
                                model.setName(patch.getName());
                        if (patch.getMainModelName() != null)
                                model.setMainModelName(patch.getMainModelName());

                        // JSON columns: skip on blank, validate on non-blank
                        if (patch.getTags() != null && !isBlank(patch.getTags()))
                                model.setTags(requireValidJson(patch.getTags().trim(), "tags"));
                        if (patch.getLocalTags() != null && !isBlank(patch.getLocalTags()))
                                model.setLocalTags(requireValidJson(patch.getLocalTags().trim(), "localTags"));
                        if (patch.getAliases() != null && !isBlank(patch.getAliases()))
                                model.setAliases(requireValidJson(patch.getAliases().trim(), "aliases"));
                        if (patch.getTriggerWords() != null && !isBlank(patch.getTriggerWords()))
                                model.setTriggerWords(requireValidJson(patch.getTriggerWords().trim(), "triggerWords"));

                        if (patch.getLocalPath() != null)
                                model.setLocalPath(patch.getLocalPath());
                        if (patch.getCategory() != null)
                                model.setCategory(patch.getCategory());
                        if (patch.getNsfw() != null)
                                model.setNsfw(patch.getNsfw());
                        if (patch.getUrlAccessable() != null)
                                model.setUrlAccessable(patch.getUrlAccessable());
                        if (patch.getFlag() != null)
                                model.setFlag(patch.getFlag());
                        if (patch.getMyRating() != null)
                                model.setMyRating(patch.getMyRating());
                }
                models_Table_Repository.save(model);

                // -------- models_descriptions_table (UPSERT if provided) --------
                if (incoming.getDescription() != null) {
                        Models_Descriptions_Table_Entity in = incoming.getDescription();
                        Models_Descriptions_Table_Entity row = models_Descriptions_Table_Repository.findById(id)
                                        .orElseGet(() -> {
                                                var x = new Models_Descriptions_Table_Entity();
                                                x.setId(id);
                                                return x;
                                        });
                        if (in.getDescription() != null)
                                row.setDescription(in.getDescription());
                        models_Descriptions_Table_Repository.save(row);
                }

                // -------- models_urls_table (UPSERT if provided) --------
                if (incoming.getUrl() != null) {
                        Models_Urls_Table_Entity in = incoming.getUrl();
                        Models_Urls_Table_Entity row = models_Urls_Table_Repository.findById(id)
                                        .orElseGet(() -> {
                                                var x = new Models_Urls_Table_Entity();
                                                x.setId(id);
                                                x.setModelsTableEntity(model);
                                                return x;
                                        });
                        if (in.getUrl() != null)
                                row.setUrl(in.getUrl());
                        if (row.getModelsTableEntity() == null || row.getModelsTableEntity().getId() != id) {
                                row.setModelsTableEntity(model);
                        }
                        models_Urls_Table_Repository.save(row);
                }

                // -------- models_details_table (UPSERT if provided) --------
                if (incoming.getDetails() != null) {
                        Models_Details_Table_Entity in = incoming.getDetails();
                        Models_Details_Table_Entity row = models_Details_Table_Repository.findById(id)
                                        .orElseGet(() -> {
                                                var x = new Models_Details_Table_Entity();
                                                x.setId(id);
                                                return x;
                                        });
                        if (in.getType() != null)
                                row.setType(in.getType());
                        if (in.getStats() != null)
                                row.setStats(in.getStats());
                        if (in.getUploaded() != null)
                                row.setUploaded(in.getUploaded());
                        if (in.getBaseModel() != null)
                                row.setBaseModel(in.getBaseModel());
                        if (in.getHash() != null)
                                row.setHash(in.getHash());
                        if (in.getUsageTips() != null)
                                row.setUsageTips(in.getUsageTips());
                        if (in.getCreatorName() != null)
                                row.setCreatorName(in.getCreatorName());
                        models_Details_Table_Repository.save(row);
                }

                // -------- models_images_table (UPSERT if provided) --------
                if (incoming.getImages() != null) {
                        Models_Images_Table_Entity in = incoming.getImages();
                        Models_Images_Table_Entity row = models_Images_Table_Repository.findById(id)
                                        .orElseGet(() -> {
                                                var x = new Models_Images_Table_Entity();
                                                x.setId(id);
                                                return x;
                                        });
                        if (in.getImageUrls() != null && !isBlank(in.getImageUrls())) {
                                row.setImageUrls(requireValidJson(in.getImageUrls().trim(), "images.imageUrls"));
                        }
                        models_Images_Table_Repository.save(row);
                }

                // Reload & trim cycles before returning (no entity annotation changes)
                FullModelRecordDTO out = findFullByModelAndVersion(model.getModelNumber(), model.getVersionNumber())
                                .orElseThrow(() -> new IllegalStateException("Updated record could not be reloaded."));
                trimCycles(out);
                return out;
        }

        @Override
        @Transactional(readOnly = true, rollbackFor = Exception.class)
        public PageResponse<Map<String, Object>> get_offline_download_list_paged(
                        int page,
                        int size,
                        boolean filterEmptyBaseModel,
                        List<String> prefixes,
                        String search,
                        String op) { // <-- onlyPending removed

                final int p = Math.max(0, page);
                final int s = Math.min(Math.max(1, size), 500);

                var pageable = org.springframework.data.domain.PageRequest.of(
                                p, s, org.springframework.data.domain.Sort.by("id").descending());

                // short-circuit for “no prefixes selected”
                if (prefixes != null && prefixes.size() == 1 && "__NONE__".equals(prefixes.get(0))) {
                        var out = new PageResponse<Map<String, Object>>();
                        out.content = java.util.List.of();
                        out.page = p;
                        out.size = s;
                        out.totalElements = 0L;
                        out.totalPages = 0;
                        out.hasNext = false;
                        out.hasPrevious = false;
                        return out;
                }

                Specification<Models_Offline_Table_Entity> spec = (root, q, cb) -> {
                        var ands = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

                        // 1) base model present (optional)
                        if (filterEmptyBaseModel) {
                                var base = root.get("civitaiBaseModel").as(String.class);
                                ands.add(cb.and(cb.isNotNull(base), cb.notEqual(base, "")));
                        }

                        // 2) prefixes (case-insensitive; “Updates” is contains)
                        if (prefixes != null && !prefixes.isEmpty()) {
                                var pathExpr = root.get("downloadFilePath").as(String.class);
                                ands.add(cb.isNotNull(pathExpr));
                                var pathLower = cb.lower(pathExpr);

                                var ors = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
                                for (String pref : prefixes) {
                                        if ("/@scan@/Update/".equalsIgnoreCase(pref)) {
                                                ors.add(cb.like(pathLower, "%/@scan@/update/%"));
                                        } else {
                                                ors.add(cb.like(pathLower, pref.toLowerCase() + "%"));
                                        }
                                }
                                if (!ors.isEmpty())
                                        ands.add(cb.or(ors.toArray(jakarta.persistence.criteria.Predicate[]::new)));
                        }

                        // 3) text search with operator
                        if (search != null && !search.isBlank()) {
                                String sTerm = search.trim().toLowerCase();
                                String opNorm = (op == null ? "contains" : op).toLowerCase();

                                java.util.function.Function<jakarta.persistence.criteria.Expression<String>, jakarta.persistence.criteria.Predicate> pos = expr -> {
                                        var v = cb.lower(cb.coalesce(expr, ""));
                                        return switch (opNorm) {
                                                case "equals" -> cb.equal(v, sTerm);
                                                case "begins with" -> cb.like(v, sTerm + "%");
                                                case "ends with" -> cb.like(v, "%" + sTerm);
                                                default -> cb.like(v, "%" + sTerm + "%"); // contains
                                        };
                                };
                                java.util.function.Function<jakarta.persistence.criteria.Expression<String>, jakarta.persistence.criteria.Predicate> neg = expr -> {
                                        var v = cb.lower(cb.coalesce(expr, ""));
                                        return switch (opNorm) {
                                                case "does not equal" -> cb.notEqual(v, sTerm);
                                                case "does not contain" -> cb.notLike(v, "%" + sTerm + "%");
                                                default -> null;
                                        };
                                };

                                var fields = java.util.List.of(
                                                root.get("civitaiFileName").as(String.class),
                                                root.get("civitaiUrl").as(String.class),
                                                root.get("downloadFilePath").as(String.class),
                                                root.get("selectedCategory").as(String.class),
                                                root.get("civitaiModelID").as(String.class),
                                                root.get("civitaiVersionID").as(String.class),
                                                root.get("civitaiTags").as(String.class), // JSON text
                                                root.get("modelVersionObject").as(String.class) // JSON text
                                );

                                boolean isNegative = "does not contain".equals(opNorm)
                                                || "does not equal".equals(opNorm);
                                var preds = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
                                for (var f : fields) {
                                        var pPred = isNegative ? neg.apply(f) : pos.apply(f);
                                        if (pPred != null)
                                                preds.add(pPred);
                                }

                                if (!preds.isEmpty()) {
                                        ands.add(isNegative
                                                        ? cb.and(preds.toArray(
                                                                        jakarta.persistence.criteria.Predicate[]::new))
                                                        : cb.or(preds.toArray(
                                                                        jakarta.persistence.criteria.Predicate[]::new)));
                                }
                        }

                        return ands.isEmpty() ? cb.conjunction()
                                        : cb.and(ands.toArray(jakarta.persistence.criteria.Predicate[]::new));
                };

                var pageResult = models_Offline_Table_Repository.findAll(spec, pageable);

                var mapped = new java.util.ArrayList<java.util.Map<String, Object>>(pageResult.getNumberOfElements());
                for (var e : pageResult.getContent()) {
                        var m = new java.util.HashMap<String, Object>();
                        m.put("civitaiFileName", e.getCivitaiFileName());
                        m.put("downloadFilePath", e.getDownloadFilePath());
                        m.put("civitaiUrl", e.getCivitaiUrl());
                        m.put("civitaiBaseModel", e.getCivitaiBaseModel());
                        m.put("selectedCategory", e.getSelectedCategory());
                        m.put("civitaiModelID",
                                        e.getCivitaiModelID() == null ? null : String.valueOf(e.getCivitaiModelID()));
                        m.put("civitaiVersionID", e.getCivitaiVersionID() == null ? null
                                        : String.valueOf(e.getCivitaiVersionID()));

                        try {
                                if (e.getCivitaiModelFileList() != null && !e.getCivitaiModelFileList().isBlank()) {
                                        m.put("civitaiModelFileList", objectMapper.readValue(
                                                        e.getCivitaiModelFileList(),
                                                        new com.fasterxml.jackson.core.type.TypeReference<java.util.List<java.util.Map<String, Object>>>() {
                                                        }));
                                } else
                                        m.put("civitaiModelFileList", null);

                                if (e.getModelVersionObject() != null && !e.getModelVersionObject().isBlank()) {
                                        m.put("modelVersionObject", objectMapper.readValue(
                                                        e.getModelVersionObject(),
                                                        new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                                                        }));
                                } else
                                        m.put("modelVersionObject", null);

                                if (e.getCivitaiTags() != null && !e.getCivitaiTags().isBlank()) {
                                        m.put("civitaiTags", objectMapper.readValue(
                                                        e.getCivitaiTags(),
                                                        new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {
                                                        }));
                                } else
                                        m.put("civitaiTags", null);

                                if (e.getImageUrlsArray() != null && !e.getImageUrlsArray().isBlank()) {
                                        var urls = objectMapper.readValue(
                                                        e.getImageUrlsArray(),
                                                        new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {
                                                        });
                                        m.put("imageUrlsArray", urls.toArray(new String[0]));
                                } else
                                        m.put("imageUrlsArray", null);
                        } catch (Exception ignore) {
                        }

                        mapped.add(m);
                }

                var out = new PageResponse<java.util.Map<String, Object>>();
                out.content = mapped;
                out.page = p;
                out.size = s;
                out.totalElements = pageResult.getTotalElements();
                out.totalPages = pageResult.getTotalPages();
                out.hasNext = pageResult.hasNext();
                out.hasPrevious = pageResult.hasPrevious();
                return out;
        }

        @Override
        @org.springframework.transaction.annotation.Transactional(readOnly = true, rollbackFor = Exception.class)
        public com.civitai.server.models.dto.PageResponse<com.civitai.server.models.dto.TagCountDTO> get_top_tags_page(
                        com.civitai.server.models.dto.TopTagsRequest req) {

                final int p = Math.max(0, req.getPage());
                final int s = Math.min(Math.max(1, req.getSize()), 500);

                // ---- JPA spec: PENDING rows only (+ optional search/op) ----
                org.springframework.data.jpa.domain.Specification<com.civitai.server.models.entities.civitaiSQL.Models_Offline_Table_Entity> spec = (
                                root, q, cb) -> {
                        java.util.List<jakarta.persistence.criteria.Predicate> ands = new java.util.ArrayList<>();

                        // pending paths (case-insensitive)
                        jakarta.persistence.criteria.Expression<String> pathExpr = cb
                                        .lower(cb.coalesce(root.get("downloadFilePath"), ""));
                        jakarta.persistence.criteria.Predicate p1 = cb.equal(pathExpr, "/@scan@/acg/pending");
                        jakarta.persistence.criteria.Predicate p2 = cb.equal(pathExpr, "/@scan@/acg/pending/");
                        ands.add(cb.or(p1, p2));

                        // optional text search across multiple fields
                        String search = req.getSearch();
                        String op = (req.getOp() == null ? "contains" : req.getOp()).toLowerCase(java.util.Locale.ROOT);
                        if (search != null && !search.isBlank()) {
                                String sTerm = search.trim().toLowerCase(java.util.Locale.ROOT);

                                java.util.List<jakarta.persistence.criteria.Expression<String>> fields = java.util.List
                                                .of(
                                                                root.get("civitaiFileName").as(String.class),
                                                                root.get("civitaiUrl").as(String.class),
                                                                root.get("downloadFilePath").as(String.class),
                                                                root.get("selectedCategory").as(String.class),
                                                                root.get("civitaiModelID").as(String.class),
                                                                root.get("civitaiVersionID").as(String.class),
                                                                root.get("civitaiTags").as(String.class), // JSON text
                                                                root.get("modelVersionObject").as(String.class) // JSON
                                                                                                                // text
                                );

                                java.util.function.Function<jakarta.persistence.criteria.Expression<String>, jakarta.persistence.criteria.Predicate> pos = expr -> {
                                        var v = cb.lower(cb.coalesce(expr, ""));
                                        return switch (op) {
                                                case "equals" -> cb.equal(v, sTerm);
                                                case "begins with" -> cb.like(v, sTerm + "%");
                                                case "ends with" -> cb.like(v, "%" + sTerm);
                                                default -> cb.like(v, "%" + sTerm + "%"); // contains
                                        };
                                };

                                java.util.function.Function<jakarta.persistence.criteria.Expression<String>, jakarta.persistence.criteria.Predicate> neg = expr -> {
                                        var v = cb.lower(cb.coalesce(expr, ""));
                                        return switch (op) {
                                                case "does not equal" -> cb.notEqual(v, sTerm);
                                                case "does not contain" -> cb.notLike(v, "%" + sTerm + "%");
                                                default -> null; // not applicable
                                        };
                                };

                                boolean isNegative = "does not contain".equals(op) || "does not equal".equals(op);
                                java.util.List<jakarta.persistence.criteria.Predicate> preds = new java.util.ArrayList<>();
                                for (var f : fields) {
                                        var pr = isNegative ? neg.apply(f) : pos.apply(f);
                                        if (pr != null)
                                                preds.add(pr);
                                }
                                if (!preds.isEmpty()) {
                                        ands.add(isNegative
                                                        ? cb.and(preds.toArray(
                                                                        jakarta.persistence.criteria.Predicate[]::new))
                                                        : cb.or(preds.toArray(
                                                                        jakarta.persistence.criteria.Predicate[]::new)));
                                }
                        }

                        return ands.isEmpty()
                                        ? cb.conjunction()
                                        : cb.and(ands.toArray(jakarta.persistence.criteria.Predicate[]::new));
                };

                java.util.List<com.civitai.server.models.entities.civitaiSQL.Models_Offline_Table_Entity> rows = models_Offline_Table_Repository
                                .findAll(spec);

                // ---- counting tokens in-memory ----
                final String source = java.util.Optional.ofNullable(req.getSource()).orElse("all")
                                .toLowerCase(java.util.Locale.ROOT);
                final java.util.Set<String> excluded = java.util.Optional.ofNullable(req.getExclude())
                                .orElseGet(java.util.List::of)
                                .stream().filter(java.util.Objects::nonNull)
                                .map(s2 -> s2.toLowerCase(java.util.Locale.ROOT))
                                .collect(java.util.stream.Collectors.toSet());
                final int minLen = Math.max(0, req.getMinLen());
                final boolean allowNumbers = req.isAllowNumbers();
                final java.util.regex.Pattern splitter = java.util.regex.Pattern.compile("[^\\p{L}\\p{N}]+");

                java.util.Map<String, Long> freq = new java.util.HashMap<>(4096);

                for (var e : rows) {
                        java.util.List<String> tokens = new java.util.ArrayList<>(16);

                        // 1) tags (JSON array of strings)
                        if ("all".equals(source) || "tags".equals(source) || "other".equals(source)) {
                                String raw = e.getCivitaiTags();
                                if (raw != null && !raw.isBlank()) {
                                        try {
                                                java.util.List<String> tags = objectMapper.readValue(
                                                                raw,
                                                                new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {
                                                                });
                                                if (tags != null)
                                                        tokens.addAll(tags);
                                        } catch (Exception ignore) {
                                        }
                                }
                        }

                        // 2) fileName
                        if ("all".equals(source) || "filename".equals(source) || "fileName".equals(source)
                                        || "other".equals(source)) {
                                String fn = e.getCivitaiFileName();
                                if (fn != null && !fn.isBlank()) {
                                        for (String t : splitter.split(fn)) {
                                                if (!t.isBlank())
                                                        tokens.add(t);
                                        }
                                }
                        }

                        // 3) titles: model.model.name (+ version name for "all"/"other")
                        if ("all".equals(source) || "titles".equals(source) || "other".equals(source)) {
                                String mvoRaw = e.getModelVersionObject();
                                if (mvoRaw != null && !mvoRaw.isBlank()) {
                                        try {
                                                java.util.Map<String, Object> mvo = objectMapper.readValue(
                                                                mvoRaw,
                                                                new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                                                                });

                                                if ("all".equals(source) || "other".equals(source)) {
                                                        Object verNameObj = mvo.get("name");
                                                        String verName = verNameObj == null ? null
                                                                        : String.valueOf(verNameObj);
                                                        if (verName != null && !verName.isBlank()) {
                                                                for (String t : splitter.split(verName)) {
                                                                        if (!t.isBlank())
                                                                                tokens.add(t);
                                                                }
                                                        }
                                                }

                                                Object modelObj = mvo.get("model");
                                                if (modelObj instanceof java.util.Map<?, ?> mm) {
                                                        Object modelNameObj = ((java.util.Map<?, ?>) mm).get("name");
                                                        String modelName = modelNameObj == null ? null
                                                                        : String.valueOf(modelNameObj);
                                                        if (modelName != null && !modelName.isBlank()) {
                                                                for (String t : splitter.split(modelName)) {
                                                                        if (!t.isBlank())
                                                                                tokens.add(t);
                                                                }
                                                        }
                                                }
                                        } catch (Exception ignore) {
                                        }
                                }
                        }

                        // normalize & count
                        for (String raw : tokens) {
                                String tag = raw.toLowerCase(java.util.Locale.ROOT).trim();
                                if (tag.isEmpty())
                                        continue;
                                if (tag.length() < minLen)
                                        continue;
                                if (!allowNumbers && tag.chars().allMatch(Character::isDigit))
                                        continue;
                                if (excluded.contains(tag))
                                        continue;
                                freq.merge(tag, 1L, Long::sum);
                        }
                }

                // ---- sort by count desc, then tag asc ----
                java.util.List<java.util.Map.Entry<String, Long>> sorted = freq.entrySet()
                                .stream()
                                .sorted((a, b) -> {
                                        int c = java.lang.Long.compare(b.getValue(), a.getValue());
                                        return (c != 0) ? c : a.getKey().compareTo(b.getKey());
                                })
                                .toList();

                // ---- paginate ----
                int from = Math.min(p * s, sorted.size());
                int to = Math.min(from + s, sorted.size());

                java.util.List<com.civitai.server.models.dto.TagCountDTO> pageContent = sorted.subList(from, to)
                                .stream()
                                .map(e -> new com.civitai.server.models.dto.TagCountDTO(e.getKey(), e.getValue()))
                                .toList();

                com.civitai.server.models.dto.PageResponse<com.civitai.server.models.dto.TagCountDTO> out = new com.civitai.server.models.dto.PageResponse<>();
                out.content = pageContent;
                out.page = p;
                out.size = s;
                out.totalElements = (long) sorted.size();
                out.totalPages = (int) Math.ceil(sorted.size() / (double) s);
                out.hasNext = to < sorted.size();
                out.hasPrevious = from > 0;
                return out;
        }

}
