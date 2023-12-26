package com.civitai.server;

import org.junit.jupiter.api.Test;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders; // Add this import

import com.civitai.server.controllers.CivitaiSQL_Controller;
import com.civitai.server.models.entities.civitaiSQL.Models_Table_Entity;
import com.civitai.server.services.CivitaiSQL_Service;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.sql.Timestamp;
import java.util.Optional;

@WebMvcTest(CivitaiSQL_Controller.class)
public class CivitaiSQL_Controller_Test {

        /*
        @Autowired
        private MockMvc mockMvc;
        
        @MockBean
        private CivitaiSQL_Service civitaiSQL_Service;
        
        //private static final Logger log = LoggerFactory.getLogger(CivitaiSQL_Service_Impl.class);
        
        @Test
        void testFindTesting() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/testing"))
                .andExpect(status().isOk())
                .andExpect(content().string("Testing now"));
        }
        
        @Test
        void testFindAllFromModelsTable() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/models-table-entities"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        // Add more assertions based on your response
        }
        
        @Test
        void testFindOneFromModelsTable() throws Exception {
        
        //{"success":true,"message":"Model retrieval successful",
        //"data":{"id":1,"name":"Model1","tags":"[\"tag1\", \"tag2\"]","category":"Anime","versionNumber":"1.0","modelNumber":"M123",
        //"triggerWords":"[\"tag1\", \"tag2\"]","nsfw":false,"flag":false,"createdAt":"2023-12-21T11:33:58.000+00:00","updatedAt":"2023-12-24T04:14:31.000+00:00"}}
        
        // Assuming you have a constructor that takes all fields as parameters
        Models_Table_Entity mockEntity = new Models_Table_Entity(
                1, "Model1", "[\"tag1\", \"tag2\"]", "Category1",
                "1.0", "M123", "[\"tag1\", \"tag2\"]", false, false,
                new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()));
        
        when(civitaiSQL_Service.find_one_from_models_table(1))
                .thenReturn(Optional.of(mockEntity));
        
        mockMvc.perform(MockMvcRequestBuilders.get("/api/models-table-entity/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Model1"))
                .andExpect(jsonPath("$.data.tags").value("[\"tag1\", \"tag2\"]"))
                .andExpect(jsonPath("$.data.category").value("Category1"))
                .andExpect(jsonPath("$.data.versionNumber").value("1.0"))
                .andExpect(jsonPath("$.data.modelNumber").value("M123"))
                .andExpect(jsonPath("$.data.triggerWords").value("[\"tag1\", \"tag2\"]"))
                .andExpect(jsonPath("$.data.nsfw").value(false))
                .andExpect(jsonPath("$.data.flag").value(false));
        // Add more assertions based on your response
        }
        */
}
