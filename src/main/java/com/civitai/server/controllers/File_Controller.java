package com.civitai.server.controllers;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.civitai.server.services.CivitaiSQL_Service;
import com.civitai.server.services.Civitai_Service;
import com.civitai.server.services.File_Service;
import com.civitai.server.utils.CustomResponse;

import com.civitai.server.utils.JsonUtils;

import jakarta.annotation.PostConstruct;

@RestController
@RequestMapping("/api")
public class File_Controller {

    private File_Service fileService;
    private Civitai_Service civitai_Service;
    private CivitaiSQL_Service civitaiSQL_Service;

    // @PostConstruct
    // public void createFileAtStartup() {
    //     try {
    //         Path downloadFolder = Paths
    //                 .get("G:\\Start_Here_Mac.app\\Formats\\AI Tools\\Stable Diffusion\\BackUp\\Temp\\New folder");
    //         updateAllPngs(downloadFolder);
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     }
    // }

    // // ---------------------------------------------------------------
    // // The SINGLE METHOD that does the update process
    // // ---------------------------------------------------------------
    // private void updateAllPngs(Path downloadFolder) throws InterruptedException, IOException {

    //     // Regex to match:
    //     //   {modelId}_{versionId}_{baseModel}_{someName}.preview.png
    //     // Example: "1231563_51651_Pony_abcdef.preview.png"
    //     Pattern FILENAME_PATTERN = Pattern.compile(
    //             "^(\\d+)_(\\d+)_(\\w+)_(.+)\\.preview\\.png$");

    //     // 1) Validate folder
    //     if (!Files.exists(downloadFolder)) {
    //         System.out.println("Folder does not exist: " + downloadFolder);
    //         return;
    //     }

    //     // 2) Gather *.png files (recursively)
    //     List<Path> pngFiles;
    //     try (Stream<Path> walk = Files.walk(downloadFolder)) {
    //         pngFiles = walk
    //                 .filter(Files::isRegularFile)
    //                 .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".png"))
    //                 .collect(Collectors.toList());
    //     }

    //     if (pngFiles.isEmpty()) {
    //         System.out.println("No PNG files found under: " + downloadFolder);
    //         return;
    //     }

    //     // 3) Loop through each .png
    //     for (Path pngFile : pngFiles) {
    //         String fileName = pngFile.getFileName().toString();
    //         Matcher matcher = FILENAME_PATTERN.matcher(fileName);

    //         // Skip if not matching your naming pattern
    //         if (!matcher.matches()) {
    //             System.out.println("Skipping file (no match): " + fileName);
    //             continue;
    //         }

    //         // Extract modelId, versionId, baseModel, etc.
    //         String modelId = matcher.group(1); // e.g. "1231563"
    //         String versionId = matcher.group(2); // e.g. "51651"
    //         String baseModel = matcher.group(3); // e.g. "Pony"
    //         String origName = matcher.group(4); // e.g. "abcdef"

    //         System.out.println("\n=== Found PNG: " + fileName + " ===");
    //         System.out.println("  modelId   = " + modelId);
    //         System.out.println("  versionId = " + versionId);
    //         System.out.println("  baseModel = " + baseModel);
    //         System.out.println("  fileName  = " + origName);

    //         // 4) Retrieve the image URL from your database/service
    //         //    Adjust to match your method signature:
    //         //    findFirstImageUrlByModelNumberAndVersionNumber(String model, String version)
    //         Optional<String> urlOpt = civitaiSQL_Service.findFirstImageUrlByModelNumberAndVersionNumber(modelId,
    //                 versionId);

    //         if (!urlOpt.isPresent() || urlOpt.get().isEmpty()) {
    //             System.out.println("No image URL found in DB for " + modelId + "/" + versionId + "; skipping.");
    //             continue;
    //         }

    //         String remoteImageUrl = urlOpt.get();
    //         System.out.println("  DB returned URL: " + remoteImageUrl);

    //         // 5) Download raw bytes from that URL & overwrite local .png
    //         try {
    //             downloadImage(remoteImageUrl, pngFile);
    //             System.out.println("  Overwrote local PNG: " + pngFile);
    //         } catch (Exception e) {
    //             System.out.println("  Download/overwrite failed for file: " + fileName);
    //             e.printStackTrace();
    //         }

    //         // 6) Delay 1 second to avoid spamming
    //         Thread.sleep(2000);
    //     }
    // }

    // ---------------------------------------------------------------
    // The "raw byte" download in the same class for convenience
    // ---------------------------------------------------------------
    private void downloadImage(String imageUrl, Path outputPath) throws Exception {
        URL url = new URL(imageUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // If you need auth headers, do it here:
        // connection.setRequestProperty("Authorization", "Bearer <token>");

        try (InputStream inputStream = connection.getInputStream();
                FileOutputStream fileOutputStream = new FileOutputStream(outputPath.toFile())) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    @Autowired
    public File_Controller(File_Service fileService, Civitai_Service civitai_Service,
            CivitaiSQL_Service civitaiSQL_Service) {
        this.fileService = fileService;
        this.civitai_Service = civitai_Service;
        this.civitaiSQL_Service = civitaiSQL_Service;
    }

    @GetMapping("/open-download-directory")
    public ResponseEntity<CustomResponse<String>> openDownloadDirectory() {
        fileService.open_download_directory();
        return ResponseEntity.ok().body(CustomResponse.success("Success open directory."));
    }

    @GetMapping("/clear-cart-list")
    public ResponseEntity<CustomResponse<String>> clearCartList() {
        fileService.empty_cart_list();
        return ResponseEntity.ok().body(CustomResponse.success("Success clear cartlist"));
    }

    @PostMapping("/append-to-must-add-list")
    public ResponseEntity<CustomResponse<String>> AppendToMustAddList(@RequestBody Map<String, Object> requestBody) {
        String url = (String) requestBody.get("url");
        // Validate null or empty
        if (url == null || url == "") {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }
        fileService.update_must_add_list(url);
        return ResponseEntity.ok().body(CustomResponse.success("Success append to mustAddList"));
    }

    @PostMapping("/check-cart-list")
    public ResponseEntity<CustomResponse<Map<String, Boolean>>> checkCartList(
            @RequestBody Map<String, Object> requestBody) {
        String url = (String) requestBody.get("url");

        // Validate null or empty
        if (url == null || url == "") {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }
        Boolean isCarted = fileService.check_cart_list(url);

        if (isCarted != null) {
            Map<String, Boolean> payload = new HashMap<>();
            payload.put("isCarted", isCarted);

            return ResponseEntity.ok().body(CustomResponse.success("FoldersList retrieval successful", payload));
        } else {
            return ResponseEntity.ok().body(CustomResponse.failure("No Model found in the database"));
        }
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/get_offline_download_list")
    public ResponseEntity<CustomResponse<Map<String, List<Map<String, Object>>>>> getOfflineDownloadList() {
        List<Map<String, Object>> offlineDownloadList = fileService.get_offline_download_list();

        if (!offlineDownloadList.isEmpty()) {
            Map<String, List<Map<String, Object>>> payload = new HashMap<>();
            payload.put("offlineDownloadList", offlineDownloadList);

            return ResponseEntity.ok()
                    .body(CustomResponse.success("Offline Download List retrieval successful", payload));
        } else {
            return ResponseEntity.ok().body(CustomResponse.failure("No offline downloads found"));
        }
    }

    @GetMapping("/get_folders_list")
    public ResponseEntity<CustomResponse<Map<String, List<String>>>> getFoldersList() {
        List<String> foldersList = fileService.get_folders_list();

        if (!foldersList.isEmpty()) {
            Map<String, List<String>> payload = new HashMap<>();
            payload.put("foldersList", foldersList);

            return ResponseEntity.ok().body(CustomResponse.success("FoldersList retrieval successful", payload));
        } else {
            return ResponseEntity.ok().body(CustomResponse.failure("No Model found in the database"));
        }
    }

    @GetMapping("/get_pending_remove_tags_list")
    public ResponseEntity<CustomResponse<Map<String, List<String>>>> getPendingRemoveTagsList() {
        List<String> pendingRemoveTagsList = fileService.get_pending_remove_tags_list();

        if (!pendingRemoveTagsList.isEmpty()) {
            Map<String, List<String>> payload = new HashMap<>();
            payload.put("pendingRemoveTagsList", pendingRemoveTagsList);

            return ResponseEntity.ok()
                    .body(CustomResponse.success("pendingRemoveTagsList retrieval successful", payload));
        } else {
            return ResponseEntity.ok().body(CustomResponse.failure("No Model found in the database"));
        }
    }

    @PostMapping("/add_pending_remove_tag")
    public ResponseEntity<CustomResponse<Void>> addPendingRemoveTag(
            @RequestBody Map<String, Object> requestBody) {

        // Extract the fields from the request body
        String pendingRemoveTag = (String) requestBody.get("pendingRemoveTag");

        // Validation (optional) - make sure 'pendingRemoveTag' is not null/empty
        if (pendingRemoveTag == null || pendingRemoveTag.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(CustomResponse.failure("Invalid input: pendingRemoveTag cannot be null or empty"));
        }

        // Call the service method to add the tag
        fileService.add_pending_remove_tag(pendingRemoveTag);

        return ResponseEntity.ok(CustomResponse.success("Tag added successfully", null));
    }

    @PostMapping("/compare-combination-to-pending-remove-tags-list")
    public ResponseEntity<CustomResponse<Map<String, List<List<String>>>>> compareCombinationToPendingRemoveTagsList(
            @RequestBody Map<String, Object> requestBody) {

        @SuppressWarnings("unchecked")
        List<List<String>> combinations = (List<List<String>>) requestBody.get("combinations");
        if (combinations == null) {
            combinations = Collections.emptyList();
        }

        // 2) Fetch your pending‐remove list
        List<String> pendingRemove = fileService.get_pending_remove_tags_list();
        // Normalize to lowercase for safe matching
        List<String> removeLower = pendingRemove.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        // --- DEBUG LOGGING ---
        // System.out.println("Pending-remove tags: " + removeLower);
        // System.out.println("Incoming combinations (" + combinations.size() + "):");
        // combinations.forEach(c -> System.out.println("  " + c));
        // ---------------------

        // 3) Filter: keep any combo where at least one element is in pendingRemove
        // 3) Filter: **exclude** any combo that contains at least one pending-remove tag
        List<List<String>> matched = combinations.stream()
                .filter(combo -> combo.stream()
                        .map(String::toLowerCase)
                        .noneMatch(removeLower::contains) // <-- reversed here
                )
                .distinct()
                .collect(Collectors.toList());

        // --- DEBUG LOGGING ---
        // System.out.println("Matched combinations (" + matched.size() + "):");
        // matched.forEach(c -> System.out.println("  " + c));
        // ---------------------

        Map<String, List<List<String>>> payload = Collections.singletonMap("matchedCombinations", matched);
        return ResponseEntity.ok(CustomResponse.success("Filtered combinations retrieved", payload));
    }

    @GetMapping("/get_error_model_list")
    public ResponseEntity<CustomResponse<Map<String, List<String>>>> getErrorModelList() {
        List<String> errorModelList = fileService.get_error_model_list();

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
        List<Map<String, Object>> creatorUrlList = fileService.get_creator_url_list();

        Map<String, List<Map<String, Object>>> payload = new HashMap<>();
        payload.put("creatorUrlList", creatorUrlList);

        return ResponseEntity.ok().body(CustomResponse.success("creatorUrlList retrieval successful", payload));
    }

    @GetMapping("/get_tags_list")
    public ResponseEntity<CustomResponse<Map<String, List<Map<String, Object>>>>> getTagsList(
            @RequestParam(value = "prefix", required = false) String prefix) {
        // Retrieve both top 10 and recent 10 lists based on prefix
        Map<String, List<Map<String, Object>>> tagsMap = fileService.get_tags_list(prefix);

        // Check if both lists are empty
        if (!tagsMap.get("topTags").isEmpty() || !tagsMap.get("recentTags").isEmpty()) {
            return ResponseEntity.ok().body(CustomResponse.success("TagsList retrieval successful", tagsMap));
        } else {
            return ResponseEntity.ok().body(CustomResponse.failure("No tags found in the database"));
        }
    }

    @GetMapping("/get_categories_prefix_list")
    public ResponseEntity<CustomResponse<Map<String, List<Map<String, String>>>>> getCategoriesPrefixsList() {
        List<Map<String, String>> categoriesPrefixsList = fileService.get_categories_prefix_list();

        if (!categoriesPrefixsList.isEmpty()) {
            Map<String, List<Map<String, String>>> payload = new HashMap<>();
            payload.put("categoriesPrefixsList", categoriesPrefixsList);

            return ResponseEntity.ok().body(CustomResponse.success("Categories prefix retrieval successful", payload));
        } else {
            return ResponseEntity.ok().body(CustomResponse.failure("No categories found in the database"));
        }
    }

    @GetMapping("/get_filePath_categories_list")
    public ResponseEntity<CustomResponse<Map<String, List<Map<String, String>>>>> getFilePathCategoriesList() {
        List<Map<String, String>> filePathCategoriesList = fileService.get_filePath_categories_list();

        if (!filePathCategoriesList.isEmpty()) {
            Map<String, List<Map<String, String>>> payload = new HashMap<>();
            payload.put("filePathCategoriesList", filePathCategoriesList);

            return ResponseEntity.ok()
                    .body(CustomResponse.success("filePathCategoriesList retrieval successful", payload));
        } else {
            return ResponseEntity.ok().body(CustomResponse.failure("No categories found in the database"));
        }
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/download-file-server")
    public ResponseEntity<CustomResponse<String>> downloadFileServer(@RequestBody Map<String, Object> requestBody) {
        /*{
        "modelID": "123",
        "loraFileName": "example.txt",
        "versionID": "1.0",
        "downloadFilePath": "/downloads",
        "nameAndDownloadUrlArray": [
        {"name": "file1", "downloadUrl": "https://example.com/file1"},
        {"name": "file2", "downloadUrl": "https://example.com/file2"}
        ],
        "loraURL": "https://example.com/lora"
        } */
        String url = (String) requestBody.get("url");
        String name = ((String) requestBody.get("name")).split("\\.")[0];
        String modelID = (String) requestBody.get("modelID");
        String versionID = (String) requestBody.get("versionID");
        String downloadFilePath = (String) requestBody.get("downloadFilePath");
        List<Map<String, Object>> filesList = (List<Map<String, Object>>) requestBody.get("filesList");

        // Validate null or empty
        if (url == null || url == "" || name == null || name == "" ||
                modelID == null || modelID == "" || versionID == null || versionID == "" ||
                downloadFilePath == null || downloadFilePath == "" || filesList == null || filesList.isEmpty()) {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        fileService.download_file_by_server(name, modelID, versionID, downloadFilePath, filesList, url);
        return ResponseEntity.ok().body(CustomResponse.success("Success download file"));
    }

    @PostMapping("/download-file-browser")
    public ResponseEntity<CustomResponse<String>> downloadFileBrowser(@RequestBody Map<String, Object> requestBody) {
        String downloadFilePath = (String) requestBody.get("downloadFilePath");
        String url = (String) requestBody.get("url");

        // Validate null or empty
        if (url == null || url == "" || downloadFilePath == null || downloadFilePath == "") {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        fileService.update_folder_list(downloadFilePath);
        fileService.update_cart_list(url);
        fileService.update_tags_list(downloadFilePath);
        //fileService.remove_from_offline_download_list(civitaiModelID, civitaiVersionID);
        return ResponseEntity.ok().body(CustomResponse.success("Success download file"));
    }

    /**
    * Endpoint to backup the offline_download_list.json file.
    *
    * @return ResponseEntity with isBackedUp flag.
    */
    @PostMapping("/backup_offline_download_list")
    public ResponseEntity<CustomResponse<Map<String, Boolean>>> backupOfflineDownloadList() {
        boolean isBackedUp = fileService.backupOfflineDownloadList();

        Map<String, Boolean> payload = new HashMap<>();
        payload.put("isBackedUp", isBackedUp);

        if (isBackedUp) {
            return ResponseEntity.ok()
                    .body(CustomResponse.success("Backup created successfully.", payload));
        } else {
            return ResponseEntity.status(500)
                    .body(CustomResponse.failure("Backup failed. Please check server logs for details."));
        }
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
                fileService.update_offline_download_list(
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
            fileService.remove_from_offline_download_list(civitaiModelID, civitaiVersionID);
            return ResponseEntity.ok()
                    .body(CustomResponse.success("Success remove download file from offline download list"));

        } catch (Exception ex) {
            System.err.println("An error occurred: " + ex.getMessage());
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/update_creator_url_list")
    public ResponseEntity<CustomResponse<String>> updateCreatorUrlList(
            @RequestBody Map<String, Object> requestBody) {

        String creatorUrl = (String) requestBody.get("creatorUrl");
        String status = (String) requestBody.get("status");
        Boolean lastChecked = (Boolean) requestBody.get("lastChecked");

        // Validate null or empty
        if (creatorUrl == null || creatorUrl == "") {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        try {

            fileService.update_creator_url_list(creatorUrl, status, lastChecked);

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

            fileService.remove_creator_url(creatorUrl);

            return ResponseEntity.ok()
                    .body(CustomResponse.success("Success removing creator url from list"));

        } catch (Exception ex) {
            System.err.println("Error - " + creatorUrl + " : "
                    + ex.getMessage());
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
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
            fileService.remove_from_error_model_list(civitaiModelID, civitaiVersionID);
            return ResponseEntity.ok()
                    .body(CustomResponse.success("Success remove download file from offline download list"));

        } catch (Exception ex) {
            System.err.println("An error occurred: " + ex.getMessage());
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/download-file-server-v2")
    public ResponseEntity<CustomResponse<String>> downloadFileServerV2(@RequestBody Map<String, Object> requestBody) {

        Map<String, Object> modelObject = (Map<String, Object>) requestBody.get("modelObject");
        String civitaiFileName = ((String) modelObject.get("civitaiFileName"));
        List<Map<String, Object>> civitaiModelFileList = (List<Map<String, Object>>) modelObject
                .get("civitaiModelFileList");
        String downloadFilePath = (String) modelObject.get("downloadFilePath");
        String civitaiUrl = (String) modelObject.get("civitaiUrl");
        String civitaiModelID = (String) modelObject.get("civitaiModelID");
        String civitaiVersionID = (String) modelObject.get("civitaiVersionID");

        // Validate null or empty
        if (modelObject == null ||
                civitaiUrl == null || civitaiUrl == "" ||
                downloadFilePath == null || downloadFilePath == "" ||
                civitaiUrl == null || civitaiUrl == "" ||
                civitaiModelID == null || civitaiModelID == "" ||
                civitaiVersionID == null || civitaiVersionID == "" ||
                civitaiModelFileList == null || civitaiModelFileList.isEmpty()) {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        Optional<Map<String, Object>> modelVersionOptional = null;

        try {
            modelVersionOptional = civitai_Service.findModelByVersionID(civitaiVersionID);
        } catch (Exception ex) {
            System.err.println("Failed calling civitai to retrieve model information" + ex.getMessage());
            String modelID = civitaiModelID, versionID = civitaiVersionID, url = civitaiUrl,
                    name = civitaiFileName.split("\\.")[0].trim();
            String modelName = modelID + "_" + versionID + "_" + name;

            // Map<String, Object> modelVersionObject = modelVersionOptional.get();

            fileService.update_error_model_list(modelName);
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        try {
            if (!modelVersionOptional.isPresent()) {
                return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
            } else {
                Map<String, Object> modelVersionObject = modelVersionOptional.get();

                // Extract URLs
                String[] imageUrlsArray = JsonUtils.extractImageUrls(modelVersionObject);

                fileService.download_file_by_server_v2(civitaiFileName, civitaiModelFileList, downloadFilePath,
                        modelVersionObject, civitaiModelID, civitaiVersionID, civitaiUrl,
                        (String) modelVersionObject.get("baseModel"), imageUrlsArray);

                fileService.update_tags_list(downloadFilePath);
                fileService.remove_from_offline_download_list(civitaiModelID, civitaiVersionID);
                return ResponseEntity.ok().body(CustomResponse.success("Success download file"));
            }

        } catch (Exception ex) {
            String modelID = civitaiModelID, versionID = civitaiVersionID, url = civitaiUrl,
                    name = civitaiFileName.split("\\.")[0].trim();
            String modelName = modelID + "_" + versionID + "_" + name;

            // Map<String, Object> modelVersionObject = modelVersionOptional.get();

            fileService.update_error_model_list(modelName);
            // List<String> emptyList = new ArrayList<>();
            // fileService.update_error_model_list_v2(civitaiFileName, civitaiModelFileList, downloadFilePath, modelObject,
            //         civitaiModelID, civitaiVersionID, civitaiUrl, (String) modelVersionObject.get("baseModel"),
            //         emptyList.toArray(new String[0]), "N/A", emptyList);

            System.err.println("An error occurred while downloading " + civitaiModelID + "_" + civitaiVersionID + "_"
                    + civitaiFileName + "\n" + ex.getMessage());
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }
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
        Long quantity = fileService.checkQuantityOfOfflineDownloadList(modelID);

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
        Optional<List<String>> versionExistenceMapOptional = fileService.getCivitaiVersionIds(modelNumber);

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

    // @CrossOrigin(origins = "https://civitai.com")
    // @PostMapping(path = "/check-if-modelId-and-versionId-exist-in-database-tampermonkey")
    // @SuppressWarnings("unchecked")
    // public ResponseEntity<CustomResponse<Map<String, Boolean>>> checkIfModelIdandVersionIdExistInDatabase(
    //         @RequestBody Map<String, Object> requestBody) {
    //     String url = (String) requestBody.get("url");

    //     // Validate null or empty
    //     if (url == null || url == "") {
    //         return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
    //     }
    //     Boolean isSaved = civitaiSQL_Service.find_one_from_models_urls_table(url);
    //     if (isSaved != null) {
    //         Map<String, Boolean> payload = new HashMap<>();
    //         payload.put("isSaved", isSaved);

    //         return ResponseEntity.ok().body(CustomResponse.success("Model retrieval successful", payload));
    //     } else {
    //         return ResponseEntity.ok().body(CustomResponse.failure("Model not found in the Database"));
    //     }
    // }

    @CrossOrigin(origins = "*")
    @PostMapping(path = "/search_offline_downloads")
    public ResponseEntity<CustomResponse<Map<String, Object>>> searchOfflineDownloads(
            @RequestBody Map<String, Object> requestBody) {

        List<String> keywords = (List<String>) requestBody.get("keywords");

        try {
            // Call the service method and get the matching entries
            List<Map<String, Object>> filteredList = fileService.searchOfflineDownloads(keywords);

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

}
