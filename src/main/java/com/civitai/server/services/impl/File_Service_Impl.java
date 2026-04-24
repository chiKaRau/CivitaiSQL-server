package com.civitai.server.services.impl;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.civitai.server.exception.CustomException;
import com.civitai.server.services.File_Service;
import com.civitai.server.utils.ConfigUtils;
import com.civitai.server.utils.FileUtils;
import com.civitai.server.utils.ProgressBarUtils;
import com.fasterxml.jackson.core.type.TypeReference;

import jakarta.annotation.PostConstruct;

import com.civitai.server.utils.FileUtils;

@Service
public class File_Service_Impl implements File_Service {

    private static final Logger log = LoggerFactory.getLogger(CivitaiSQL_Service_Impl.class);

    @PostConstruct
    public void createFileAtStartup() {

        // Create a offline_download_list if have none
        // create_offline_download_list();

        // Create a folder_list if have none
        create_folder_list();

        // Create a cart_list if have none
        create_cart_list();

        // Create a must_add_list if have none
        create_must_add_list();

        // Create a tags list if have none
        // create_tags_list();

        // Create a error_model_list if have none
        // create_error_model_list();

        create_pending_remove_tags_list();

        // create_creator_url_list();
    }

    public void create_offline_download_list() {
        String offlineDownloadFile = "files/data/offline_download_list.json";
        try {
            Path filePath = Paths.get(offlineDownloadFile);

            // Ensure parent directories exist
            if (filePath.getParent() != null && !Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }

            if (!Files.exists(filePath)) {
                // Initialize the list to hold maps
                List<Map<String, Object>> offlineDownloadList = new ArrayList<>();

                // // (Optional) Add sample data to the list
                // // Comment out the following block if you want to start with an empty list
                // Map<String, Object> sampleItem = new HashMap<>();
                // sampleItem.put("civitaiFileName", "sample_file_name");
                // sampleItem.put("civitaiModelFileList", new ArrayList<Map<String, Object>>());
                // // Empty list or populate as needed
                // sampleItem.put("downloadFilePath", "/path/to/download");
                // sampleItem.put("modelVersionObject", new HashMap<String, Object>()); // Empty
                // map or populate as needed
                // sampleItem.put("civitaiModelID", "model_12345");
                // sampleItem.put("civitaiVersionID", "version_1.0");
                // sampleItem.put("civitaiUrl", "https://civitai.com/model/12345");
                // sampleItem.put("civitaiBaseModel", "base_model_xyz");
                // sampleItem.put("imageUrlsArray", new String[] {
                // "https://civitai.com/images/img1.png",
                // "https://civitai.com/images/img2.png"
                // });

                // offlineDownloadList.add(sampleItem);

                // Convert the list to a JSON string with pretty printing
                ObjectMapper objectMapper = new ObjectMapper();
                String offlineDownloadListJson = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(offlineDownloadList);

                // Write the JSON string to the file
                Files.write(filePath, offlineDownloadListJson.getBytes(), StandardOpenOption.CREATE_NEW);

                System.out.println("offline_download_list.json has been created with sample data.");
            } else {
                System.out.println(
                        "offline_download_list.json already exists: " + offlineDownloadFile + ". Doing nothing.");
            }
        } catch (IOException e) {
            // Log and handle exceptions appropriately
            log.error("Unexpected error while creating offline_download_list", e);
            throw new CustomException("An unexpected error occurred", e);
        }
    }

