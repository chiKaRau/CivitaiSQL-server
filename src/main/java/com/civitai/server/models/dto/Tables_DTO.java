package com.civitai.server.models.dto;

import com.civitai.server.models.entities.civitaiSQL.Models_Descriptions_Table_Entity;
import com.civitai.server.models.entities.civitaiSQL.Models_Details_Table_Entity;
import com.civitai.server.models.entities.civitaiSQL.Models_Images_Table_Entity;
import com.civitai.server.models.entities.civitaiSQL.Models_Table_Entity;
import com.civitai.server.models.entities.civitaiSQL.Models_Urls_Table_Entity;

import lombok.Data;

@Data
public class Tables_DTO {
    private Models_Table_Entity models_Table_Entitiy;
    private Models_Descriptions_Table_Entity models_Descriptions_Table_Entity;
    private Models_Urls_Table_Entity models_Urls_Table_Entity;
    private Models_Details_Table_Entity models_Details_Table_Entity;
    private Models_Images_Table_Entity models_Images_Table_Entity;
}