package com.civitai.server.controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

@RestController
@RequestMapping("/api")
public class File_Controller {

    private File_Service fileService;
    private Civitai_Service civitai_Service;

    @Autowired
    public File_Controller(File_Service fileService, Civitai_Service civitai_Service) {
        this.fileService = fileService;
        this.civitai_Service = civitai_Service;
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

    @SuppressWarnings("unchecked")
    @PostMapping("/add-offline-download-file-into-offline-download-list")
    public ResponseEntity<CustomResponse<String>> addOfflineDownloadFileIntoOfflineDownloadList(
            @RequestBody Map<String, Object> requestBody) {

        Map<String, Object> modelObject = (Map<String, Object>) requestBody.get("modelObject");
        String civitaiFileName = ((String) modelObject.get("civitaiFileName"));
        List<Map<String, Object>> civitaiModelFileList = (List<Map<String, Object>>) modelObject
                .get("civitaiModelFileList");
        String downloadFilePath = (String) modelObject.get("downloadFilePath");
        String civitaiUrl = (String) modelObject.get("civitaiUrl");
        String civitaiModelID = (String) modelObject.get("civitaiModelID");
        String civitaiVersionID = (String) modelObject.get("civitaiVersionID");
        String selectedCategory = (String) modelObject.get("selectedCategory");
        Boolean isModifyMode = (Boolean) requestBody.get("isModifyMode");

        // Validate null or empty
        if (modelObject == null ||
                civitaiUrl == null || civitaiUrl == "" ||
                downloadFilePath == null || downloadFilePath == "" ||
                civitaiUrl == null || civitaiUrl == "" ||
                civitaiModelID == null || civitaiModelID == "" ||
                civitaiVersionID == null || civitaiVersionID == "" ||
                selectedCategory == null || selectedCategory == "" ||
                civitaiModelFileList == null || civitaiModelFileList.isEmpty()) {
            return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
        }

        try {

            Optional<Map<String, Object>> modelVersionOptional = civitai_Service.findModelByVersionID(civitaiVersionID);

            if (!modelVersionOptional.isPresent()) {
                return ResponseEntity.badRequest().body(CustomResponse.failure("Invalid input"));
            } else {
                Map<String, Object> modelVersionObject = modelVersionOptional.get();

                // Extract URLs
                String[] imageUrlsArray = JsonUtils.extractImageUrls(modelVersionObject);

                fileService.update_offline_download_list(civitaiFileName, civitaiModelFileList, downloadFilePath,
                        modelVersionObject, civitaiModelID, civitaiVersionID, civitaiUrl,
                        (String) modelVersionObject.get("baseModel"), imageUrlsArray, selectedCategory, isModifyMode);

                return ResponseEntity.ok().body(CustomResponse.success("Success download file"));
            }

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

        try {

            Optional<Map<String, Object>> modelVersionOptional = civitai_Service.findModelByVersionID(civitaiVersionID);

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
            System.err.println("An error occurred: " + ex.getMessage());
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

}
