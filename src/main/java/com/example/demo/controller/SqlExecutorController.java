package com.example.demo.controller;

import com.example.demo.dto.ConnectionInfo;
import com.example.demo.dto.QueryResult;
import com.example.demo.service.SqlExecutorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;

@Controller
@RequestMapping("/")
public class SqlExecutorController {

    @Autowired
    private SqlExecutorService sqlExecutorService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("connectionInfo", new ConnectionInfo());
        return "index";
    }

    @PostMapping("/connect")
    public String connect(@ModelAttribute ConnectionInfo connectionInfo, Model model) {
        try {
            boolean connected = sqlExecutorService.connect(connectionInfo);
            model.addAttribute("connectionSuccess", connected);
            model.addAttribute("connectionMessage", connected ? "连接成功" : "连接失败：无效的策略ID或数据库配置");
        } catch (Exception e) {
            model.addAttribute("connectionSuccess", false);
            model.addAttribute("connectionMessage", "连接错误：" + e.getMessage());
        }
        model.addAttribute("connectionInfo", connectionInfo);
        return "index";
    }

    @PostMapping("/execute")
    public String executeQuery(@ModelAttribute ConnectionInfo connectionInfo, 
                               @RequestParam String sqlQuery, 
                               Model model) {
        try {
            QueryResult result = sqlExecutorService.executeQuery(connectionInfo, sqlQuery);
            model.addAttribute("queryResult", result);
            model.addAttribute("connectionSuccess", true);
        } catch (SQLException e) {
            model.addAttribute("queryError", "查询错误：" + e.getMessage());
            model.addAttribute("connectionSuccess", true);
        } catch (Exception e) {
            model.addAttribute("queryError", "执行错误：" + e.getMessage());
            model.addAttribute("connectionSuccess", true);
        }
        model.addAttribute("connectionInfo", connectionInfo);
        model.addAttribute("sqlQuery", sqlQuery);
        return "index";
    }


}