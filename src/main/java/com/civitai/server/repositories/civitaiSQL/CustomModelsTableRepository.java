package com.civitai.server.repositories.civitaiSQL;

import java.util.List;

public interface CustomModelsTableRepository {
    int updateLocalPathByPairsDynamic(List<Object[]> pairs, String localPath);
}
