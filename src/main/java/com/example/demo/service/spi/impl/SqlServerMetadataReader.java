package com.example.demo.service.spi.impl;

import com.example.demo.service.spi.DatabaseMetadataReader;

import java.sql.*;
import java.util.*;
import java.util.TimeZone;

public class SqlServerMetadataReader implements DatabaseMetadataReader {

    private static final String[] TABLE_TYPES = {"TABLE"};

    @Override
    public boolean supports(String databaseProductName) {
        return databaseProductName.toLowerCase().contains("sql server");
    }

    @Override
    public String extractCatalog(String jdbcUrl) throws SQLException {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:sqlserver:")) {
            throw new SQLException("无效的SQL Server JDBC URL");
        }

        try {
            String[] parts = jdbcUrl.split(";");
            for (String part : parts) {
                if (part.toLowerCase().startsWith("databasename=")) {
                    return part.substring("databasename=".length());
                }
            }
        } catch (Exception e) {
            throw new SQLException("Error extracting catalog from JDBC URL", e);
        }

        throw new SQLException("无法从JDBC URL提取数据库名称");
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

        // 添加时区信息
        TimeZone timeZone = TimeZone.getDefault();
        metadata.put("timeZone", timeZone.getID());
        metadata.put("timeZoneOffset", timeZone.getRawOffset());
        metadata.put("timeZoneDSTOffset", timeZone.getDSTSavings());
        metadata.put("timeZoneID", timeZone.getID());
        metadata.put("timeZoneOffsetMillis", timeZone.getOffset(System.currentTimeMillis()));
        metadata.put("timeZoneDSTOffsetMillis", timeZone.getDSTSavings());

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
