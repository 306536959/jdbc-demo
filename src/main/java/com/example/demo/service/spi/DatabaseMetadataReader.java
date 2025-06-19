package com.example.demo.service.spi;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;

/**
 * 数据库元数据读取器接口
 */
public interface DatabaseMetadataReader {

    /**
     * 判断当前读取器是否支持指定的数据库类型
     * @param databaseProductName 数据库产品名称
     * @return 是否支持
     */
    boolean supports(String databaseProductName);

    /**
     * 从JDBC URL中提取catalog信息
     * @param jdbcUrl JDBC连接URL
     * @return catalog名称
     */
    String extractCatalog(String jdbcUrl) throws SQLException;

    /**
     * 读取数据库元数据
     * @param metaData 数据库元数据对象
     * @param catalog 数据库catalog
     * @return 元数据信息
     */
    Map<String, Object> readMetadata(DatabaseMetaData metaData, String catalog) throws SQLException;
}