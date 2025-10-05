package com.civitai.server.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.swing.text.html.Option;

import org.springframework.lang.Nullable;

import com.civitai.server.models.dto.FullModelRecordDTO;
import com.civitai.server.models.dto.Models_DTO;
import com.civitai.server.models.dto.PageResponse;
import com.civitai.server.models.dto.Tables_DTO;
import com.civitai.server.models.dto.TagCountDTO;
import com.civitai.server.models.dto.TopTagsRequest;
import com.civitai.server.models.entities.civitaiSQL.Models_Offline_Table_Entity;
import com.civitai.server.models.entities.civitaiSQL.Models_Table_Entity;
import com.civitai.server.models.entities.civitaiSQL.Models_Urls_Table_Entity;
import com.civitai.server.models.entities.civitaiSQL.Recycle_Table_Entity;
import com.civitai.server.models.entities.civitaiSQL.VisitedPath_Table_Entity;

public interface CivitaiSQL_Service {
        // Single Tables only //
        Optional<List<Models_Table_Entity>> find_all_from_models_table();

        Optional<Models_Table_Entity> find_one_from_models_table(Integer id);

        Boolean find_one_from_models_urls_table(String url);

        Long find_quantity_from_models_urls_table(String url);

        Long find_quantity_from_models_table(String modelID);

        Optional<List<String>> find_all_categories();

        Optional<List<String>> find_version_numbers_for_model(String modelNumber, List<String> versionNumbers);

        // All Tables only //
        Optional<List<Tables_DTO>> find_all_from_all_tables();

        Optional<Tables_DTO> find_one_tables_DTO_from_all_tables(Integer id);

        Optional<Models_DTO> find_one_models_DTO_from_all_tables_by_id(Integer id);

        Optional<Models_DTO> find_one_models_DTO_from_all_tables_by_url(String url);

        Optional<List<Models_DTO>> find_List_of_models_DTO_from_all_tables_by_modelID(String modelID);

        Optional<List<Models_DTO>> find_List_of_models_DTO_from_all_tables_by_alike_url(String name);

        Optional<List<Models_DTO>> find_List_of_models_DTO_from_all_tables_by_alike_tags(String name);

        Optional<List<Models_DTO>> find_List_of_models_DTO_from_all_tables_by_alike_triggerWords(String name);

        Optional<List<Models_DTO>> find_List_of_models_DTO_from_all_tables_by_alike_name(String name);

        Optional<List<Models_DTO>> find_List_of_models_DTO_from_all_tables_by_alike_tagsList(List<String> tagsList);

        Optional<Map<String, List<Models_DTO>>> find_lastest_three_models_DTO_in_each_category_from_all_table();

        Optional<List<String>> find_List_of_Version_Number_from_model_tables_by_Url(String modelID);

        Optional<Models_Table_Entity> find_one_from_models_table_by_model_number_and_version_number(String modelNumber,
                        String versionNumber);

        // Transcation Actions
        // Add a record to all tables
        void create_record_to_all_tables(Models_DTO dto);

        // Update a record to all tables
        void update_record_to_all_tables_by_id(Models_DTO dto, Integer id);

        void delete_record_to_all_table_by_id(Integer id);

        // Add a dummy record to all table
        void create_dummy_record_to_all_tables();

        // Delete latest row from all tables
        void delete_latest_record_from_all_tables();

        // Delete every row from all tables
        void delete_every_record_from_all_tables();

        // Utils
        Models_DTO convertToDTO(Models_Table_Entity entity);

        Optional<Models_DTO> create_models_DTO_by_Url(String url, String category, String downloadFilePath);

        Optional<String> findFirstImageUrlByModelNumberAndVersionNumber(String modelNumber,
                        String versionNumber);

        int updateLocalPath(List<Map<String, Object>> fileArray, String localPath);

        public Optional<List<Models_DTO>> findListOfModelsDTOByModelAndVersion(
                        List<Map<String, String>> compositeList);

        public void update_record_to_all_tables_by_model_and_version(Models_DTO dto, Integer id,
                        List<String> fieldsToUpdate);

        public Optional<Models_Table_Entity> find_one_from_models_table_by_model_and_version(String modelNumber,
                        String versionNumber);

        public Optional<List<Map<String, Object>>> findVirtualFiles(String path);

        // interface
        PageResponse<Map<String, Object>> findVirtualFilesPaged(
                        String path, int page, int size, String sortKey, String sortDir, String q);

        public List<Map<String, Object>> findVirtualDirectoriesWithDrive(String path, String sortKey, String sortDir);

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

        public void remove_from_offline_download_list(String civitaiModelID, String civitaiVersionID);

        public long checkQuantityOfOfflineDownloadList(String civitaiModelID);

        public List<Map<String, Object>> get_offline_download_list();

        public List<Map<String, Object>> searchOfflineDownloads(List<String> keywords);

        public Optional<List<String>> getCivitaiVersionIds(String civitaiModelID);

        public List<String> get_error_model_list();

        public void update_error_model_offline_list(String civitaiModelID, String civitaiVersionID, Boolean isError);

        public List<Map<String, Object>> get_creator_url_list();

        public void update_creator_url_list(String creatorUrl, String status, Boolean lastChecked, String rating);

        public void remove_creator_url(String creatorUrl);

        public void pathVisited(String path, String parentPath, String drive);

        public List<VisitedPath_Table_Entity> getChildren(String parentPath);

        Optional<FullModelRecordDTO> findFullByModelAndVersion(String modelNumber, String versionNumber);

        public Recycle_Table_Entity add_to_recycle(Recycle_Table_Entity e);

        public boolean delete_from_recycle(String id);

        public List<Recycle_Table_Entity> fetch_recycle();

        Optional<Models_Offline_Table_Entity> getOfflineRecordByModelAndVersion(String civitaiModelID,
                        String civitaiVersionID);

        Models_Table_Entity updateMyRatingByModelAndVersion(String modelNumber, String versionNumber, int rating);

        /**
         * Apply only non-null fields/sections; locate by dto.model.modelNumber +
         * dto.model.versionNumber.
         * Returns refreshed record after update.
         */
        FullModelRecordDTO updateFullByModelAndVersion(FullModelRecordDTO incoming);

        public PageResponse<Map<String, Object>> get_offline_download_list_paged(
                        int page, int size, boolean filterEmptyBaseModel, List<String> prefixes,
                        String search, String op, String status);

        PageResponse<TagCountDTO> get_top_tags_page(TopTagsRequest req);

}
