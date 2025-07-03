package com.example.demo.service.spi.impl;

import com.example.demo.service.spi.DatabaseMetadataReader;
import java.sql.*;
import java.util.*;

public class DmMetadataReader implements DatabaseMetadataReader {
    private static final String[] TABLE_TYPES = {"TABLE"};

    @Override
    public boolean supports(String databaseProductName) {
        return databaseProductName.toLowerCase().contains("dm");
    }

    @Override
    public String extractCatalog(String jdbcUrl) throws SQLException {
        int lastSlash = jdbcUrl.lastIndexOf("/");
        if (lastSlash != -1) {
            int paramIndex = jdbcUrl.indexOf("?", lastSlash);
            if (paramIndex == -1) paramIndex = jdbcUrl.length();
            return jdbcUrl.substring(lastSlash + 1, paramIndex);
        }
        throw new SQLException("无法从达梦JDBC URL提取数据库名");
    }

    @Override
    public Map<String, Object> readMetadata(DatabaseMetaData metaData, String catalog) throws SQLException {
        Map<String, Object> metadata = new HashMap<>();
        List<Map<String, Object>> tables = new ArrayList<>();
        try (ResultSet rs = metaData.getTables(catalog, null, "%", TABLE_TYPES)) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                Map<String, Object> table = new HashMap<>();
                table.put("name", tableName);
                table.put("columns", getColumns(metaData, catalog, tableName));
                tables.add(table);
            }
        }
        metadata.put("tables", tables);
        metadata.put("databaseName", catalog);
        metadata.put("driverName", metaData.getDriverName());
        metadata.put("driverVersion", metaData.getDriverVersion());
        metadata.put("databaseProductName", metaData.getDatabaseProductName());
        metadata.put("databaseProductVersion", metaData.getDatabaseProductVersion());
        metadata.put("url", metaData.getURL());
        metadata.put("userName", metaData.getUserName());
        return metadata;
    }

    private List<Map<String, Object>> getColumns(DatabaseMetaData metaData, String catalog, String tableName) throws SQLException {
        List<Map<String, Object>> columns = new ArrayList<>();
        try (ResultSet rs = metaData.getColumns(catalog, null, tableName, "%")) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                Map<String, Object> column = new HashMap<>();
                column.put("name", columnName);
                columns.add(column);
            }
        }
        return columns;
    }
} 