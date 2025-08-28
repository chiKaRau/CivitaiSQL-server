package com.civitai.server.services.impl;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.civitai.server.exception.CustomDatabaseException;
import com.civitai.server.exception.CustomException;
import com.civitai.server.models.dto.Models_DTO;
import com.civitai.server.models.dto.Tables_DTO;
import com.civitai.server.models.entities.civitaiSQL.Models_Descriptions_Table_Entity;
import com.civitai.server.models.entities.civitaiSQL.Models_Details_Table_Entity;
import com.civitai.server.models.entities.civitaiSQL.Models_Images_Table_Entity;
import com.civitai.server.models.entities.civitaiSQL.Models_Table_Entity;
import com.civitai.server.models.entities.civitaiSQL.Models_Urls_Table_Entity;
import com.civitai.server.repositories.civitaiSQL.Models_Descriptions_Table_Repository;
import com.civitai.server.repositories.civitaiSQL.Models_Details_Table_Repository;
import com.civitai.server.repositories.civitaiSQL.Models_Images_Table_Repository;
import com.civitai.server.repositories.civitaiSQL.Models_Table_Repository;
import com.civitai.server.repositories.civitaiSQL.Models_Table_Repository_Specification;
import com.civitai.server.repositories.civitaiSQL.Models_Urls_Table_Repository;
import com.civitai.server.services.CivitaiSQL_Service;
import com.civitai.server.services.Civitai_Service;
import com.civitai.server.specification.civitaiSQL.Models_Table_Specification;
import com.civitai.server.utils.CustomResponse;
import com.civitai.server.utils.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
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
                        Models_Table_Repository_Specification models_Table_Repository_Specification,
                        Civitai_Service civitai_Service) {
                this.models_Table_Repository = models_Table_Repository;
                this.models_Descriptions_Table_Repository = models_Descriptions_Table_Repository;
                this.models_Urls_Table_Repository = models_Urls_Table_Repository;
                this.models_Details_Table_Repository = models_Details_Table_Repository;
                this.models_Images_Table_Repository = models_Images_Table_Repository;
                this.models_Table_Repository_Specification = models_Table_Repository_Specification;
                this.civitai_Service = civitai_Service;
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

        @Override
        @SuppressWarnings("null")
        public Optional<List<Models_DTO>> find_List_of_models_DTO_from_all_tables_by_alike_tagsList(
                        List<String> tagsList) {

                Specification<Models_Table_Entity> spec = Models_Table_Specification.findByTagsList(tagsList);

                List<Models_Table_Entity> entityList = models_Table_Repository_Specification.findAll(spec);

                if (entityList != null && !entityList.isEmpty()) {
                        List<Models_DTO> modelsDTOList = entityList.stream()
                                        .map(this::convertToDTO)
                                        .collect(Collectors.toList());

                        return Optional.of(modelsDTOList);
                } else {
                        return Optional.empty();
                }
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

}
