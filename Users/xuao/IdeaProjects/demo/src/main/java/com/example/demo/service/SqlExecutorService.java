public QueryResult executeQuery(ConnectionInfo connectionInfo, String sqlQuery) throws SQLException {
    String strategyId = connectionInfo.getStrategyId();
    if (strategyId == null || !connectionStatus.getOrDefault(strategyId, false)) {
        throw new SQLException("未连接到数据库或连接已关闭");
    }

    if (sqlQuery == null || sqlQuery.trim().isEmpty()) {
        throw new SQLException("SQL查询不能为空");
    }

    // 添加表存在的检查（示例）
    if (sqlQuery.toUpperCase().contains("FROM USERS")) {
        try (Connection conn = getConnection(connectionInfo);
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            if (!rs.next()) {
                throw new SQLException("表 'users' 不存在");
            }
        }
    }

    QueryResult result = new QueryResult();
    try (Connection conn = getConnection(connectionInfo);
         Statement stmt = conn.createStatement()) {
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
        connectionStatus.put(strategyId, false);
        throw new SQLException("SQL执行失败: " + e.getMessage(), e);
    }

    return result;
}