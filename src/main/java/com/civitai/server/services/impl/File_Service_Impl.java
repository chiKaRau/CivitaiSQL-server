package com.civitai.server.services.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.civitai.server.exception.CustomException;
import com.civitai.server.services.File_Service;
import com.civitai.server.utils.ProgressBarUtils;
import com.fasterxml.jackson.core.type.TypeReference;

import jakarta.annotation.PostConstruct;

@Service
public class File_Service_Impl implements File_Service {

    private static final Logger log = LoggerFactory.getLogger(CivitaiSQL_Service_Impl.class);

    @PostConstruct
    public void createFileAtStartup() {

        // Create a folder_list if have none
        create_folder_list();

        // Create a cart_list if have none
        create_cart_list();
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

    //TODO return payload
    @Override
    public List<String> get_folders_list() {

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<String> dataList = objectMapper.readValue(
                    Files.readAllBytes(Paths.get("files/data/folder_list.json")),
                    new TypeReference<List<String>>() {
                    });

            // System.out.println(dataList);
            return dataList;
        } catch (IOException e) {
            // Log and handle other types of exceptions
            log.error("Unexpected error while retreving folder list", e);
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

    //TODO return payload
    @Override
    public Boolean check_cart_list(String url) {
        try {
            String cartListFile = "files/data/cart_list.json";

            // Read the content of cart_list.json
            String data = new String(Files.readAllBytes(Path.of(cartListFile)));

            // Parse the JSON data into an array
            String[] cartList = new ObjectMapper().readValue(data, String[].class);

            // Check if the URL is already in the array
            for (String cartedUrl : cartList) {
                if (cartedUrl.equals(url)) {
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
                // Otherwise, Update folder_list.json
                jsonData.add(downloadFilePath + "/");
                Files.write(filePath, objectMapper.writeValueAsString(jsonData).getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                // Update folder_list.txt
                Files.write(Path.of("files/data/folder_list.txt"), ("." + downloadFilePath + "\n").getBytes(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);

                System.out.println(downloadFilePath + " has added to the array and files updated.");
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
                URL downloadUrl = new URL((String) data.get("downloadUrl"));
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
            System.out.println("\nZip process completed for: " + "\u001B[1m" + name + ".zip" + "\u001B[0m");

        } catch (Exception e) {
            // Log and handle other types of exceptions
            log.error("Unexpected error while downloading file", e);
            throw new CustomException("An unexpected error occurred", e);
        }
    }
}
