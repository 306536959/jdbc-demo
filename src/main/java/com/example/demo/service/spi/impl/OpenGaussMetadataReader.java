package com.example.demo.service.spi.impl;

import com.example.demo.service.spi.DatabaseMetadataReader;
import java.sql.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenGaussMetadataReader implements DatabaseMetadataReader {
    private static final Logger log = LoggerFactory.getLogger(OpenGaussMetadataReader.class);
    private static final String[] TABLE_TYPES = {"TABLE"};

    @Override
    public boolean supports(String databaseProductName) {
        return databaseProductName.toLowerCase().contains("opengauss");
    }

    @Override
    public String extractCatalog(String jdbcUrl) throws SQLException {
        int lastSlash = jdbcUrl.lastIndexOf("/");
        if (lastSlash != -1) {
            int paramIndex = jdbcUrl.indexOf("?", lastSlash);
            if (paramIndex == -1) paramIndex = jdbcUrl.length();
            return jdbcUrl.substring(lastSlash + 1, paramIndex);
        }
        throw new SQLException("无法从openGauss JDBC URL提取数据库名");
    }

    @Override
    public Map<String, Object> readMetadata(DatabaseMetaData metaData, String catalog) throws SQLException {
        try {
            Class.forName("org.opengauss.Driver");
            log.info("OpenGauss 驱动已加载");
        } catch (ClassNotFoundException e) {
            log.error("OpenGauss 驱动未找到", e);
            throw new SQLException("OpenGauss 驱动未找到", e);
        }
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