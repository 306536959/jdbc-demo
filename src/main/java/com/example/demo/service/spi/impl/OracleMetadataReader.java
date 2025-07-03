package com.example.demo.service.spi.impl;

import com.example.demo.service.spi.DatabaseMetadataReader;
import java.sql.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OracleMetadataReader implements DatabaseMetadataReader {
    private static final Logger log = LoggerFactory.getLogger(OracleMetadataReader.class);
    private static final String[] TABLE_TYPES = {"TABLE"};

    @Override
    public boolean supports(String databaseProductName) {
        return databaseProductName.toLowerCase().contains("oracle");
    }

    @Override
    public String extractCatalog(String jdbcUrl) throws SQLException {
        // Oracle JDBC URL 通常格式为 jdbc:oracle:thin:@host:port:sid 或 jdbc:oracle:thin:@//host:port/service
        // 这里只简单提取最后的sid或service名
        int lastColon = jdbcUrl.lastIndexOf(":");
        int lastSlash = jdbcUrl.lastIndexOf("/");
        if (jdbcUrl.contains("@//")) {
            // 形如 jdbc:oracle:thin:@//host:port/service
            if (lastSlash != -1) {
                int paramIndex = jdbcUrl.indexOf("?", lastSlash);
                if (paramIndex == -1) paramIndex = jdbcUrl.length();
                return jdbcUrl.substring(lastSlash + 1, paramIndex);
            }
        } else if (lastColon != -1) {
            // 形如 jdbc:oracle:thin:@host:port:sid
            int paramIndex = jdbcUrl.indexOf("?", lastColon);
            if (paramIndex == -1) paramIndex = jdbcUrl.length();
            return jdbcUrl.substring(lastColon + 1, paramIndex);
        }
        throw new SQLException("无法从Oracle JDBC URL提取数据库名");
    }

    @Override
    public Map<String, Object> readMetadata(DatabaseMetaData metaData, String catalog) throws SQLException {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            log.info("Oracle 驱动已加载");
        } catch (ClassNotFoundException e) {
            log.error("Oracle 驱动未找到", e);
            throw new SQLException("Oracle 驱动未找到", e);
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