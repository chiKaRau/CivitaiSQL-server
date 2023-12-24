package com.civitai.server.controllers;

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

@RestController
@RequestMapping("/api")
public class File_Controller {

    @Autowired
    private File_Service fileService;

    @GetMapping("/open-download-directory")
    public ResponseEntity<Void> openDownloadDirectory() {
        fileService.open_download_directory();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/clear-cart-list")
    public ResponseEntity<Void> clearCartList() {
        fileService.empty_cart_list();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/check-cart-list")
    public ResponseEntity<?> checkCartList(@RequestBody Map<String, Object> requestBody) {
        String url = (String) requestBody.get("loraURL");
        Boolean isCarted = fileService.check_cart_list(url);
        return ResponseEntity.ok(isCarted);
    }

    @GetMapping("/list-autocomplete")
    public ResponseEntity<List<String>> listAutoComplete() {
        List<String> folders_list = fileService.get_folder_list();
        return ResponseEntity.ok(folders_list);
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/download-file-server")
    public ResponseEntity<Void> downloadFileServer(@RequestBody Map<String, Object> requestBody) {
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
        String modelID = (String) requestBody.get("modelID");
        String loraFileName = ((String) requestBody.get("loraFileName")).split("\\.")[0];
        String versionID = (String) requestBody.get("versionID");
        String downloadFilePath = (String) requestBody.get("downloadFilePath");
        List<Map<String, Object>> name_and_downloadUrl_Array = (List<Map<String, Object>>) requestBody
                .get("name_and_downloadUrl_Array");
        String loraURL = (String) requestBody.get("loraURL");
        fileService.download_file_by_server(loraFileName, modelID, versionID, downloadFilePath,
                name_and_downloadUrl_Array, loraURL);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/download-file-browser")
    public ResponseEntity<Void> downloadFileBrowser(@RequestBody Map<String, Object> requestBody) {
        String downloadFilePath = (String) requestBody.get("downloadFilePath");
        String loraURL = (String) requestBody.get("loraURL");
        fileService.update_folder_list(downloadFilePath);
        fileService.update_cart_list(loraURL);
        return ResponseEntity.ok().build();
    }

    //TODO
    //multi model download
}
