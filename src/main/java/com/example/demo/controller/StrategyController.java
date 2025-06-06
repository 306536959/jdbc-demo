package com.example.demo.controller;

import com.example.demo.service.StrategyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
public class StrategyController {
@Autowired
private StrategyService strategyService;
    @GetMapping("/api/strategies")
    public List<String> getAvailableStrategies() {

        return strategyService.getAvailableStrategies();
    }
}
