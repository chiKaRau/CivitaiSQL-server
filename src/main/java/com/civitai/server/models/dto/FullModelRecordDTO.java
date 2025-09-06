package com.civitai.server.models.dto;

import com.civitai.server.models.entities.civitaiSQL.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FullModelRecordDTO {
    private Models_Table_Entity model;
    private Models_Descriptions_Table_Entity description;
    private Models_Urls_Table_Entity url;
    private Models_Details_Table_Entity details;
    private Models_Images_Table_Entity images;
}