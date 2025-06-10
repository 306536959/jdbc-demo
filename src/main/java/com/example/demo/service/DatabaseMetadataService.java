package com.example.demo.service;

import com.example.demo.dto.ConnectionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Service
public class DatabaseMetadataService {

    @Autowired
    private SqlExecutorService sqlExecutorService;

    public Map<String, Object> getDatabaseMetadata(String strategyId) {
        ConnectionInfo connectionInfo = new ConnectionInfo();
        connectionInfo.setStrategyId(strategyId);

        try {
            if (sqlExecutorService.isConnected(strategyId)) {
                DataSource dataSource = sqlExecutorService.getDataSource(strategyId);
                try (Connection connection = dataSource.getConnection()) {
                    DatabaseMetaData metaData = connection.getMetaData();

                    // 提取当前连接的 catalog（数据库名称）
                    String catalog = extractCatalogFromURL(connection.getMetaData().getURL());

                    return extractMetadata(metaData, catalog);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Collections.emptyMap();
    }

    /**
     * 从 JDBC URL 中提取数据库名称（catalog）
     */
    private String extractCatalogFromURL(String jdbcUrl) throws SQLException {
        if (jdbcUrl == null || !jdbcUrl.contains("//")) {
            throw new SQLException("无效的 JDBC URL");
        }

        if (jdbcUrl.startsWith("jdbc:mysql:")) {
            int lastSlashIndex = jdbcUrl.lastIndexOf("/");
            int queryIndex = jdbcUrl.indexOf("?");
            if (queryIndex == -1) queryIndex = jdbcUrl.length();
            return jdbcUrl.substring(lastSlashIndex + 1, queryIndex);
        } else if (jdbcUrl.startsWith("jdbc:postgresql:")) {
            int startIndex = jdbcUrl.indexOf("//") + 2;
            int slashIndex = jdbcUrl.indexOf("/", startIndex);
            if (slashIndex != -1) {
                return jdbcUrl.substring(slashIndex + 1);
            } else {
                throw new SQLException("无法从 PostgreSQL URL 提取数据库名称");
            }
        } else if (jdbcUrl.startsWith("jdbc:sqlserver:")) {
            String[] parts = jdbcUrl.split(";");
            for (String part : parts) {
                if (part.toLowerCase().startsWith("database=")) {
                    return part.split("=")[1];
                }
            }
            throw new SQLException("SQL Server URL 中未指定 database 参数");
        } else {
            throw new SQLException("不支持的数据库类型");
        }
    }

    /**
     * 提取指定 catalog 下的表结构
     */
    private Map<String, Object> extractMetadata(DatabaseMetaData metaData, String catalog) throws SQLException {
        Map<String, Object> metadata = new HashMap<>();
        List<Map<String, Object>> tables = new ArrayList<>();

        try (ResultSet rs = metaData.getTables(catalog, null, null, new String[]{"TABLE"})) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                Map<String, Object> table = new HashMap<>();
                table.put("name", tableName);
                table.put("columns", getColumns(metaData, catalog, tableName));
                tables.add(table);
            }
        }

        metadata.put("tables", tables);
        return metadata;
    }

    /**
     * 获取指定表的字段信息
     */
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
