package com.example.demo.service;

import com.example.demo.dto.StrategyInfo;
import com.example.demo.mapper.StrategyMapper;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class StrategyService {
    @Autowired
    private StrategyMapper strategyMapper;
    public List<StrategyInfo> getAvailableStrategies() {
        return strategyMapper.getAvailableStrategies();
//    return Arrays.asList("test1", "test2", "defaultStrategy");
    }
}
