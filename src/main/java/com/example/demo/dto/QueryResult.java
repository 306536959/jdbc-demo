package com.example.demo.dto;

import lombok.Data;

import java.util.List;
@Data
public class QueryResult {
    private List<String> columnNames;
    private List<List<Object>> rows;
    private int rowsAffected;
    private boolean success;
    private String errorMessage;

    // getters and setters
    public boolean hasData() {
        return rows != null && !rows.isEmpty();
    }


}