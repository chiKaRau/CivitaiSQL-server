package com.civitai.server.services.impl;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

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
import com.civitai.server.repositories.civitaiSQL.Models_Urls_Table_Repository;
import com.civitai.server.services.CivitaiSQL_Service;
import com.civitai.server.services.Civitai_Service;
import com.civitai.server.utils.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;

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
        private final Civitai_Service civitai_Service;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        private static final Logger log = LoggerFactory.getLogger(CivitaiSQL_Service_Impl.class);

        // CRUD, CREATE, READ, UPDATE, DELETE
        public CivitaiSQL_Service_Impl(Models_Table_Repository models_Table_Repository,
                        Models_Descriptions_Table_Repository models_Descriptions_Table_Repository,
                        Models_Urls_Table_Repository models_Urls_Table_Repository,
                        Models_Details_Table_Repository models_Details_Table_Repository,
                        Models_Images_Table_Repository models_Images_Table_Repository,
                        Civitai_Service civitai_Service) {
                this.models_Table_Repository = models_Table_Repository;
                this.models_Descriptions_Table_Repository = models_Descriptions_Table_Repository;
                this.models_Urls_Table_Repository = models_Urls_Table_Repository;
                this.models_Details_Table_Repository = models_Details_Table_Repository;
                this.models_Images_Table_Repository = models_Images_Table_Repository;
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

                //return Optional.ofNullable(categories); above if code is equipviemnt with this
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
        @Override
        public Optional<Map<String, List<Models_DTO>>> find_lastest_three_models_DTO_in_each_category_from_all_table() {
                try {
        
                        List<String> categories = models_Table_Repository.findAllCategories();
        
                        Map<String, List<Models_DTO>> latestModelsDTOList = new HashMap<>();
        
                        PageRequest pageable = PageRequest.of(0, 3);
        
                        //Check if the category list is exist
                        if (categories != null) {
                                //Map through each category
                                for (String category : categories) {
                                        //Define an empty list for every loop
                                        List<Models_DTO> modelsDTOList = new ArrayList<>();
        
                                        //Retrieve a list of id which contains the latest records for each category
                                        List<Integer> idList = models_Table_Repository
                                                        .findLastThreeAddedRecordsID(category, pageable);
        
                                        //Map through each id and retrive its Models_DTO
                                        for (Integer id : idList) {
                                                Optional<Models_DTO> entityOptional = find_one_models_DTO_from_all_tables(
                                                                id);
                                                if (entityOptional.isPresent()) {
                                                        Models_DTO entity = entityOptional.get();
                                                        //Add the Models_DTO into the list
                                                        modelsDTOList.add(entity);
                                                }
                                        }
        
                                        //add the Models_DTO list into the hashObject
                                        latestModelsDTOList.put(category, modelsDTOList);
        
                                }
                        }
        
                        return latestModelsDTOList.isEmpty() ? Optional.empty() : Optional.of(latestModelsDTOList);
        
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

        //Transcation Actions
        @Override
        @Transactional
        public void create_record_to_all_tables(Models_DTO dto) {
                try {
                        // Step 1: Save the Models_Table_Entities entity to generate the ID
                        Models_Table_Entity models_Table_Entities = Models_Table_Entity.builder()
                                        .name(dto.getName())
                                        .tags(JsonUtils.convertObjectToString(dto.getTags()))
                                        .category(dto.getCategory())
                                        .versionNumber(dto.getVersionNumber())
                                        .modelNumber(dto.getModelNumber())
                                        .triggerWords(JsonUtils.convertObjectToString(dto.getTriggerWords()))
                                        .nsfw(dto.getNsfw())
                                        .flag(dto.getFlag())
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

        //Utils
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
                models_DTO.setTags(JsonUtils.convertStringToObject(
                                tables_DTO.getModels_Table_Entitiy().getTags(), List.class));
                models_DTO.setCategory(tables_DTO.getModels_Table_Entitiy().getCategory());
                models_DTO.setVersionNumber(tables_DTO.getModels_Table_Entitiy().getVersionNumber());
                models_DTO.setModelNumber(tables_DTO.getModels_Table_Entitiy().getModelNumber());
                models_DTO.setTriggerWords(JsonUtils.convertStringToObject(
                                tables_DTO.getModels_Table_Entitiy().getTriggerWords(), List.class));
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
        public Optional<Models_DTO> create_models_DTO_by_Url(String url, String category) {
                try {

                        String modelID = url.replaceAll(".*/models/(\\d+).*", "$1");

                        Optional<Map<String, Object>> modelOptional = civitai_Service.findModelByModelID(modelID);

                        String name = null, versionNumber = null, description = null, type = null, stats = null,
                                        hash = null, usageTips = null, creatorName = null,
                                        baseModel = null;

                        List<Map<String, Object>> images = new ArrayList<>();
                        List<String> tags = new ArrayList<>(), triggerWords = new ArrayList<>();

                        Boolean nsfw = false, flag = false;

                        LocalDate uploaded = null;

                        if (modelOptional.isPresent()) {
                                Map<String, Object> model = modelOptional.get();

                                //Retriving the version list 
                                Optional<List<Map<String, Object>>> modelVersionList = Optional
                                                .ofNullable(model)
                                                .map(map -> (List<Map<String, Object>>) map.get("modelVersions"))
                                                .filter(list -> !list.isEmpty());

                                //For Version Number
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

                                //Retriving appropriate model from the version list
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

                                //For NSFW
                                nsfw = Optional.ofNullable((Boolean) model.get("nsfw"))
                                                .orElse(null);

                                //For Tags
                                tags = Optional.ofNullable((List<String>) model.get("tags"))
                                                .orElse(null);

                                //For Trigger Words
                                triggerWords = matchingVersionModel.stream()
                                                .map(map -> (List<String>) map.get("trainedWords"))
                                                .findFirst().orElse(null);

                                //For Description
                                description = Optional.ofNullable((String) model.get("description"))
                                                .orElse(null);

                                //For Type
                                type = Optional.ofNullable((String) model.get("type"))
                                                .orElse(null);

                                //For Stats
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

                                //For Uploaded Date

                                String uploadedString = matchingVersionModel.stream()
                                                .map(map -> (String) map.get("createdAt"))
                                                .findFirst().orElse(null);

                                uploaded = LocalDate.parse(
                                                Instant.parse(uploadedString).atOffset(ZoneOffset.UTC)
                                                                .toLocalDateTime()
                                                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                                                DateTimeFormatter.ISO_LOCAL_DATE);

                                //For Model Name
                                name = matchingVersionModel.stream()
                                                .map(map -> (String) map.get("name"))
                                                .findFirst().orElse(null);

                                //For Base Model
                                baseModel = matchingVersionModel.stream()
                                                .map(map -> (String) map.get("baseModel"))
                                                .findFirst().orElse(null);

                                //For Hash
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

                                //For Creator Name
                                creatorName = Optional.ofNullable((Map<String, Object>) model.get("creator"))
                                                .map(creator -> (String) creator.get("username"))
                                                .orElse(null);

                                //For Images Urls

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

                                //Create a Models_DTO
                                Models_DTO dto = new Models_DTO();
                                dto.setUrl(url);
                                dto.setName(name);
                                dto.setModelNumber(modelID);
                                dto.setVersionNumber(versionNumber);
                                dto.setCategory(category);
                                dto.setNsfw(nsfw);
                                dto.setFlag(flag);
                                dto.setTags(tags);
                                dto.setTriggerWords(triggerWords);
                                dto.setDescription(description);
                                dto.setType(type);
                                dto.setStats(stats);
                                dto.setUploaded(uploaded);
                                dto.setBaseModel(baseModel);
                                dto.setHash(hash);
                                dto.setUsageTips(usageTips);
                                dto.setCreatorName(creatorName);
                                dto.setImageUrls(images);

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

        private void updateModelsTable(Models_DTO dto, Integer id) {
                Optional<Models_Table_Entity> entityOptional = models_Table_Repository
                                .findById(id);
                if (entityOptional.isPresent()) {
                        Models_Table_Entity entity = entityOptional.get();
                        entity.setName(dto.getName());
                        entity.setTags(JsonUtils.convertObjectToString(dto.getTags()));
                        entity.setCategory(dto.getCategory());
                        entity.setVersionNumber(dto.getVersionNumber());
                        entity.setModelNumber(dto.getModelNumber());
                        entity.setTriggerWords(JsonUtils.convertObjectToString(dto.getTriggerWords()));
                        entity.setNsfw(dto.getNsfw());
                        entity.setFlag(dto.getFlag());
                        models_Table_Repository.save(entity);
                }
        }

        private void updateModelsUrlsTable(Models_DTO dto, Integer id) {
                Optional<Models_Urls_Table_Entity> entityOptional = models_Urls_Table_Repository
                                .findById(id);
                if (entityOptional.isPresent()) {
                        Models_Urls_Table_Entity entity = entityOptional.get();
                        entity.setUrl(dto.getUrl());
                        models_Urls_Table_Repository.save(entity);
                }
        }

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

}
