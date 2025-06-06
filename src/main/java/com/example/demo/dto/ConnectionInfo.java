package com.example.demo.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ConnectionInfo implements Serializable {

    private String strategyId;
    private String jdbcUrl;
    private String username;
    private String password;
}    