    @SuppressWarnings("unchecked")
    public long checkQuantityOfOfflineDownloadList(String civitaiModelID) {
        String offlineDownloadFile = "files/data/offline_download_list.json";
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            Path filePath = Paths.get(offlineDownloadFile);

            // Check if parent directories exist
            if (filePath.getParent() != null && !Files.exists(filePath.getParent())) {
                System.err.println("Parent directories do not exist for the file: " + offlineDownloadFile);
                return 0;
            }

            if (!Files.exists(filePath)) {
                // File does not exist
                System.err.println("The file does not exist: " + offlineDownloadFile);
                return 0;
            }

            // Read all bytes from the file
            byte[] fileBytes = Files.readAllBytes(filePath);
            String data = new String(fileBytes, StandardCharsets.UTF_8).trim();

            if (data.isEmpty()) {
                // File is empty
                System.err.println("The file is empty: " + offlineDownloadFile);
                return 0;
            }

            // Deserialize JSON array into List<Map<String, Object>>
            List<Map<String, Object>> offlineDownloadList = objectMapper.readValue(
                    data, new TypeReference<List<Map<String, Object>>>() {
                    });

            // Use Streams to count the occurrences of the civitaiModelID
            long count = offlineDownloadList.stream()
                    .filter(item -> civitaiModelID.equals(item.get("civitaiModelID")))
                    .count();

            return (int) count;

        } catch (IOException e) {
            // Log the exception to standard error
            System.err.println(
                    "Unexpected error while counting civitaiModelID in offline_download_list: " + e.getMessage());
            // Optionally, rethrow the exception as a runtime exception
            throw new RuntimeException("An unexpected error occurred while counting the offline download list.", e);
        }
    }

    private static final Object JSON_WRITE_LOCK = new Object();

    // Counter for update operations (either new entry or modified entry)
    private static int updateCreatorUrlUpdateCount = 0;

    /**
     * Backs up the creator_url_list.json file by creating a copy with an
     * incremental name.
     *
     * @return true if the backup was successful, false otherwise.
     */
    public boolean backupCreatorUrlList() {
        String originalFilePath = "files/data/creator_url_list.json";
        Path originalPath = Paths.get(originalFilePath);

        synchronized (JSON_WRITE_LOCK) { // Ensure thread safety
            try {
                if (!Files.exists(originalPath)) {
                    System.out.println("Original file does not exist. Backup not created.");
                    return false;
                }

                // Define the backup directory path; using "creatorbackup" for this file
                Path backupDir = originalPath.getParent().resolve("creatorbackup");

                // Create the backup directory if it doesn't exist
                if (!Files.exists(backupDir)) {
                    Files.createDirectories(backupDir);
                    System.out.println("Backup directory created at: " + backupDir.toString());
                }

                // Determine the backup file name
                String baseName = "creator_url_list";
                String extension = ".json";
                int copyIndex = 1;
                Path backupPath;

                do {
                    String backupFileName = String.format("%s copy %d%s", baseName, copyIndex, extension);
                    backupPath = backupDir.resolve(backupFileName);
                    copyIndex++;
                } while (Files.exists(backupPath));

                // Copy the file to the backup directory
                Files.copy(originalPath, backupPath, StandardCopyOption.COPY_ATTRIBUTES);

                System.out.println("Backup created at: " + backupPath.toString());
                return true;

            } catch (IOException e) {
                System.out.println("Error while backing up the creator_url_list file: " + e.getMessage());
                return false;
            }
        }
    }

    // Method to update the offline_download_list.json
    @SuppressWarnings("unchecked")
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

        String offlineDownloadFile = "files/data/offline_download_list.json";
        ObjectMapper objectMapper = new ObjectMapper();

        System.out.println("handling update_offline_download_list");

        synchronized (JSON_WRITE_LOCK) { // <--- Use a lock if multi-threaded
            try {
                Path filePath = Paths.get(offlineDownloadFile);

                // Ensure parent directories exist
                if (filePath.getParent() != null && !Files.exists(filePath.getParent())) {
                    Files.createDirectories(filePath.getParent());
                }

                // Read existing JSON data (if file exists)
                List<Map<String, Object>> offlineDownloadList;
                if (Files.exists(filePath)) {
                    String data = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
                    if (data.trim().isEmpty()) {
                        offlineDownloadList = new ArrayList<>();
                    } else {
                        offlineDownloadList = objectMapper.readValue(
                                data, new TypeReference<List<Map<String, Object>>>() {
                                });
                    }
                } else {
                    offlineDownloadList = new ArrayList<>();
                }

                // Find if there's an existing entry
                int existingIndex = -1;
                for (int i = 0; i < offlineDownloadList.size(); i++) {
                    Map<String, Object> item = offlineDownloadList.get(i);
                    if (Objects.equals(item.get("civitaiModelID"), civitaiModelID) &&
                            Objects.equals(item.get("civitaiVersionID"), civitaiVersionID)) {
                        existingIndex = i;
                        break;
                    }
                }

                if (existingIndex != -1) {
                    // Existing entry found
                    if (Boolean.TRUE.equals(isModifyMode)) {
                        // Update the existing entry
                        Map<String, Object> existingEntry = offlineDownloadList.get(existingIndex);
                        existingEntry.put("downloadFilePath", downloadFilePath);
                        existingEntry.put("selectedCategory", selectedCategory);
                        // ... other updates as needed
                    } else {
                        // Not in modify mode, so do nothing if entry exists
                        return;
                    }
                } else {
                    // No existing entry
                    if (Boolean.TRUE.equals(isModifyMode)) {
                        // If in modify mode but no entry found, decide your logic; here we add a new
                        // entry
                        System.out.println("Modify mode but entry not found. Adding a new entry...");
                    }

                    // Create and add new entry
                    Map<String, Object> newEntry = new HashMap<>();
                    newEntry.put("civitaiFileName", civitaiFileName);
                    newEntry.put("civitaiModelFileList", civitaiModelFileList);
                    newEntry.put("downloadFilePath", downloadFilePath);
                    newEntry.put("modelVersionObject", modelVersionObject);
                    newEntry.put("civitaiModelID", civitaiModelID);
                    newEntry.put("civitaiVersionID", civitaiVersionID);
                    newEntry.put("civitaiUrl", civitaiUrl);
                    newEntry.put("civitaiBaseModel", civitaiBaseModel);
                    newEntry.put("imageUrlsArray", imageUrlsArray);
                    newEntry.put("selectedCategory", selectedCategory);
                    newEntry.put("civitaiTags", civitaiTags);

                    offlineDownloadList.add(newEntry);
                }

                // Serialize the updated list back to JSON
                String updatedJson = objectMapper
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(offlineDownloadList);

                // --- Perform an atomic write here ---
                writeJsonAtomically(filePath, updatedJson);

            } catch (IOException e) {
                // Handle exceptions appropriately
                log.error("Unexpected error while updating offline_download_list", e);
                throw new CustomException(
                        "An unexpected error occurred while updating the offline download list.", e);
            }
        }
    }

    private void writeJsonAtomically(Path targetFilePath, String jsonContent) throws IOException {
        // Create a temp file in the same directory
        Path tempFilePath = Files.createTempFile(targetFilePath.getParent(), "offline_download_list", ".tmp");

        // Write the new JSON content to the temp file
        Files.write(tempFilePath, jsonContent.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.TRUNCATE_EXISTING);

        // Atomically move/replace the old file with the temp file
        Files.move(tempFilePath, targetFilePath,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
    }

    public void create_cart_list() {
        String cartListFile = "files/data/cart_list.json";
        try {
            if (!Files.exists(Paths.get(cartListFile))) {
                // Create an empty list
                List<String> cartList = new ArrayList<>();

                // Convert the empty list to a JSON string
                String cartListJson = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(cartList);

                // Write the JSON string to the file
                Files.write(Path.of(cartListFile), cartListJson.getBytes(), StandardOpenOption.CREATE);

                System.out.println("cart_list.json has been created as an empty file.");
            } else {
                System.out.println("cart_list.json is already exists: " + cartListFile + ". Doing nothing.");
            }
        } catch (IOException e) {
            // Log and handle other types of exceptions
            log.error("Unexpected error while creating cart list", e);
            throw new CustomException("An unexpected error occurred", e);
        }
    }

    public void create_error_model_list() {
        String errorModelListFile = "files/data/error_model_list.json";
        try {
            if (!Files.exists(Paths.get(errorModelListFile))) {
                // Create an empty list
                List<String> errorModelList = new ArrayList<>();

                // Convert the empty list to a JSON string
                String errorModelListJson = new ObjectMapper().writerWithDefaultPrettyPrinter()
                        .writeValueAsString(errorModelList);

                // Write the JSON string to the file
                Files.write(Path.of(errorModelListFile), errorModelListJson.getBytes(), StandardOpenOption.CREATE);

                System.out.println("error_model_list.json has been created as an empty file.");
            } else {
                System.out
                        .println("error_model_list.json is already exists: " + errorModelListFile + ". Doing nothing.");
            }
        } catch (IOException e) {
            // Log and handle other types of exceptions
            log.error("Unexpected error while creating error_model_list", e);
            throw new CustomException("An unexpected error occurred", e);
        }
    }

    public void create_must_add_list() {
        String mustAddListFile = "files/data/must_add_list.json";
        try {
            if (!Files.exists(Paths.get(mustAddListFile))) {
                // Create an empty list
                List<String> mustAddList = new ArrayList<>();

                // Convert the empty list to a JSON string
                String mustAddListJson = new ObjectMapper().writerWithDefaultPrettyPrinter()
                        .writeValueAsString(mustAddList);

                // Write the JSON string to the file
                Files.write(Path.of(mustAddListFile), mustAddListJson.getBytes(), StandardOpenOption.CREATE);

                System.out.println("must_add_list.json has been created as an empty file.");
            } else {
                System.out.println("must_add_list.json is already exists: " + mustAddListFile + ". Doing nothing.");
            }
        } catch (IOException e) {
            // Log and handle other types of exceptions
            log.error("Unexpected error while creating mmust_add_list", e);
            throw new CustomException("An unexpected error occurred", e);
        }
    }

    public void create_tags_list() {
        String tagsListFile = "files/data/tags_list.json";
        try {
            if (!Files.exists(Paths.get(tagsListFile))) {
                // Create an empty list of tag records
                List<Map<String, Object>> tagsList = new ArrayList<>();

                // Convert the empty list to a JSON string
                String tagsListJson = new ObjectMapper().writerWithDefaultPrettyPrinter()
                        .writeValueAsString(tagsList);

                // Write the JSON string to the file
                Files.write(Path.of(tagsListFile), tagsListJson.getBytes(), StandardOpenOption.CREATE);

                System.out.println("tags_list.json has been created as an empty file.");
            } else {
                System.out.println("tags_list.json already exists: " + tagsListFile + ". Doing nothing.");
            }
        } catch (IOException e) {
            log.error("Unexpected error while creating tags_list", e);
            throw new CustomException("An unexpected error occurred", e);
        }
    }

    public void create_pending_remove_tags_list() {
        String pendingRemoveTagsList = "files/data/pending_remove_tags_list.json";
        try {
            if (!Files.exists(Paths.get(pendingRemoveTagsList))) {
                // Create an empty list of tag records
                List<Map<String, Object>> tagsList = new ArrayList<>();

                // Convert the empty list to a JSON string
                String tagsListJson = new ObjectMapper().writerWithDefaultPrettyPrinter()
                        .writeValueAsString(tagsList);

                // Write the JSON string to the file
                Files.write(Path.of(pendingRemoveTagsList), tagsListJson.getBytes(), StandardOpenOption.CREATE);

                System.out.println("pending_remove_tags_list.json has been created as an empty file.");
            } else {
                System.out.println(
                        "pending_remove_tags_list.json already exists: " + pendingRemoveTagsList + ". Doing nothing.");
            }
        } catch (IOException e) {
            log.error("Unexpected error while creating pending_remove_tags_list", e);
            throw new CustomException("An unexpected error occurred", e);
        }
    }

    public void create_creator_url_list() {
        String creatorUrlList = "files/data/creator_url_list.json";
        try {
            if (!Files.exists(Paths.get(creatorUrlList))) {
                // Create an empty list of tag records
                List<Map<String, Object>> tagsList = new ArrayList<>();

                // Convert the empty list to a JSON string
                String tagsListJson = new ObjectMapper().writerWithDefaultPrettyPrinter()
                        .writeValueAsString(tagsList);

                // Write the JSON string to the file
                Files.write(Path.of(creatorUrlList), tagsListJson.getBytes(), StandardOpenOption.CREATE);

                System.out.println("creator_url_list.json has been created as an empty file.");
            } else {
                System.out.println(
                        "creator_url_list.json already exists: " + creatorUrlList + ". Doing nothing.");
            }
        } catch (IOException e) {
            log.error("Unexpected error while creating creator_url_list", e);
            throw new CustomException("An unexpected error occurred", e);
        }
    }

    public void create_folder_list() {
        try {
            String inputFile = "files/data/folder_list.txt";
            String outputFile = "files/data/folder_list.json";

            // Check if input file exists, and create an empty file if it doesn't
            if (!Files.exists(Paths.get(inputFile))) {
                Files.createFile(Paths.get(inputFile));
                System.out.println("Empty input file created: " + inputFile);
            } else {
                System.out.println("folder_list.txt is already exists: " + inputFile + ". Doing nothing.");
            }

            if (!Files.exists(Paths.get(outputFile))) {
                List<String> lines = Files.readAllLines(Paths.get(inputFile), StandardCharsets.UTF_8);

                // Use a Set to eliminate duplicates
                Set<String> uniquePaths = new HashSet<>();

                // Remove the first two lines and add to the Set
                lines.stream().skip(1).forEach(line -> uniquePaths.add(line));

                // Add './@scan@/Temp' to the Set
                uniquePaths.add("./@scan@/Temp");

                List<String> options = uniquePaths.stream()
                        .map(line -> "/" + line.substring(2) + "/") // Wrap the line with "/"
                        .collect(Collectors.toList());

                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

                // Convert the list to a JSON array and write to the output file
                objectMapper.writeValue(Paths.get(outputFile).toFile(), options);

                System.out.println("Data successfully converted and written to: " + outputFile);
            } else {
                System.out.println("folder_list.json is already exists: " + outputFile + ". Doing nothing.");
            }

        } catch (IOException e) {
            // Log and handle other types of exceptions
            log.error("Unexpected error while creating folder list", e);
            throw new CustomException("An unexpected error occurred", e);
        }
    }

    @Override
    public List<String> get_folders_list() {

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<String> dataList = objectMapper.readValue(
                    Files.readAllBytes(Paths.get("files/data/folder_list.json")),
                    new TypeReference<List<String>>() {
                    });

            // Check if dataList is null or empty
            if (dataList == null || dataList.isEmpty()) {
                return Collections.emptyList(); // Return an empty list
            }

            dataList = dataList.stream()
                    .filter(s -> !s.contains("/@scan@/Update/"))
                    .collect(Collectors.toList());

            return dataList;
        } catch (IOException e) {
            // Log and handle other types of exceptions
            log.error("Unexpected error while retreving folder list", e);
            throw new CustomException("An unexpected error occurred", e);
        }
    }

    @Override
    public List<String> get_pending_remove_tags_list() {

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<String> pendingRemoveTagsList = objectMapper.readValue(
                    Files.readAllBytes(Paths.get("files/data/pending_remove_tags_list.json")),
                    new TypeReference<List<String>>() {
                    });

            // Check if dataList is null or empty
            if (pendingRemoveTagsList == null || pendingRemoveTagsList.isEmpty()) {
                return Collections.emptyList(); // Return an empty list
            }

            return pendingRemoveTagsList;
        } catch (IOException e) {
            // Log and handle other types of exceptions
            log.error("Unexpected error while retreving pending remove tags list", e);
            throw new CustomException("An unexpected error occurred", e);
        }
    }

    @Override
    public void add_pending_remove_tag(String pendingRemoveTag) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Path filePath = Paths.get("files/data/pending_remove_tags_list.json");

            List<String> pendingRemoveTagsList;

            // Check if the file exists; if not, create a new ArrayList
            if (Files.exists(filePath) && Files.size(filePath) > 0) {
                pendingRemoveTagsList = objectMapper.readValue(
                        Files.readAllBytes(filePath),
                        new TypeReference<List<String>>() {
                        });
            } else {
                pendingRemoveTagsList = new ArrayList<>();
            }

            // Add the new tag
            pendingRemoveTagsList.add(pendingRemoveTag);

            // Write the updated list back to the JSON file
            objectMapper.writeValue(filePath.toFile(), pendingRemoveTagsList);

        } catch (IOException e) {
            log.error("Unexpected error while adding pending remove tag", e);
            throw new CustomException("An unexpected error occurred", e);
        }
    }

    @Override
    public List<Map<String, String>> get_categories_prefix_list() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            // Read the JSON file and parse it as a Map
            Map<String, List<Map<String, String>>> jsonData = objectMapper.readValue(
                    Files.readAllBytes(Paths.get("files/data/categories_prefix_list.json")),
                    new TypeReference<Map<String, List<Map<String, String>>>>() {
                    });

            // Retrieve the "prefixsList" key from the JSON map
            List<Map<String, String>> dataList = jsonData.getOrDefault("categoriesPrefixsList",
                    Collections.emptyList());

            // Return the full list of objects
            return dataList;
        } catch (IOException e) {
            // Log and handle exceptions
            log.error("Unexpected error while retrieving categories_prefix_list list", e);
            throw new CustomException("An unexpected error occurred", e);
        }
    }

    @Override
    public List<Map<String, String>> get_filePath_categories_list() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            // Read the JSON file and parse it as a Map
            Map<String, List<Map<String, String>>> jsonData = objectMapper.readValue(
                    Files.readAllBytes(Paths.get("files/data/filepath_categories_list.json")),
                    new TypeReference<Map<String, List<Map<String, String>>>>() {
                    });

            // Retrieve the "prefixsList" key from the JSON map
            List<Map<String, String>> dataList = jsonData.getOrDefault("filePathCategoriesList",
                    Collections.emptyList());

            // Return the full list of objects
            return dataList;
        } catch (IOException e) {
            // Log and handle exceptions
            log.error("Unexpected error while retrieving filepath_categories_list list", e);
            throw new CustomException("An unexpected error occurred", e);
        }
    }

    @Override
    public Map<String, List<Map<String, Object>>> get_tags_list(String prefix) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // Read and parse the tags_list.json as a List of Maps
            List<Map<String, Object>> dataList = objectMapper.readValue(
                    Files.readAllBytes(Paths.get("files/data/tags_list.json")),
                    new TypeReference<List<Map<String, Object>>>() {
                    });

            // Check if dataList is null or empty
            if (dataList == null || dataList.isEmpty()) {
                return Map.of("topTags", Collections.emptyList(), "recentTags", Collections.emptyList());
            }

            // Filter out any entries where string_value contains "/@scan@/Update/"
            List<Map<String, Object>> filteredList = dataList.stream()
                    .filter(map -> {
                        String stringValue = (String) map.get("string_value");
                        return stringValue != null && !stringValue.contains("/@scan@/Update/");
                    })
                    .collect(Collectors.toList());

            // If a prefix is provided, filter the list by the prefix
            if (prefix != null && !prefix.isEmpty()) {
                filteredList = filteredList.stream()
                        .filter(map -> {
                            String stringValue = (String) map.get("string_value");
                            return stringValue != null && stringValue.startsWith(prefix);
                        })
                        .collect(Collectors.toList());
            }

            // Define the current date and the date one month ago
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime oneMonthAgo = now.minusMonths(2);

            // Filter the tags that were added in the last month
            List<Map<String, Object>> recentMonthTags = filteredList.stream()
                    .filter(map -> {
                        String lastAddedStr = (String) map.get("last_added");
                        if (lastAddedStr == null || lastAddedStr.isEmpty()) {
                            return false;
                        }
                        LocalDateTime lastAddedDate;
                        try {
                            lastAddedDate = LocalDateTime.parse(lastAddedStr);
                        } catch (DateTimeParseException e) {
                            // If the date format is incorrect, exclude this tag
                            return false;
                        }
                        return lastAddedDate.isAfter(oneMonthAgo);
                    })
                    .collect(Collectors.toList());

            // Sort by count (descending) and get the top 10 for tags in the last month
            List<Map<String, Object>> topTags = recentMonthTags.stream()
                    .sorted((map1, map2) -> {
                        Integer count1 = (Integer) map1.get("count");
                        Integer count2 = (Integer) map2.get("count");
                        return count2.compareTo(count1);
                    })
                    .limit(10)
                    .collect(Collectors.toList());

            // Sort by last_added (descending) and get the most recent 10
            List<Map<String, Object>> recentTags = filteredList.stream()
                    .sorted((map1, map2) -> {
                        String dateStr1 = (String) map1.get("last_added");
                        String dateStr2 = (String) map2.get("last_added");
                        try {
                            LocalDateTime date1 = LocalDateTime.parse(dateStr1);
                            LocalDateTime date2 = LocalDateTime.parse(dateStr2);
                            return date2.compareTo(date1);
                        } catch (DateTimeParseException e) {
                            // Handle parsing error by considering unparsable dates as older
                            return 0;
                        }
                    })
                    .limit(10)
                    .collect(Collectors.toList());

            // System.out.println("topTags");

            // System.out.println(topTags);

            // System.out.println("recentTags");

            // System.out.println(recentTags);

            // Return both lists in a map
            return Map.of("topTags", topTags, "recentTags", recentTags);

        } catch (IOException e) {
            // Log and handle other types of exceptions
            log.error("Unexpected error while retrieving tags list", e);
            throw new CustomException("An unexpected error occurred", e);
        }
    }

    @Override
    public void empty_cart_list() {
        try {
            String cartListFile = "files/data/cart_list.json";

            // Create an empty list
            List<String> emptyCartList = Collections.emptyList();

            // Convert the empty list to a JSON string
            String emptyCartListJson = new ObjectMapper().writeValueAsString(emptyCartList);

            // Write the empty JSON string to the file
            Files.writeString(Path.of(cartListFile), emptyCartListJson, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            System.out.println("cart_list.json has been emptied.");

        } catch (IOException e) {
            // Log and handle other types of exceptions
            log.error("Unexpected error while empty the cart list", e);
            throw new CustomException("An unexpected error occurred", e);
        }
    }

    @Override
    public Boolean check_cart_list(String url) {
        try {
            String cartListFile = "files/data/cart_list.json";

            // Read the content of cart_list.json
            String data = new String(Files.readAllBytes(Path.of(cartListFile)));

            if (data.isEmpty()) {
                // Handle empty file or invalid JSON
                throw new CustomException("Empty file or invalid JSON");
            }

            // Parse the JSON data into an array
            String[] cartList = new ObjectMapper().readValue(data, String[].class);

            if (cartList == null) {
                // Handle null result after deserialization
                throw new CustomException("Failed to deserialize JSON array");
            }

            // Check if the URL is already in the array
            for (String cartedUrl : cartList) {
                if (cartedUrl != null && cartedUrl.equals(url)) {
                    return true;
                }
            }

            return false;

        } catch (IOException e) {
            // Log and handle other types of exceptions
            log.error("Unexpected error while checking cart list", e);
            throw new CustomException("An unexpected error occurred", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void update_folder_list(String downloadFilePath) {
        try {

            Path filePath = Path.of("files/data/folder_list.json");

            // Read the content of folder_list.json
            String data = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);

            // Take out "/" if the file path last character is "/"
            if (downloadFilePath.endsWith("/")) {
                downloadFilePath = downloadFilePath.substring(0, downloadFilePath.length() - 1);
            }

            // Parse the JSON data into a List
            ObjectMapper objectMapper = new ObjectMapper();
            List<String> jsonData = objectMapper.readValue(data, List.class);

            if (jsonData == null) {
                System.err.println("The JSON data is not a list.");
                return;
            }

            if (jsonData.contains(downloadFilePath + "/")) {
                // Do nothing if download path already existed.
                System.out.println(downloadFilePath + "/ already exists in the array.");
            } else {

                // prevent "/update/"
                if (!downloadFilePath.contains("/update/")) {
                    // Otherwise, Update folder_list.json
                    jsonData.add(downloadFilePath + "/");
                    Files.write(filePath, objectMapper.writeValueAsString(jsonData).getBytes(StandardCharsets.UTF_8),
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                    // Update folder_list.txt
                    Files.write(Path.of("files/data/folder_list.txt"), ("." + downloadFilePath + "\n").getBytes(),
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    System.out.println(downloadFilePath + " has added to the array and files updated.");
                }
            }

        } catch (IOException e) {
            // Log and handle other types of exceptions
            log.error("Unexpected error while updating folder list", e);
            throw new CustomException("An unexpected error occurred", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void update_cart_list(String url) {
        try {

            Path filePath = Path.of("files/data/cart_list.json");

            // Read the content of folder_list.json
            String data = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);

            // Parse the JSON data into a list
            ObjectMapper objectMapper = new ObjectMapper();
            List<String> cartList = objectMapper.readValue(data, List.class);

            // Check if the URL is already in the list
            if (!cartList.contains(url)) {
                // Add the new URL to the list
                cartList.add(url);

                // Convert the updated list back to a JSON string
                String updatedCartListJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(cartList);

                // Write the updated JSON string back to the file
                Files.write(filePath, updatedCartListJson.getBytes(StandardCharsets.UTF_8));
                System.out.println("Added \"" + url + "\" to cart_list.json.");
            } else {
                // Do nothing if download path already existed.
                System.out.println("\"" + url + "\" is already in cart_list.json. No duplicates allowed.");
            }

        } catch (IOException e) {
            // Log and handle other types of exceptions
            log.error("Unexpected error while updating cart list", e);
            throw new CustomException("An unexpected error occurred", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void update_must_add_list(String url) {
        try {

            Path filePath = Path.of("files/data/must_add_list.json");

            // Read the content of folder_list.json
            String data = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);

            // Parse the JSON data into a list
            ObjectMapper objectMapper = new ObjectMapper();
            List<String> mustAddList = objectMapper.readValue(data, List.class);

            // Check if the URL is already in the list
            if (!mustAddList.contains(url)) {
                // Add the new URL to the list
                mustAddList.add(url);

                // Convert the updated list back to a JSON string
                String updatedMustAddListJson = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(mustAddList);

                // Write the updated JSON string back to the file
                Files.write(filePath, updatedMustAddListJson.getBytes(StandardCharsets.UTF_8));
                System.out.println("Added \"" + url + "\" to must_add_list.json.");
            } else {
                // Do nothing if download path already existed.
                System.out.println("\"" + url + "\" is already in must_add_list.json. No duplicates allowed.");
            }

        } catch (IOException e) {
            // Log and handle other types of exceptions
            log.error("Unexpected error while updating cart list", e);
            throw new CustomException("An unexpected error occurred", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void update_tags_list(String inputTag) {
        try {
            // Ignore input tags that start with "/@scan@/Update/"
            if (inputTag.startsWith("/@scan@/Update/")) {
                System.out.println("Ignoring tag: \"" + inputTag + "\" as it starts with \"/@scan@/Update/\".");
                return;
            }

            Path filePath = Path.of("files/data/tags_list.json");

            // Read the content of the JSON file
            String data = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);

            // Parse the JSON data into a list of maps
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, Object>> tagsList = objectMapper.readValue(data, List.class);

            // Check if the input tag already exists
            boolean found = false;
            for (Map<String, Object> entry : tagsList) {
                if (entry.get("string_value").equals(inputTag)) {
                    // Increment the count and update the last_added timestamp
                    int count = (int) entry.get("count");
                    entry.put("count", count + 1);
                    entry.put("last_added", LocalDateTime.now().toString());
                    found = true;
                    break;
                }
            }

            if (!found) {
                // If the tag doesn't exist, add a new entry
                Map<String, Object> newEntry = new HashMap<>();
                newEntry.put("string_value", inputTag);
                newEntry.put("count", 1);
                newEntry.put("last_added", LocalDateTime.now().toString());
                tagsList.add(newEntry);
            }

            // Convert the updated list back to a JSON string
            String updatedTagsListJson = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(tagsList);

            // Write the updated JSON string back to the file
            Files.write(filePath, updatedTagsListJson.getBytes(StandardCharsets.UTF_8));
            System.out.println("Updated tags_list.json with \"" + inputTag + "\".");

        } catch (IOException e) {
            log.error("Unexpected error while updating tags list", e);
            throw new CustomException("An unexpected error occurred", e);
        }
    }

    @Override
    public void open_download_directory() {
        try {
            String downloadFolderPath = "files/download"; // Adjust the path accordingly

            File file = new File(downloadFolderPath);

            if (!file.exists()) {
                System.out.println("Directory does not exist");
                return;
            }

            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                // For Windows
                ProcessBuilder processBuilder = new ProcessBuilder("explorer.exe", file.getAbsolutePath());
                processBuilder.start();
            } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
                // For Linux or macOS
                ProcessBuilder processBuilder = new ProcessBuilder("xdg-open", file.getAbsolutePath());
                processBuilder.start();
            } else {
                System.out.println("Unsupported operating system: " + os);
            }

        } catch (Exception e) {
            // Log and handle other types of exceptions
            log.error("Unexpected error while opening download directory", e);
            throw new CustomException("An unexpected error occurred", e);
        }
    }

    @Override
    public void open_model_downloaded_directory(String modelDownloadPath) {
        try {
            String downloadFolderPath = "files/download" + "/" + modelDownloadPath; // Adjust the path accordingly

            File file = new File(downloadFolderPath);

            if (!file.exists()) {
                System.out.println("Directory does not exist");
                return;
            }

            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                // For Windows
                ProcessBuilder processBuilder = new ProcessBuilder("explorer.exe", file.getAbsolutePath());
                processBuilder.start();
            } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
                // For Linux or macOS
                ProcessBuilder processBuilder = new ProcessBuilder("xdg-open", file.getAbsolutePath());
                processBuilder.start();
            } else {
                System.out.println("Unsupported operating system: " + os);
            }

        } catch (Exception e) {
            // Log and handle other types of exceptions
            log.error("Unexpected error while opening download directory", e);
            throw new CustomException("An unexpected error occurred", e);
        }
    }

    @Override
    public void download_file_by_server(String name, String modelID, String versionID, String downloadFilePath,
            List<Map<String, Object>> filesList, String url) {

        String modelName = modelID + "_" + versionID + "_" + name.split("\\.")[0];
        Path modelDirectory = Paths.get("files/download/", modelName);

        // Load the configuration file
        ConfigUtils.loadConfig("civitaiConfig.json");

        // Get a specific configuration value
        String civitaiApiKey = ConfigUtils.getConfigValue("apiKey");

        try {
            String downloadPath = "/" + modelID + "_" + versionID + "_" + name.split("\\.")[0]
                    + downloadFilePath;

            // Check if the downloadPath inside the folder_list.json,
            // if not, append it
            update_folder_list(downloadFilePath);

            // Create the 'download' directory within the 'files' directory
            Path downloadDirectory = Paths.get("files", "download");
            Files.createDirectories(downloadDirectory);

            // Create the directories based on the user input path
            Path currentPath = downloadDirectory;
            for (String dir : downloadPath.split("/")) {
                if (!dir.isEmpty()) {
                    currentPath = currentPath.resolve(dir);
                    Files.createDirectories(currentPath);
                }
            }

            // Download files
            for (Map<String, Object> data : filesList) {
                String fileName = modelID + "_" + versionID + "_" + data.get("name");

                String prepareUrl = (String) data.get("downloadUrl");

                System.out.println(prepareUrl);

                // Check if the URL does NOT contain either 'type' or 'format'
                if (!prepareUrl.contains("type") && !prepareUrl.contains("format")) {
                    prepareUrl = prepareUrl + "?token=" + civitaiApiKey;
                }

                if (prepareUrl.contains("Training")) {
                    continue;
                }

                if (prepareUrl.contains("VAE")) {
                    continue;
                }

                URL downloadUrl = new URL(prepareUrl);

                Path filePath = currentPath.resolve(fileName);

                URLConnection connection = downloadUrl.openConnection();
                long totalSize = connection.getContentLengthLong();

                try (InputStream inputStream = downloadUrl.openStream();
                        FileOutputStream fileOutputStream = new FileOutputStream(filePath.toFile())) {

                    // Download the file
                    /*
                     * byte[] buffer = new byte[1024];
                     * int bytesRead;
                     * while ((bytesRead = inputStream.read(buffer)) != -1) {
                     * fileOutputStream.write(buffer, 0, bytesRead);
                     * }
                     */

                    // Use a custom method to copy the InputStream to the file with a progress
                    // indicator
                    ProgressBarUtils.copyInputStreamWithProgress(inputStream, filePath, totalSize, fileName);

                }
            }

            // Log download completion
            System.out.println("Download completed for: " + name);

            // Create ZIP archive
            Path zipFilePath = Paths.get("files/download/", modelID + "_" + versionID + "_" + name);
            Path zipFile = Paths.get("files/download/", modelID + "_" + versionID + "_" + name + ".zip");

            try (
                    ZipOutputStream zos = new ZipOutputStream(
                            new FileOutputStream(zipFile.toFile()))) {

                long totalSize = ProgressBarUtils.calculateTotalSize(zipFilePath);

                // Convert totalSize to kilobytes
                // long totalSizeKB = totalSize / 1024;

                // System.out.println(totalSizeKB);

                // if (totalSizeKB < 15) {
                // throw new Exception("File size is less than 15kb, may need browser
                // download.");
                // }

                Files.walkFileTree(zipFilePath, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file,
                            BasicFileAttributes attributes) {

                        // only copy files, no symbolic links
                        if (attributes.isSymbolicLink()) {
                            return FileVisitResult.CONTINUE;
                        }

                        try (FileInputStream fis = new FileInputStream(file.toFile())) {
                            Path targetFile = zipFilePath.relativize(file);
                            zos.putNextEntry(new ZipEntry(targetFile.toString()));

                            byte[] buffer = new byte[1024];
                            int len;
                            long bytesRead = 0;

                            while ((len = fis.read(buffer)) > 0) {
                                zos.write(buffer, 0, len);
                                bytesRead += len;

                                // Update the progress bar in the console
                                ProgressBarUtils.updateProgressBar(bytesRead, totalSize,
                                        modelID + "_" + versionID + "_" + name + ".zip");

                            }

                            // if large file, throws out of memory
                            // byte[] bytes = Files.readAllBytes(file);
                            // zos.write(bytes, 0, bytes.length);

                            zos.closeEntry();

                            // System.out.printf("Zip file : %s%n", file);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        System.err.printf("Unable to zip : %s%n%s%n", file, exc);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }

            // Delete the folder and its contents
            Files.walkFileTree(zipFilePath, new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });

            long zipFileSize = Files.size(zipFile);
            if ((zipFileSize / 1024) < 15) {
                Files.delete(zipFile);
                throw new Exception(zipFile + " size is less than 15kb, may need browser download.");
            }

            // Log zip completion
            System.out.println("\nZip process completed for: " + "\u001B[1m" + name + ".zip" + "\u001B[0m"
                    + " and saved into " + downloadFilePath);

        } catch (Exception e) {

            // Log and handle other types of exceptions
            System.out.println("Error Model Name: " + modelName);

            System.out.println(e);

            // update_error_model_list(modelName);

            FileUtils.deleteDirectory(modelDirectory);

            // log.error("Unexpected error while downloading file", e);
            throw new CustomException("An unexpected error occurred", e);

        }
    }

    @Override
    public void download_file_by_server_v2(String civitaiFileName,
            List<Map<String, Object>> civitaiModelFileList,
            String downloadFilePath,
            Map<String, Object> modelVersionObject,
            String civitaiModelID,
            String civitaiVersionID,
            String civitaiUrl,
            String civitaiBaseModel,
            String[] imageUrlsArray) {

        String modelID = civitaiModelID.trim();
        String versionID = civitaiVersionID.trim();
        String url = civitaiUrl;
        String name = civitaiFileName.split("\\.")[0].trim();
        civitaiBaseModel = civitaiBaseModel.trim();

        String modelName = modelID + "_" + versionID + "_" + civitaiBaseModel + "_" + name;

        Path baseDownloadDirectory = Paths.get("files", "download");

        // Load config once
        ConfigUtils.loadConfig("civitaiConfig.json");
        String civitaiApiKey = ConfigUtils.getConfigValue("apiKey");

        Path finalPreviewImagePath = null;
        Path uniqueDirectory = null;

        // NEW: Track whether we actually downloaded anything
        boolean anyFileDownloaded = false;

        try {
            // Normalize downloadFilePath
            String normalizedDownloadFilePath = downloadFilePath.trim()
                    .replaceAll("^/+", "")
                    .replaceAll("/+$", "");

            update_folder_list(normalizedDownloadFilePath);

            Files.createDirectories(baseDownloadDirectory);
            System.out.println("Ensured download directory exists: " + baseDownloadDirectory);

            // Create a unique directory for each request
            String uniqueID = UUID.randomUUID().toString();
            uniqueDirectory = baseDownloadDirectory.resolve(
                    Paths.get(normalizedDownloadFilePath, uniqueID));
            Files.createDirectories(uniqueDirectory);
            System.out.println("Created unique download directory: " + uniqueDirectory);

            // Download each file
            for (Map<String, Object> data : civitaiModelFileList) {
                String dataName = ((String) data.get("name")).trim();
                String fileName = modelID + "_" + versionID + "_" + civitaiBaseModel + "_" + dataName;
                String prepareUrl = (String) data.get("downloadUrl");

                // Append token if needed
                if (!prepareUrl.contains("type") && !prepareUrl.contains("format")) {
                    prepareUrl += "?token=" + civitaiApiKey;
                }

                // Skip if it's a training file or VAE
                if (prepareUrl.contains("Training") || prepareUrl.contains("VAE")) {
                    System.out.println("Skipping file due to Training/VAE in URL: " + prepareUrl);
                    continue;
                }

                Path filePath = uniqueDirectory.resolve(fileName);
                int maxRetries = 4;

                System.out.println("Starting download: " + prepareUrl + " -> " + filePath);
                boolean ok = httpDownloadWithResume(prepareUrl, filePath, fileName, maxRetries);

                if (ok) {
                    System.out.println("\nDownloaded file: " + filePath);
                    anyFileDownloaded = true;
                } else {
                    System.err.println("Failed to download after retries: " + prepareUrl);
                    continue;
                }

                // Create .civitai.info
                String fName = civitaiFileName.replace(".safetensors", "").trim();
                String civitaiInfoFileName = modelID + "_" + versionID + "_" + civitaiBaseModel + "_" + fName
                        + ".civitai.info";
                Path civitaiInfoFilePath = uniqueDirectory.resolve(civitaiInfoFileName);

                String modelVersionJson = new ObjectMapper().writeValueAsString(modelVersionObject);
                try {
                    Files.write(civitaiInfoFilePath, modelVersionJson.getBytes(StandardCharsets.UTF_8));
                    System.out.println("Created info file: " + civitaiInfoFilePath);
                } catch (IOException e) {
                    System.err.println("Failed to create info file: " + civitaiInfoFilePath);
                    e.printStackTrace();
                }

                // Attempt to create preview image
                String previewImageFileName = modelID + "_" + versionID + "_" + civitaiBaseModel + "_" + fName
                        + ".preview.png";
                Path previewImagePath = uniqueDirectory.resolve(previewImageFileName);
                boolean validImageFound = false;

                for (String imageUrl : imageUrlsArray) {
                    try {
                        BufferedImage image = ImageIO.read(new URL(imageUrl));
                        if (image != null) {
                            downloadImage(imageUrl, previewImagePath);
                            // ImageIO.write(image, "png", previewImagePath.toFile());
                            System.out.println("Saved preview image: " + previewImagePath.toString());
                            validImageFound = true;
                            break; // Stop after the first valid image
                        }
                    } catch (Exception e) {
                        System.out.println("Failed to download or process image from URL: " + imageUrl);
                    }
                }

                // Use placeholder if none found
                if (!validImageFound) {
                    try {
                        String placeholderUrl = "https://placehold.co/350x450.png";
                        BufferedImage placeholderImage = ImageIO.read(new URL(placeholderUrl));
                        if (placeholderImage != null) {
                            downloadImage(placeholderUrl, previewImagePath);
                            // ImageIO.write(placeholderImage, "png", previewImagePath.toFile());
                            System.out.println(
                                    "No valid image found, using online placeholder: " + previewImagePath.toString());
                        } else {
                            System.out.println("Failed to download the online placeholder image.");
                        }
                    } catch (Exception e) {
                        System.out.println("Failed to download or process the placeholder image.");
                    }
                }

                // Keep track of preview image
                finalPreviewImagePath = previewImagePath;
            }

            // If no files were successfully downloaded, skip zip creation
            if (!anyFileDownloaded) {
                System.out.println("No files were downloaded successfully; skipping zip creation for: " + name);

                // Optionally, remove the uniqueDirectory if you want no leftover
                try {
                    if (Files.exists(uniqueDirectory)) {
                        Files.walkFileTree(uniqueDirectory, new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                Files.delete(file);
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                                if (exc == null) {
                                    Files.delete(dir);
                                    return FileVisitResult.CONTINUE;
                                }
                                throw exc;
                            }
                        });
                    }
                    System.out.println("Cleaned up empty directory due to no successful downloads.");
                } catch (IOException e) {
                    System.err.println("Failed to clean up directory: " + uniqueDirectory);
                }

                // Instead of `return;` we throw an exception
                throw new CustomException("No files were downloaded successfully for: " + name);
            }

            // If we get here, at least one file was downloaded
            System.out.println("Download completed for: " + name);

            // ------------------------------------------------------------
            // 1) CREATE THE "INNER ZIP"
            // ------------------------------------------------------------
            String innerZipFileName = modelID + "_" + versionID + "_" + civitaiBaseModel + "_" + name + ".zip";
            Path innerZipFile = uniqueDirectory.resolve(innerZipFileName);
            System.out.println("Creating Inner ZIP at: " + innerZipFile);

            final Path finalUniqueDirectory = uniqueDirectory;
            try (ZipOutputStream innerZos = new ZipOutputStream(
                    new FileOutputStream(innerZipFile.toFile()))) {

                long totalSizeInner = ProgressBarUtils.calculateTotalSize(finalUniqueDirectory);
                System.out.println("Total size for Inner ZIP: " + totalSizeInner + " bytes");

                Files.walkFileTree(finalUniqueDirectory, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                        if (file.equals(innerZipFile) || attributes.isSymbolicLink()) {
                            return FileVisitResult.CONTINUE;
                        }
                        try (FileInputStream fis = new FileInputStream(file.toFile())) {
                            Path targetFile = finalUniqueDirectory.relativize(file);
                            ZipEntry zipEntry = new ZipEntry(targetFile.toString());
                            innerZos.putNextEntry(zipEntry);

                            byte[] buffer = new byte[4096];
                            int len;
                            long bytesRead = 0;
                            while ((len = fis.read(buffer)) > 0) {
                                innerZos.write(buffer, 0, len);
                                bytesRead += len;
                                // progress bar
                                ProgressBarUtils.updateProgressBar(
                                        bytesRead,
                                        totalSizeInner,
                                        "INNER-ZIP: " + innerZipFile.getFileName());
                            }
                            innerZos.closeEntry();
                            System.out.println("Added to Inner ZIP: " + targetFile);
                        } catch (IOException e) {
                            System.err.println("Failed to add file to Inner ZIP: " + file);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        System.err.printf("Unable to zip (inner) : %s%n%s%n", file, exc);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
            System.out.println("Inner ZIP created: " + innerZipFile);

            // ------------------------------------------------------------
            // 2) DELETE individual files, leaving only the .zip + preview
            // ------------------------------------------------------------
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(uniqueDirectory)) {
                for (Path file : stream) {
                    if (!file.equals(innerZipFile) &&
                            (finalPreviewImagePath == null || !file.equals(finalPreviewImagePath))) {
                        Files.delete(file);
                        System.out.println("Deleted file from unique directory: " + file);
                    }
                }
            } catch (IOException e) {
                System.err.println("Failed to delete individual files from unique directory.");
                e.printStackTrace();
            }

            // (Optional) final verification
            // ...

            System.out.println("\nZip-within-zip process completed for: " + name + ".zip" +
                    " and saved into " + downloadFilePath);

            // ------------------------------------------------------------
            // 4) Move .zip & .preview.png up, remove unique directory
            // ------------------------------------------------------------
            try {
                Path parentDirectory = uniqueDirectory.getParent();

                // Move the final zip up
                if (Files.exists(innerZipFile)) {
                    Path movedZip = parentDirectory.resolve(innerZipFile.getFileName());
                    Files.move(innerZipFile, movedZip, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Moved final ZIP to: " + movedZip);
                }

                // Move the preview image up
                if (finalPreviewImagePath != null && Files.exists(finalPreviewImagePath)) {
                    Path movedPreview = parentDirectory.resolve(finalPreviewImagePath.getFileName());
                    Files.move(finalPreviewImagePath, movedPreview, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Moved preview image to: " + movedPreview);
                }

                // Now remove the uniqueDirectory
                Files.walkFileTree(uniqueDirectory, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        if (exc != null)
                            throw exc;
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
                System.out.println("Removed unique directory: " + uniqueDirectory);

            } catch (IOException e) {
                System.err.println("Failed to move final files or remove unique directory.");
                e.printStackTrace();
            }

            System.out.println(baseDownloadDirectory.resolve(normalizedDownloadFilePath).toAbsolutePath().toUri());

        } catch (Exception e) {
            System.out.println("Error Model Name: " + modelName);
            e.printStackTrace();
            // update_error_model_list(modelName);

            // Cleanup if something failed
            if (uniqueDirectory != null && Files.exists(uniqueDirectory)) {
                try {
                    Files.walkFileTree(uniqueDirectory, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            if (exc != null)
                                throw exc;
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                    System.out.println("Deleted unique directory due to error: " + uniqueDirectory);
                } catch (IOException ioException) {
                    System.err.println("Failed to delete unique directory: " + uniqueDirectory);
                }
            }
            throw new CustomException("Download processing failed: " + getDeepErrorMessage(e), e);
        }
    }

    private String getDeepErrorMessage(Throwable ex) {
        if (ex == null)
            return "Unknown error";

        Throwable root = ex;
        while (root.getCause() != null) {
            root = root.getCause();
        }

        if (root.getMessage() != null && !root.getMessage().isBlank()) {
            return root.getMessage();
        }

        if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
            return ex.getMessage();
        }

        return root.getClass().getSimpleName();
    }

    // ------------------------------------------------------------------------
    // Save a PNG using TwelveMonkeys for better compression.
    // You need the TwelveMonkeys ImageIO plugin on the classpath.
    // ------------------------------------------------------------------------
    // This one does not contain meta data
    // private void saveOptimizedPng(BufferedImage image, File outputFile) throws
    // IOException {
    // ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
    // try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile)) {
    // writer.setOutput(ios);
    // ImageWriteParam param = writer.getDefaultWriteParam();
    // // We can set compression mode & quality (1.0f is “best”).
    // param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
    // param.setCompressionQuality(1.0f);

    // writer.write(null, new IIOImage(image, null, null), param);
    // } finally {
    // writer.dispose();
    // }
    // }

    public static void downloadImage(String imageUrl, Path previewImagePath) throws Exception {
        URL url = new URL(imageUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        try (InputStream inputStream = connection.getInputStream();
                FileOutputStream fileOutputStream = new FileOutputStream(previewImagePath.toFile())) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
            System.out.println("Image saved to " + previewImagePath.toString());
        }
    }

    /**
     * Utility class for handling progress updates and size calculations.
     */
    public static class ProgressBarUtils {
        public static void copyInputStreamWithProgress(InputStream inputStream,
                Path filePath,
                long totalSize,
                String fileName) throws IOException {
            byte[] buffer = new byte[4096];
            int bytesRead;
            long bytesCopied = 0;
            // Ensure the file is empty before starting
            Files.deleteIfExists(filePath);
            Files.createFile(filePath);
            try (BufferedOutputStream bos = new BufferedOutputStream(
                    Files.newOutputStream(filePath, StandardOpenOption.WRITE))) {
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                    bytesCopied += bytesRead;
                    updateProgressBar(bytesCopied, totalSize, fileName);
                }
            }
        }

        public static void updateProgressBar(long bytesRead, long totalSize, String fileName) {
            if (totalSize > 0) {
                int progress = (int) ((bytesRead * 100) / totalSize);
                // Use carriage return (\r) + flush instead of println
                System.out.print("\rDownloading " + fileName + ": " + progress + "%");
                System.out.flush();
            }
        }

        public static long calculateTotalSize(Path directory) throws IOException {
            final long[] total = { 0 };
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    total[0] += attrs.size();
                    return FileVisitResult.CONTINUE;
                }
            });
            return total[0];
        }
    }

    // ---- networking constants ----
    private static final int CONNECT_TIMEOUT_MS = 20_000;
    private static final int READ_TIMEOUT_MS = 60_000;
    private static final String USER_AGENT = "CivitaiSQL/1.0 (+java)";
    private static final int HTTP_PARTIAL_CODE = HttpURLConnection.HTTP_PARTIAL; // 206
    private static final int HTTP_OK_CODE = HttpURLConnection.HTTP_OK; // 200
    private static final int HTTP_RANGE_NOT_SAT = 416; // Requested Range Not Satisfiable

    // Robust downloader: redirects, timeouts, exact bytes (identity), Range resume,
    // and tail-probe when size is unknown to guarantee completeness.
    private boolean httpDownloadWithResume(String urlStr, Path filePath, String fileName, int maxRetries) {
        int attempt = 0;
        long backoffMs = 1000;

        while (++attempt <= maxRetries) {
            HttpURLConnection conn = null;
            try {
                long existing = java.nio.file.Files.exists(filePath) ? java.nio.file.Files.size(filePath) : 0L;

                URL url = URI.create(urlStr).toURL(); // avoids URL(String) deprecation on Java 20+
                conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setRequestProperty("Accept-Encoding", "identity"); // exact bytes; no auto-gzip

                if (existing > 0) {
                    conn.setRequestProperty("Range", "bytes=" + existing + "-");
                }

                int code = conn.getResponseCode();

                // If server ignored Range, start fresh
                if (existing > 0 && code == HTTP_OK_CODE) {
                    java.nio.file.Files.deleteIfExists(filePath);
                    existing = 0L;
                }

                if (code != HTTP_OK_CODE && code != HTTP_PARTIAL_CODE) {
                    throw new java.io.IOException("HTTP " + code + " for " + urlStr);
                }

                long contentLen = conn.getContentLengthLong(); // -1 if unknown
                long expectedTotal = (code == HTTP_PARTIAL_CODE && contentLen >= 0)
                        ? existing + contentLen
                        : (contentLen >= 0 ? contentLen : -1);

                try (java.io.InputStream in = new java.io.BufferedInputStream(conn.getInputStream());
                        java.io.OutputStream out = java.nio.file.Files.newOutputStream(
                                filePath,
                                existing > 0
                                        ? new java.nio.file.OpenOption[] { StandardOpenOption.APPEND }
                                        : new java.nio.file.OpenOption[] { StandardOpenOption.CREATE,
                                                StandardOpenOption.TRUNCATE_EXISTING })) {

                    byte[] buf = new byte[8192];
                    long written = existing;
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                        written += n;
                        if (expectedTotal > 0) {
                            ProgressBarUtils.updateProgressBar(written, expectedTotal, fileName);
                        }
                    }
                    out.flush();

                    if (expectedTotal > 0) {
                        // size known → require full
                        if (written < expectedTotal)
                            throw new java.io.EOFException("Early EOF: " + written + " / " + expectedTotal);
                        return true;
                    } else {
                        // size unknown → tail-probe until EOF is proven
                        long w = written;
                        while (true) {
                            int tail = tailProbeAppend(urlStr, filePath, w);
                            if (tail == 0) {
                                return true; // confirmed EOF
                            } else if (tail > 0) {
                                w += tail; // appended more; keep probing
                            } else {
                                throw new java.io.EOFException("Tail probe failed; retrying");
                            }
                        }
                    }
                }
            } catch (java.io.IOException ex) {
                // Keep partial for resume; delete only if tiny junk
                try {
                    if (java.nio.file.Files.exists(filePath) && java.nio.file.Files.size(filePath) < 1024) {
                        java.nio.file.Files.deleteIfExists(filePath);
                    }
                } catch (java.io.IOException ignore) {
                }

                if (attempt >= maxRetries)
                    return false;

                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                backoffMs = Math.min(backoffMs * 2, 8000);
                System.out.println("Retrying (" + (attempt + 1) + "/" + maxRetries + ") with resume if supported...");
            } finally {
                if (conn != null)
                    conn.disconnect();
            }
        }
        return false;
    }

    /**
     * Tail probe to prove EOF or fetch remaining tail.
     * 
     * @return >0 bytes appended, 0 = confirmed EOF, <0 = probe failed (retry)
     */
    private int tailProbeAppend(String urlStr, Path filePath, long written) {
        HttpURLConnection probe = null;
        try {
            URL probeUrl = URI.create(urlStr).toURL(); // avoids URL(String) deprecation
            probe = (HttpURLConnection) probeUrl.openConnection();
            probe.setInstanceFollowRedirects(true);
            probe.setConnectTimeout(CONNECT_TIMEOUT_MS);
            probe.setReadTimeout(READ_TIMEOUT_MS);
            probe.setRequestMethod("GET");
            probe.setRequestProperty("User-Agent", USER_AGENT);
            probe.setRequestProperty("Accept-Encoding", "identity");
            probe.setRequestProperty("Range", "bytes=" + written + "-");

            int code = probe.getResponseCode();

            if (code == HTTP_RANGE_NOT_SAT) {
                // 416 => beyond EOF → complete
                return 0;
            }
            if (code == HTTP_PARTIAL_CODE) { // 206
                try (java.io.InputStream in = new java.io.BufferedInputStream(probe.getInputStream());
                        java.io.OutputStream out = java.nio.file.Files.newOutputStream(filePath,
                                StandardOpenOption.APPEND)) {
                    byte[] buf = new byte[8192];
                    int n, total = 0;
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                        total += n;
                    }
                    return total;
                }
            }
            if (code == HTTP_OK_CODE) { // 200, server ignored Range
                return -1;
            }
            return -1;
        } catch (java.io.IOException e) {
            return -1;
        } finally {
            if (probe != null)
                probe.disconnect();
        }
    }

    // Optional tiny logger for debugging CDN behavior
    private void logResponse(String label, HttpURLConnection conn) throws java.io.IOException {
        System.out.println(label + " " + conn.getURL() + " -> " + conn.getResponseCode());
        System.out.println("  Content-Length: " + conn.getContentLengthLong());
        System.out.println("  Transfer-Encoding: " + conn.getHeaderField("Transfer-Encoding"));
        System.out.println("  Content-Encoding: " + conn.getHeaderField("Content-Encoding"));
        System.out.println("  Accept-Ranges: " + conn.getHeaderField("Accept-Ranges"));
        System.out.println("  Server: " + conn.getHeaderField("Server"));
        System.out.println("  Via/X-Cache: " + conn.getHeaderField("Via") + " / " + conn.getHeaderField("X-Cache"));
    }

    /**
     * Checks whether 'source' contains 'keyword' in a case-insensitive way.
     */
    private boolean containsIgnoreCase(String source, String keyword) {
        return source != null && keyword != null && source.toLowerCase().contains(keyword.toLowerCase());
    }

    @Override
    public void updateAllPngs(Path downloadFolder) throws IOException, InterruptedException {

        // 1) Regex to capture:
        // group(1) = modelId (digits)
        // group(2) = versionId (digits)
        // group(3) = baseModel (word characters)
        // group(4) = original name (everything else) before ".preview.png"
        Pattern FILENAME_PATTERN = Pattern.compile(
                "^(\\d+)_(\\d+)_(\\w+)_(.+)\\.preview\\.png$");

        // 2) Validate the folder
        if (!Files.exists(downloadFolder)) {
            System.out.println("Folder does not exist: " + downloadFolder);
            return;
        }

        // 3) Gather all PNG files (recursively)
        List<Path> pngFiles;
        try (Stream<Path> walk = Files.walk(downloadFolder)) {
            pngFiles = walk
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".png"))
                    .collect(Collectors.toList());
        }

        if (pngFiles.isEmpty()) {
            System.out.println("No PNG files found under: " + downloadFolder);
            return;
        }

        // 4) Process each PNG
        for (Path pngFile : pngFiles) {
            String fileName = pngFile.getFileName().toString();
            Matcher matcher = FILENAME_PATTERN.matcher(fileName);

            if (!matcher.matches()) {
                // Skip if it doesn't match the
                // {modelId}_{versionId}_{baseModel}_{fileName}.preview.png pattern
                System.out.println("Skipping file (no match): " + fileName);
                continue;
            }

            // Extract data from the filename
            String modelId = matcher.group(1); // e.g. "1231563"
            String versionId = matcher.group(2); // e.g. "51651"
            String baseModel = matcher.group(3); // e.g. "Pony"
            String origName = matcher.group(4); // e.g. "abcdef"

            System.out.println("\n=== Found PNG: " + fileName + " ===");
            System.out.println("  modelId   = " + modelId);
            System.out.println("  versionId = " + versionId);
            System.out.println("  baseModel = " + baseModel);
            System.out.println("  fileName  = " + origName);

            // 5) Get a replacement PNG URL (placeholder logic)
            String newPngUrl = null;
            try {
                // Normally you’d do an HTTP call here to fetch a real URL based on
                // modelId/versionId
                // For demonstration, we just use a placeholder image
                newPngUrl = "https://placehold.co/350x450.png";
            } catch (Exception e) {
                System.out.println("  Failed to get new PNG URL, skipping: " + fileName);
                e.printStackTrace();
                continue;
            }

            // 6) If we got a valid URL, download & overwrite
            if (newPngUrl != null && !newPngUrl.isEmpty()) {
                System.out.println("  Downloading new image from: " + newPngUrl);
                try {
                    // Raw download of the image bytes (inline logic)
                    URL url = new URL(newPngUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    // If your server needs auth headers, set them here:
                    // connection.setRequestProperty("Authorization", "Bearer <token>");

                    // Download & overwrite the file
                    try (InputStream in = connection.getInputStream();
                            FileOutputStream out = new FileOutputStream(pngFile.toFile())) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                    System.out.println("  Overwrote " + fileName + " with new image bytes.");

                } catch (Exception e) {
                    System.out.println("  Download or file overwrite failed: " + fileName);
                    e.printStackTrace();
                }
            } else {
                System.out.println("  Invalid or empty URL, skipping: " + fileName);
            }

            // 7) Delay 1 second (1000 ms) to avoid spamming requests
            Thread.sleep(1000);
        }
    }

    @Override
    public String check_model_version_file_exists(String modelID, String versionID) {
        try {
            if (modelID == null || modelID.trim().isEmpty() ||
                    versionID == null || versionID.trim().isEmpty()) {
                return null;
            }

            String normalizedModelID = modelID.trim();
            String normalizedVersionID = versionID.trim();
            String filePrefix = normalizedModelID + "_" + normalizedVersionID + "_";

            Path downloadRoot = Paths.get("files", "download");

            if (!Files.exists(downloadRoot) || !Files.isDirectory(downloadRoot)) {
                return null;
            }

            try (Stream<Path> walk = Files.walk(downloadRoot)) {
                return walk
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().startsWith(filePrefix))
                        .map(path -> path.toAbsolutePath().normalize().toString())
                        .findFirst()
                        .orElse(null);
            }

        } catch (IOException e) {
            log.error("Unexpected error while checking model/version file existence", e);
            throw new CustomException("An unexpected error occurred", e);
        }
    }

    @Override
    public int move_model_version_files_to_delete(String modelID, String versionID) {
        try {
            if (modelID == null || modelID.trim().isEmpty()
                    || versionID == null || versionID.trim().isEmpty()) {
                return 0;
            }

            String normalizedModelID = modelID.trim();
            String normalizedVersionID = versionID.trim();
            String filePrefix = normalizedModelID + "_" + normalizedVersionID + "_";

            Path downloadRoot = Paths.get("files", "download");
            Path deleteRoot = Paths.get("files", "download", "@scan@", "delete");

            if (!Files.exists(downloadRoot) || !Files.isDirectory(downloadRoot)) {
                return 0;
            }

            if (!Files.exists(deleteRoot)) {
                Files.createDirectories(deleteRoot);
            }

            List<Path> matchedFiles;
            try (Stream<Path> walk = Files.walk(downloadRoot)) {
                matchedFiles = walk
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().startsWith(filePrefix))
                        .collect(Collectors.toList());
            }

            int movedCount = 0;

            for (Path sourcePath : matchedFiles) {
                String fileName = sourcePath.getFileName().toString();
                Path targetPath = buildUniqueDeleteTargetPath(deleteRoot, fileName);

                Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                movedCount++;
            }

            return movedCount;

        } catch (IOException e) {
            log.error("Unexpected error while moving model/version files to delete folder", e);
            throw new CustomException("An unexpected error occurred", e);
        }
    }

    private Path buildUniqueDeleteTargetPath(Path deleteRoot, String fileName) throws IOException {
        Path candidate = deleteRoot.resolve(fileName);

        if (!Files.exists(candidate)) {
            return candidate;
        }

        String baseName = fileName;
        String extension = "";

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex);
        }

        int counter = 1;
        while (true) {
            Path nextCandidate = deleteRoot.resolve(baseName + " (" + counter + ")" + extension);
            if (!Files.exists(nextCandidate)) {
                return nextCandidate;
            }
            counter++;
        }
    }

}
