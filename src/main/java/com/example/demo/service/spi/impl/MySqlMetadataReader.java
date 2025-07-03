package com.example.demo.service.spi.impl;

import com.example.demo.service.spi.DatabaseMetadataReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * MySQL数据库元数据读取器
 */
public class MySqlMetadataReader implements DatabaseMetadataReader {

    private static final Logger log = LoggerFactory.getLogger(MySqlMetadataReader.class);
    private static final String[] TABLE_TYPES = {"TABLE"};

    @Override
    public boolean supports(String databaseProductName) {
        return databaseProductName.toLowerCase().contains("mysql");
    }

    @Override
    public String extractCatalog(String jdbcUrl) throws SQLException {
        log.info("MySQL URL: {}", jdbcUrl);
        int lastSlashIndex = jdbcUrl.lastIndexOf("/");
        int queryIndex = jdbcUrl.indexOf("?");
        if (queryIndex == -1) queryIndex = jdbcUrl.length();
        if (lastSlashIndex + 1 >= queryIndex) {
            throw new SQLException("MySQL URL中未指定数据库名称");
        }
        return jdbcUrl.substring(lastSlashIndex + 1, queryIndex);
    }

    @Override
    public Map<String, Object> readMetadata(DatabaseMetaData metaData, String catalog) throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            log.info("MySQL 驱动已加载");
        } catch (ClassNotFoundException e) {
            log.error("MySQL 驱动未找到", e);
            throw new SQLException("MySQL 驱动未找到", e);
        }
        Map<String, Object> metadata = new HashMap<>();
        List<Map<String, Object>> tables = new ArrayList<>();

        try (ResultSet rs = metaData.getTables(catalog, null, null, TABLE_TYPES)) {
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
        try (ResultSet rs = metaData.getColumns(catalog, null, tableName, null)) {
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