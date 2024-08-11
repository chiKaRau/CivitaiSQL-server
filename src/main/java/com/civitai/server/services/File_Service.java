package com.civitai.server.services;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public interface File_Service {
    public List<String> get_folders_list();

    public void update_folder_list(String downloadFilePath);

    public void empty_cart_list();

    public Boolean check_cart_list(String url);

    public void update_cart_list(String url);

    public void update_must_add_list(String url);

    public void update_error_model_list(String url);

    public void open_download_directory();

    public void download_file_by_server(String loraFileName, String modelID, String versionID, String downloadFilePath,
            List<Map<String, Object>> name_and_downloadUrl_Array, String loraURL);
}
