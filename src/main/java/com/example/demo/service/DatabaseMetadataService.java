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
                    return extractMetadata(metaData);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Collections.emptyMap();
    }

    private Map<String, Object> extractMetadata(DatabaseMetaData metaData) throws SQLException {
        Map<String, Object> metadata = new HashMap<>();
        List<Map<String, Object>> tables = new ArrayList<>();

        try (ResultSet rs = metaData.getTables(null, null, null, new String[]{"TABLE"})) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                Map<String, Object> table = new HashMap<>();
                table.put("name", tableName);
                table.put("columns", getColumns(metaData, tableName));
                tables.add(table);
            }
        }

        metadata.put("tables", tables);
        return metadata;
    }

    private List<Map<String, Object>> getColumns(DatabaseMetaData metaData, String tableName) throws SQLException {
        List<Map<String, Object>> columns = new ArrayList<>();
        try (ResultSet rs = metaData.getColumns(null, null, tableName, null)) {
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