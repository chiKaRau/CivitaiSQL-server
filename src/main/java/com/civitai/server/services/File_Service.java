package com.civitai.server.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public interface File_Service {
        public List<String> get_folders_list();

        public List<String> get_error_model_list();

        public void remove_from_error_model_list(String ModelID, String versionID);

        public Map<String, List<Map<String, Object>>> get_tags_list(String prefix);

        public void update_offline_download_list(
                        String civitaiFileName,
                        List<Map<String, Object>> civitaiModelFileList,
                        String downloadFilePath,
                        Map<String, Object> modelVersionObject,
                        String civitaiModelID,
                        String civitaiVersionID,
                        String civitaiUrl,
                        String civitaiBaseModel,
                        String[] imageUrlsArray,
                        String selectedCategory,
                        List<String> civitaiTags,
                        Boolean isModifyMode);

        public boolean backupOfflineDownloadList();

        public void remove_from_offline_download_list(String civitaiModelID, String civitaiVersionID);

        public long checkQuantityOfOfflineDownloadList(String civitaiModelID);

        public void update_folder_list(String downloadFilePath);

        public Optional<List<String>> getCivitaiVersionIds(String civitaiModelID);

        public void empty_cart_list();

        public Boolean check_cart_list(String url);

        public void update_cart_list(String url);

        public void update_must_add_list(String url);

        public void update_error_model_list(String url);

        public void open_download_directory();

        public void create_tags_list();

        public List<Map<String, Object>> get_offline_download_list();

        public void update_tags_list(String inputTag);

        public List<Map<String, String>> get_categories_prefix_list();

        public List<Map<String, String>> get_filePath_categories_list();

        public void download_file_by_server(String loraFileName, String modelID, String versionID,
                        String downloadFilePath,
                        List<Map<String, Object>> name_and_downloadUrl_Array, String loraURL);

        public void download_file_by_server_v2(String civitaiFileName, List<Map<String, Object>> civitaiModelFileList,
                        String downloadFilePath, Map<String, Object> modelVersionObject, String civitaiModelID,
                        String civitaiVersionID, String civitaiUrl, String civitaiBaseModel, String[] imageUrlsArray);

        public List<Map<String, Object>> searchOfflineDownloads(List<String> keywords);
}
