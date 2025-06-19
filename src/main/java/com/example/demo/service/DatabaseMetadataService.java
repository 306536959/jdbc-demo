package com.example.demo.service;

import com.example.demo.service.spi.DatabaseMetadataReader;
import com.example.demo.dto.ConnectionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.*;

@Service
public class DatabaseMetadataService {

    @Autowired
    private SqlExecutorService sqlExecutorService;

    // 加载所有可用的元数据读取器
    private final ServiceLoader<DatabaseMetadataReader> metadataReaders =
            ServiceLoader.load(DatabaseMetadataReader.class);

    public Map<String, Object> getDatabaseMetadata(String strategyId) {
        ConnectionInfo connectionInfo = new ConnectionInfo();
        connectionInfo.setStrategyId(strategyId);

        try {
            if (sqlExecutorService.isConnected(strategyId)) {
                DataSource dataSource = sqlExecutorService.getDataSource(strategyId);
                try (Connection connection = dataSource.getConnection()) {
                    DatabaseMetaData metaData = connection.getMetaData();
                    String databaseProductName = metaData.getDatabaseProductName();

                    // 查找支持当前数据库类型的读取器
                    DatabaseMetadataReader reader = findReader(databaseProductName);
                    if (reader == null) {
                        throw new SQLException("不支持的数据库类型: " + databaseProductName);
                    }

                    // 提取catalog并读取元数据
                    String catalog = reader.extractCatalog(metaData.getURL());
                    return reader.readMetadata(metaData, catalog);
                }
            } else {
                throw new SQLException("策略ID " + strategyId + " 未连接到数据库");
            }
        } catch (SQLException e) {
            System.err.println("获取数据库元数据失败: " + e.getMessage());
            e.printStackTrace();
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * 查找支持指定数据库类型的读取器
     */
    private DatabaseMetadataReader findReader(String databaseProductName) {
        for (DatabaseMetadataReader reader : metadataReaders) {
            if (reader.supports(databaseProductName)) {
                return reader;
            }
        }
        return null;
    }
}