package com.civitaiSQL.server;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.civitai.server.services.CivitaiSQL_Service;
import com.civitai.server.services.impl.CivitaiSQL_Service_Impl;

@SpringBootTest
public class CivitaiSQL_Service_Test {

    private CivitaiSQL_Service civitaiSQL_Service;

    private static final Logger log = LoggerFactory.getLogger(CivitaiSQL_Service_Impl.class);

    @Autowired
    public CivitaiSQL_Service_Test(CivitaiSQL_Service civitaiSQL_Service) {
        this.civitaiSQL_Service = civitaiSQL_Service;
    }

    @Test
    void test_find_one_models_DTO_from_all_tables() {
        log.info("{}", civitaiSQL_Service.find_one_models_DTO_from_all_tables(24));
    }

}
