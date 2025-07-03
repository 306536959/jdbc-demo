package com.example.demo.service.spi.impl;

import com.example.demo.service.spi.DatabaseMetadataReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.TimeZone;

public class DB2MetadataReader implements DatabaseMetadataReader {

    private static final Logger log = LoggerFactory.getLogger(DB2MetadataReader.class);
    private static final String[] TABLE_TYPES = {"TABLE"};

    @Override
    public boolean supports(String databaseProductName) {
        return databaseProductName.toLowerCase().contains("db2");
    }


    @Override
    public String extractCatalog(String jdbcUrl) throws SQLException {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:db2:")) {
            throw new SQLException("无效的DB2 JDBC URL");
        }

        try {
            // 移除协议部分
            String urlWithoutProtocol = jdbcUrl.substring("jdbc:db2:".length());

            // 检查是否以 // 开头（网络连接）
            if (urlWithoutProtocol.startsWith("//")) {
                urlWithoutProtocol = urlWithoutProtocol.substring(2); // 去掉 "//"
                int slashIndex = urlWithoutProtocol.indexOf("/");
                if (slashIndex != -1) {
                    // 提取 / 后面的部分作为 catalog（直到 ; 或 ? 或结尾）
                    int endIndex = urlWithoutProtocol.indexOf(";", slashIndex);
                    if (endIndex == -1) endIndex = urlWithoutProtocol.length();
                    return urlWithoutProtocol.substring(slashIndex + 1, endIndex);
                } else {
                    throw new SQLException("JDBC URL 中未找到数据库名称");
                }
            } else {
                // 如果不是以 // 开头，可能是本地连接格式，尝试从参数中获取
                String[] params = jdbcUrl.split(";");
                for (String param : params) {
                    if (param.toLowerCase().startsWith("database=")) {
                        return param.substring("database=".length());
                    }
                }
            }
        } catch (Exception e) {
            throw new SQLException("Error extracting catalog from JDBC URL", e);
        }

        throw new SQLException("无法从JDBC URL提取catalog信息");
    }


    @Override
    public Map<String, Object> readMetadata(DatabaseMetaData metaData, String catalog) throws SQLException {
        try {
            Class.forName("com.ibm.db2.jcc.DB2Driver");
            log.info("DB2 驱动已加载");
        } catch (ClassNotFoundException e) {
            log.error("DB2 驱动未找到", e);
            throw new SQLException("DB2 驱动未找到", e);
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
