package com.civitai.server.repositories.civitaiSQL;

import com.civitai.server.models.entities.civitaiSQL.Category_Prefixes_Table_Entity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface Category_Prefixes_Table_Repository extends JpaRepository<Category_Prefixes_Table_Entity, Long> {

    Optional<Category_Prefixes_Table_Entity> findByPrefixName(String prefixName);

    List<Category_Prefixes_Table_Entity> findAllByOrderByDownloadPriorityAscPrefixNameAsc();

    List<Category_Prefixes_Table_Entity> findAllByOrderByIdAsc();

}