package com.civitai.server.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.civitai.server.services.File_Service;
import com.civitai.server.utils.CustomResponse;

@RestController
@RequestMapping("/api")
public class File_Controller {

    @Autowired
    private File_Service fileService;

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
        return ResponseEntity.ok().body(CustomResponse.success("Success download file"));
    }

}
