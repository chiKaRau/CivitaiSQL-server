package com.civitai.server.repositories.civitaiSQL;

import java.util.List;

import com.civitai.server.models.entities.civitaiSQL.Models_Table_Entity;

public interface CustomModelsTableRepository {
    int updateLocalPathByPairsDynamic(List<Object[]> pairs, String localPath);

    List<Models_Table_Entity> findByModelNumberAndVersionNumberIn(List<Object[]> pairs);

}
