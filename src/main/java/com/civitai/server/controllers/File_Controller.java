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

    @PostMapping("/open-model-download-directory")
    public ResponseEntity<CustomResponse<String>> openModelDownloadDirectory(
            @RequestBody Map<String, Object> requestBody) {
        String modelDownloadPath = (String) requestBody.get("modelDownloadPath");
        fileService.open_model_downloaded_directory(modelDownloadPath);
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
        // combinations.forEach(c -> System.out.println(" " + c));
        // ---------------------

        // 3) Filter: keep any combo where at least one element is in pendingRemove
        // 3) Filter: **exclude** any combo that contains at least one pending-remove
        // tag
        List<List<String>> matched = combinations.stream()
                .filter(combo -> combo.stream()
                        .map(String::toLowerCase)
                        .noneMatch(removeLower::contains) // <-- reversed here
                )
                .distinct()
                .collect(Collectors.toList());

        // --- DEBUG LOGGING ---
        // System.out.println("Matched combinations (" + matched.size() + "):");
        // matched.forEach(c -> System.out.println(" " + c));
        // ---------------------

        Map<String, List<List<String>>> payload = Collections.singletonMap("matchedCombinations", matched);
        return ResponseEntity.ok(CustomResponse.success("Filtered combinations retrieved", payload));
    }

    // @GetMapping("/get_tags_list")
    // public ResponseEntity<CustomResponse<Map<String, List<Map<String, Object>>>>>
    // getTagsList(
    // @RequestParam(value = "prefix", required = false) String prefix) {
    // // Retrieve both top 10 and recent 10 lists based on prefix
    // Map<String, List<Map<String, Object>>> tagsMap =
    // fileService.get_tags_list(prefix);

    // // Check if both lists are empty
    // if (!tagsMap.get("topTags").isEmpty() ||
    // !tagsMap.get("recentTags").isEmpty()) {
    // return ResponseEntity.ok().body(CustomResponse.success("TagsList retrieval
    // successful", tagsMap));
    // } else {
    // return ResponseEntity.ok().body(CustomResponse.failure("No tags found in the
    // database"));
    // }
    // }

    @SuppressWarnings("unchecked")
    @PostMapping("/download-file-server")
    public ResponseEntity<CustomResponse<String>> downloadFileServer(@RequestBody Map<String, Object> requestBody) {
        /*
         * {
         * "modelID": "123",
         * "loraFileName": "example.txt",
         * "versionID": "1.0",
         * "downloadFilePath": "/downloads",
         * "nameAndDownloadUrlArray": [
         * {"name": "file1", "downloadUrl": "https://example.com/file1"},
         * {"name": "file2", "downloadUrl": "https://example.com/file2"}
         * ],
         * "loraURL": "https://example.com/lora"
         * }
         */
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
        civitaiSQL_Service.update_download_file_path_count(downloadFilePath);
        // fileService.remove_from_offline_download_list(civitaiModelID,
        // civitaiVersionID);
        return ResponseEntity.ok().body(CustomResponse.success("Success download file"));
    }

    /**
     * Endpoint to backup the offline_download_list.json file.
     *
     * @return ResponseEntity with isBackedUp flag.
     */
    @PostMapping("/backup_offline_download_list")
    public void backupOfflineDownloadList() {
        // boolean isBackedUp = fileService.backupOfflineDownloadList();

        // Map<String, Boolean> payload = new HashMap<>();
        // payload.put("isBackedUp", isBackedUp);
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

            civitaiSQL_Service.update_error_model_offline_list(civitaiModelID, civitaiVersionID, true);
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

                civitaiSQL_Service.insert_model_offline_download_history(
                        Long.valueOf(civitaiModelID),
                        Long.valueOf(civitaiVersionID),
                        Arrays.asList(imageUrlsArray));

                civitaiSQL_Service.update_download_file_path_count(downloadFilePath);
                // fileService.remove_from_offline_download_list(civitaiModelID,
                // civitaiVersionID);
                civitaiSQL_Service.remove_from_offline_download_list(civitaiModelID, civitaiVersionID);
                return ResponseEntity.ok().body(CustomResponse.success("Success download file"));
            }

        } catch (Exception ex) {
            String modelID = civitaiModelID, versionID = civitaiVersionID, url = civitaiUrl,
                    name = civitaiFileName.split("\\.")[0].trim();
            String modelName = modelID + "_" + versionID + "_" + name;

            // Map<String, Object> modelVersionObject = modelVersionOptional.get();

            civitaiSQL_Service.update_error_model_offline_list(civitaiModelID, civitaiVersionID, true);
            // List<String> emptyList = new ArrayList<>();
            // fileService.update_error_model_list_v2(civitaiFileName, civitaiModelFileList,
            // downloadFilePath, modelObject,
            // civitaiModelID, civitaiVersionID, civitaiUrl, (String)
            // modelVersionObject.get("baseModel"),
            // emptyList.toArray(new String[0]), "N/A", emptyList);

            System.err.println("An error occurred while downloading " + civitaiModelID + "_" + civitaiVersionID + "_"
                    + civitaiFileName + "\n" + ex.getMessage());
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/download-file-server-v2-for-custom")
    public ResponseEntity<CustomResponse<String>> downloadFileServerV2forCustom(
            @RequestBody Map<String, Object> requestBody) {

        // 1) Pull modelObject off the request
        Map<String, Object> modelObject = (Map<String, Object>) requestBody.get("modelObject");
        String civitaiFileName = (String) modelObject.get("civitaiFileName");
        String downloadFilePath = (String) modelObject.get("downloadFilePath");
        String civitaiUrl = (String) modelObject.get("civitaiUrl");
        String civitaiModelID = (String) modelObject.get("civitaiModelID");
        String civitaiVersionID = (String) modelObject.get("civitaiVersionID");
        String baseModel = (String) modelObject.get("baseModel");

        // 2) Front end now passes an array of URL strings here:
        @SuppressWarnings("unchecked")
        List<String> imageUrlsList = (List<String>) modelObject.get("imageUrls");

        // 3) And your single file’s download URL lives here:
        String downloadUrl = (String) modelObject.get("downloadUrl");

        // 4) Validate
        if (downloadFilePath == null || downloadFilePath.isEmpty()
                || civitaiModelID == null || civitaiModelID.isEmpty()
                || civitaiVersionID == null || civitaiVersionID.isEmpty()
                || baseModel == null || baseModel.isEmpty()
                || downloadUrl == null || downloadUrl.isEmpty()
                || imageUrlsList == null || imageUrlsList.isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body(CustomResponse.failure("Invalid input"));
        }

        try {
            // 5) Build the one‐entry download list
            List<Map<String, Object>> civitaiModelFileList = new ArrayList<>();
            Map<String, Object> fileEntry = new HashMap<>();
            fileEntry.put("name", civitaiFileName);
            fileEntry.put("downloadUrl", downloadUrl);
            civitaiModelFileList.add(fileEntry);

            // 6) Convert the List<String> into a String[] for previews
            String[] imageUrlsArray = imageUrlsList.toArray(new String[0]);

            // 7) Call your existing v2 download
            fileService.download_file_by_server_v2(
                    civitaiFileName,
                    civitaiModelFileList,
                    downloadFilePath,
                    null, // skip modelVersionObject
                    civitaiModelID,
                    civitaiVersionID,
                    civitaiUrl,
                    baseModel,
                    imageUrlsArray);

            /*
             * civitaiSQL_Service.insert_model_offline_download_history(
             * Long.valueOf(civitaiModelID),
             * Long.valueOf(civitaiVersionID),
             * Arrays.asList(imageUrlsArray));
             */

            return ResponseEntity.ok(CustomResponse.success("Success download file"));
        } catch (Exception ex) {
            String modelName = civitaiModelID + "_" + civitaiVersionID + "_" + civitaiFileName;
            civitaiSQL_Service.update_error_model_offline_list(civitaiModelID, civitaiVersionID, true);
            ex.printStackTrace();
            return ResponseEntity
                    .status(500)
                    .body(CustomResponse.failure("Server error: " + ex.getMessage()));
        }
    }

    @PostMapping("/check-model-version-file-exists")
    public ResponseEntity<CustomResponse<Map<String, Boolean>>> checkModelVersionFileExists(
            @RequestBody Map<String, Object> requestBody) {

        String modelID = (String) requestBody.get("modelID");
        String versionID = (String) requestBody.get("versionID");

        if (modelID == null || modelID.trim().isEmpty() ||
                versionID == null || versionID.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        Boolean exists = fileService.check_model_version_file_exists(modelID, versionID);

        Map<String, Boolean> payload = new HashMap<>();
        payload.put("exists", exists);

        return ResponseEntity.ok().body(CustomResponse.success("File existence check successful", payload));
    }

    @PostMapping("/move-model-version-files-to-delete")
    public ResponseEntity<CustomResponse<Map<String, Object>>> moveModelVersionFilesToDelete(
            @RequestBody Map<String, Object> requestBody) {

        String modelID = (String) requestBody.get("modelID");
        String versionID = (String) requestBody.get("versionID");

        if (modelID == null || modelID.trim().isEmpty()
                || versionID == null || versionID.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        int movedCount = fileService.move_model_version_files_to_delete(modelID, versionID);

        Map<String, Object> payload = new HashMap<>();
        payload.put("isMoved", movedCount > 0);
        payload.put("movedCount", movedCount);

        return ResponseEntity.ok().body(
                CustomResponse.success("Move to delete folder completed", payload));
    }

}
