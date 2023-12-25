package com.civitai.server.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface Civitai_Service {

    String CIVITAI_MODELS_ENDPOINT = "https://civitai.com/api/v1/models/";

    Optional<Map<String, Object>> findModelByModelID(String modelID);

    Optional<List<String>> findModelTagsByModelID(String modelID);

}