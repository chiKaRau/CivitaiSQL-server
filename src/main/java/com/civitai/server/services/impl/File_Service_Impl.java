package com.civitai.server.services.impl;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

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

        // Create a folder_list if have none
        create_folder_list();

        // Create a cart_list if have none
        create_cart_list();

        // Create a must_add_list if have none
        create_must_add_list();

        // Create a tags list if have none
        create_tags_list();

        // Create a error_model_list if have none
        create_error_model_list();
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
            LocalDateTime oneMonthAgo = now.minusMonths(1);

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

                //prevent "/update/"
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

    @SuppressWarnings("unchecked")
    @Override
    public void update_error_model_list(String modelName) {
        try {

            Path filePath = Path.of("files/data/error_model_list.json");

            // Read the content of folder_list.json
            String data = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);

            // Parse the JSON data into a list
            ObjectMapper objectMapper = new ObjectMapper();
            List<String> errorModelList = objectMapper.readValue(data, List.class);

            // Check if the URL is already in the list
            if (!errorModelList.contains(modelName)) {
                // Add the new URL to the list
                errorModelList.add(modelName);

                // Convert the updated list back to a JSON string
                String updatedErrorModelListJson = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(errorModelList);

                // Write the updated JSON string back to the file
                Files.write(filePath, updatedErrorModelListJson.getBytes(StandardCharsets.UTF_8));
                System.out.println("Added \"" + modelName + "\" to error_model_list.json.");
            } else {
                // Do nothing if download path already existed.
                System.out.println("\"" + modelName + "\" is already in error_model_list.json. No duplicates allowed.");
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
            update_cart_list(url);

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

            update_error_model_list(modelName);

            FileUtils.deleteDirectory(modelDirectory);

            //log.error("Unexpected error while downloading file", e);
            throw new CustomException("An unexpected error occurred", e);

        }
    }

    @Override
    public void download_file_by_server_v2(String civitaiFileName, List<Map<String, Object>> civitaiModelFileList,
            String downloadFilePath, Map<String, Object> modelVersionObject, String civitaiModelID,
            String civitaiVersionID, String civitaiUrl, String civitaiBaseModel, String[] imageUrlsArray) {

        String modelID = civitaiModelID, versionID = civitaiVersionID, url = civitaiUrl,
                name = civitaiFileName.split("\\.")[0];

        String modelName = modelID + "_" + versionID + "_" + civitaiBaseModel + "_" + name;
        Path modelDirectory = Paths.get("files/download/", modelName);

        // Load the configuration file
        ConfigUtils.loadConfig("civitaiConfig.json");

        // Get a specific configuration value
        String civitaiApiKey = ConfigUtils.getConfigValue("apiKey");

        try {
            String downloadPath = "/" + modelID + "_" + versionID + "_" + civitaiBaseModel + "_"
                    + name
                    + downloadFilePath;

            // Check if the downloadPath inside the folder_list.json,
            // if not, append it
            update_folder_list(downloadFilePath);
            update_cart_list(url);

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
            for (Map<String, Object> data : civitaiModelFileList) {
                String fileName = modelID + "_" + versionID + "_" + civitaiBaseModel + "_" + data.get("name");

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

                // Create .civitai.info file
                String fName = civitaiFileName.replace(".safetensors", "");
                String civitaiInfoFileName = modelID + "_" + versionID + "_" + civitaiBaseModel + "_" + fName
                        + ".civitai.info";
                Path civitaiInfoFilePath = currentPath.resolve(civitaiInfoFileName);

                String modelVersionJson = new ObjectMapper().writeValueAsString(modelVersionObject);

                Files.write(civitaiInfoFilePath, modelVersionJson.getBytes(StandardCharsets.UTF_8));

                System.out.println("Created: " + civitaiInfoFilePath.toString());

                // Handle preview image
                String previewImageFileName = modelID + "_" + versionID + "_" + civitaiBaseModel + "_" + fName
                        + ".preview.png";
                Path previewImagePath = currentPath.resolve(previewImageFileName);
                boolean validImageFound = false;

                for (String imageUrl : imageUrlsArray) {
                    try {
                        BufferedImage image = ImageIO.read(new URL(imageUrl));
                        if (image != null) {
                            downloadImage(imageUrl, previewImagePath);
                            //ImageIO.write(image, "png", previewImagePath.toFile());
                            System.out.println("Saved preview image: " + previewImagePath.toString());
                            validImageFound = true;
                            break; // Stop after the first valid image
                        }
                    } catch (Exception e) {
                        System.out.println("Failed to download or process image from URL: " + imageUrl);
                    }
                }

                // Use an online placeholder if no valid image was found
                if (!validImageFound) {
                    try {
                        String placeholderUrl = "https://placehold.co/350x450.png";
                        BufferedImage placeholderImage = ImageIO.read(new URL(placeholderUrl));
                        if (placeholderImage != null) {
                            downloadImage(placeholderUrl, previewImagePath);
                            //ImageIO.write(placeholderImage, "png", previewImagePath.toFile());
                            System.out.println(
                                    "No valid image found, using online placeholder: " + previewImagePath.toString());
                        } else {
                            System.out.println("Failed to download the online placeholder image.");
                        }
                    } catch (Exception e) {
                        System.out.println("Failed to download the online placeholder image.");
                    }
                }
            }

            // Log download completion
            System.out.println("Download completed for: " + name);

            // Create ZIP archive
            Path zipFilePath = Paths.get("files/download/",
                    modelID + "_" + versionID + "_" + civitaiBaseModel + "_" + name);
            Path zipFile = Paths.get("files/download/",
                    modelID + "_" + versionID + "_" + civitaiBaseModel + "_" + name
                            + ".zip");

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
                                        modelID + "_" + versionID + "_" + civitaiBaseModel + "_" + name + ".zip");

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

            update_error_model_list(modelName);

            FileUtils.deleteDirectory(modelDirectory);

            //log.error("Unexpected error while downloading file", e);
            throw new CustomException("An unexpected error occurred", e);

        }
    }

    // Method to save optimized PNG using TwelveMonkeys
    public void saveOptimizedPng(BufferedImage image, File outputFile) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(1.0f); // Max quality, but optimized size

            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }

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

}
