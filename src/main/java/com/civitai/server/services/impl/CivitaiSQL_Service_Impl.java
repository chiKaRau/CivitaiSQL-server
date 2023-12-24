package com.civitai.server.services.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import com.civitai.server.utils.JsonUtils;

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

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        private static final Logger log = LoggerFactory.getLogger(CivitaiSQL_Service_Impl.class);

        // CRUD, CREATE, READ, UPDATE, DELETE
        public CivitaiSQL_Service_Impl(Models_Table_Repository models_Table_Repository,
                        Models_Descriptions_Table_Repository models_Descriptions_Table_Repository,
                        Models_Urls_Table_Repository models_Urls_Table_Repository,
                        Models_Details_Table_Repository models_Details_Table_Repository,
                        Models_Images_Table_Repository models_Images_Table_Repository) {
                this.models_Table_Repository = models_Table_Repository;
                this.models_Descriptions_Table_Repository = models_Descriptions_Table_Repository;
                this.models_Urls_Table_Repository = models_Urls_Table_Repository;
                this.models_Details_Table_Repository = models_Details_Table_Repository;
                this.models_Images_Table_Repository = models_Images_Table_Repository;
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
                                                                                .map(id -> find_one_models_DTO_from_all_tables(
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
                        Optional<Models_Table_Entity> modelsOptional = models_Table_Repository.findById(id);

                        return modelsOptional.isPresent() ? modelsOptional : Optional.empty();

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
                        Iterable<Models_Table_Entity> models = models_Table_Repository.findAll();

                        if (models != null) {
                                List<Tables_DTO> entities = StreamSupport.stream(models.spliterator(), false)
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

                                return Optional.of(entities);
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
                        Optional<Models_Table_Entity> modelsOptional = models_Table_Repository.findById(id);
                        if (modelsOptional.isPresent()) {
                                Models_Table_Entity model = modelsOptional.get();

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

        @SuppressWarnings("unchecked")
        @Override
        public Optional<Models_DTO> find_one_models_DTO_from_all_tables(Integer id) {
                try {
                        Optional<Models_Table_Entity> modelsOptional = models_Table_Repository.findById(id);
                        if (modelsOptional.isPresent()) {
                                Models_Table_Entity model = modelsOptional.get();

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

                                Models_DTO models_DTO = new Models_DTO();
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

        @SuppressWarnings("unchecked")
        @Override
        public Optional<Models_DTO> find_one_models_DTO_from_all_tables_by_url(String url) {

                Models_Urls_Table_Entity model = models_Urls_Table_Repository.findByUrl(url);
                if (model != null) {
                        Tables_DTO tables_DTO = new Tables_DTO();
                        tables_DTO.setModels_Table_Entitiy(
                                        models_Table_Repository.findById(model.getId()).orElse(null));
                        tables_DTO.setModels_Descriptions_Table_Entity(
                                        models_Descriptions_Table_Repository
                                                        .findById(model.getId())
                                                        .orElse(null));
                        tables_DTO.setModels_Urls_Table_Entity(model);
                        tables_DTO.setModels_Details_Table_Entity(
                                        models_Details_Table_Repository.findById(model.getId())
                                                        .orElse(null));
                        tables_DTO.setModels_Images_Table_Entity(
                                        models_Images_Table_Repository.findById(model.getId())
                                                        .orElse(null));
                        Models_DTO models_DTO = new Models_DTO();
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

                        return Optional.of(models_DTO);
                } else {
                        return Optional.empty();

                }

        }

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

}
