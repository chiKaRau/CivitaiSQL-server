package com.civitai.server.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.civitai.server.services.Civitai_Service;
import com.civitai.server.utils.CustomResponse;

@RestController
@RequestMapping("/api")
public class Civitai_Controller {

    private Civitai_Service civitai_Service;

    @Autowired
    public Civitai_Controller(Civitai_Service civitai_Service) {
        this.civitai_Service = civitai_Service;
    }

    @PostMapping("/get-tags-list")
    public ResponseEntity<CustomResponse<Map<String, List<String>>>> getTagsList(
            @RequestBody Map<String, Object> requestBody) {

        String modelID = (String) requestBody.get("modelID");

        Optional<List<String>> tagsOptional = civitai_Service.findTagsByModelID(modelID);
        if (tagsOptional.isPresent()) {
            List<String> tags = tagsOptional.get();

            Map<String, List<String>> data = new HashMap<>();
            data.put("tags", tags);

            return ResponseEntity.ok()
                    .body(CustomResponse.success("tags list retrieval successful", data));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

}
