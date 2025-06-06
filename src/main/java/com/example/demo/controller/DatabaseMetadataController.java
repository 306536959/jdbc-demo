package com.example.demo.controller;

import com.example.demo.service.DatabaseMetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DatabaseMetadataController {

    @Autowired
    private DatabaseMetadataService databaseMetadataService;

    @GetMapping("/api/database/metadata")
    public Map<String, Object> getDatabaseMetadata(@RequestParam String strategyId) {
        return databaseMetadataService.getDatabaseMetadata(strategyId);
    }
}