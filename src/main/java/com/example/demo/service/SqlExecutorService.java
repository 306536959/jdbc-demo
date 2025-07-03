package com.example.demo.service;

import com.example.demo.dto.ConnectionInfo;
import com.example.demo.dto.QueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SqlExecutorService {

    private final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();
    private final Map<String, Boolean> connectionStatus = new ConcurrentHashMap<>();

    public boolean connect(ConnectionInfo connectionInfo) throws Exception {
        String strategyId = connectionInfo.getStrategyId();
        if (strategyId == null || strategyId.isEmpty()) {
            log.error("[connect] 策略ID不能为空");
            throw new IllegalArgumentException("策略ID不能为空");
        }

        if (dataSources.containsKey(strategyId) && connectionStatus.getOrDefault(strategyId, false)) {
            log.info("[connect] 策略ID {} 已连接，无需重复连接", strategyId);
            return true;
        }

        String url = "KS:strategyCode:" + strategyId;
        log.info("[connect] 尝试创建数据源，策略ID: {}, URL: {}", strategyId, url);
        DataSource dataSource = null;
        try {
            dataSource = YamlShardingSphereDataSourceFactory.createDataSource(url);
            log.info("[connect] DataSource 创建成功，准备测试连接");
        } catch (Exception e) {
            log.error("[connect] DataSource 创建失败，策略ID: {}, URL: {}，异常: {}", strategyId, url, e.getMessage(), e);
            connectionStatus.put(strategyId, false);
            throw new Exception("创建数据源失败: " + e.getMessage(), e);
        }

        try (Connection testConn = dataSource.getConnection()) {
            String defaultSchema = testConn.getSchema();
            log.info("[connect] 测试连接成功，默认模式名称: {}", defaultSchema);
            boolean isValid = testConn != null && !testConn.isClosed();
            if (isValid) {
                dataSources.put(strategyId, dataSource);
                connectionStatus.put(strategyId, true);
                return true;
            }
        } catch (SQLException e) {
            log.error("[connect] 测试连接失败，策略ID: {}, URL: {}，异常: {}", strategyId, url, e.getMessage(), e);
            connectionStatus.put(strategyId, false);
            throw new SQLException("连接数据库失败: " + e.getMessage(), e);
        }

        log.warn("[connect] 未知原因导致连接失败，策略ID: {}", strategyId);
        return false;
    }

    public QueryResult executeQuery(ConnectionInfo connectionInfo, String sqlQuery) throws SQLException {
        String strategyId = connectionInfo.getStrategyId();
        if (strategyId == null || !connectionStatus.getOrDefault(strategyId, false)) {
            throw new SQLException("未连接到数据库或连接已关闭");
        }

        if (sqlQuery == null || sqlQuery.trim().isEmpty()) {
            throw new SQLException("SQL查询不能为空");
        }

        QueryResult result = new QueryResult();
        try (Connection conn = getConnection(connectionInfo);
             //当前模式名称是
             Statement stmt = conn.createStatement()) {
            String currentSchema = conn.getSchema();
            if (currentSchema != null && !currentSchema.isEmpty()) {
                log.info("当前模式名称: " + currentSchema);
            }
            if (sqlQuery.trim().toUpperCase().startsWith("SELECT")) {
                try (ResultSet rs = stmt.executeQuery(sqlQuery)) {
                    processResultSet(rs, result);
                }
            } else {
                int rowsAffected = stmt.executeUpdate(sqlQuery);
                result.setRowsAffected(rowsAffected);
                result.setSuccess(true);
            }
        } catch (SQLException e) {
            throw new SQLException("SQL执行失败: " + e.getMessage(), e);
        }

        return result;
    }

    private void processResultSet(ResultSet rs, QueryResult result) throws SQLException {
        if (rs == null) {
            return;
        }

        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        List<String> columnNames = new ArrayList<>();
        for (int i = 1; i <= columnCount; i++) {
            columnNames.add(metaData.getColumnName(i));
        }
        result.setColumnNames(columnNames);

        List<List<Object>> rows = new ArrayList<>();
        while (rs.next()) {
            List<Object> row = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                row.add(rs.getObject(i));
            }
            rows.add(row);
        }
        result.setRows(rows);
        result.setSuccess(true);
    }

    private Connection getConnection(ConnectionInfo config) throws SQLException {
        String strategyId = config.getStrategyId();
        DataSource dataSource = dataSources.get(strategyId);
        if (dataSource == null) {
            throw new SQLException("未找到对应策略ID的数据源: " + strategyId);
        }
        return dataSource.getConnection();
    }

    public void disconnect(String strategyId) {
        if (strategyId != null && dataSources.containsKey(strategyId)) {
            dataSources.remove(strategyId);
            connectionStatus.put(strategyId, false);
        }
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                // 记录日志但不抛出异常
            }
        }
    }

    public boolean isConnected(String strategyId) {
        return strategyId != null && connectionStatus.getOrDefault(strategyId, false);
    }

    public DataSource getDataSource(String strategyId) {
        return dataSources.get(strategyId);
    }
}