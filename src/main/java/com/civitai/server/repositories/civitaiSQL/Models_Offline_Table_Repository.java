package com.civitai.server.repositories.civitaiSQL;

import com.civitai.server.models.entities.civitaiSQL.Models_Offline_Table_Entity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface Models_Offline_Table_Repository
                extends JpaRepository<Models_Offline_Table_Entity, Long> {

        Optional<Models_Offline_Table_Entity> findFirstByCivitaiModelIDAndCivitaiVersionID(
                        Long civitaiModelID, Long civitaiVersionID);

        // deletes all matches; returns number of rows removed
        long deleteByCivitaiModelIDAndCivitaiVersionID(Long civitaiModelID, Long civitaiVersionID);

        long countByCivitaiModelID(Long civitaiModelID);

        @Query("select e.civitaiVersionID " +
                        "from Models_Offline_Table_Entity e " +
                        "where e.civitaiModelID = :modelId and e.civitaiVersionID is not null")
        List<Long> findVersionIdsByModelId(@Param("modelId") Long modelId);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("update Models_Offline_Table_Entity e " +
                        "set e.isError = :isError " +
                        "where e.civitaiModelID = :modelId and e.civitaiVersionID = :versionId")
        int updateIsErrorByModelAndVersion(@Param("modelId") Long modelId,
                        @Param("versionId") Long versionId,
                        @Param("isError") Boolean isError);

        // assumes your entity field is named "id" and mapped to column `_id`
        List<Models_Offline_Table_Entity> findAllByIsErrorTrueOrderByIdAsc();

}