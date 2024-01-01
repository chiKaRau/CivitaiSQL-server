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

    //TODO
    //Docker mySQL is mounting at ongoing folder,
    //Reorganzie folder and find new place for mounting
    @PostMapping("/find-civitaiModel-info-by-modelID")
    public ResponseEntity<CustomResponse<Map<String, Map<String, Object>>>> findCivitaiModelInfo(
            @RequestBody Map<String, Object> requestBody) {

        String modelID = (String) requestBody.get("modelID");

        Optional<Map<String, Object>> modelOptional = civitai_Service.findModelByModelID(modelID);

        if (modelOptional.isPresent()) {
            Map<String, Object> model = modelOptional.get();

            Map<String, Map<String, Object>> payload = new HashMap<>();
            payload.put("model", model);

            return ResponseEntity.ok()
                    .body(CustomResponse.success("Civitai Info retrieval successful", payload));
        } else {
            return ResponseEntity.ok().body(CustomResponse.failure("Model not found in the Civitai"));
        }
    }

    //TODO add payload
    @PostMapping("/get-tags-list")
    public ResponseEntity<CustomResponse<Map<String, List<String>>>> getTagsList(
            @RequestBody Map<String, Object> requestBody) {

        String modelID = (String) requestBody.get("modelID");

        Optional<List<String>> tagsOptional = civitai_Service.findModelTagsByModelID(modelID);
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
