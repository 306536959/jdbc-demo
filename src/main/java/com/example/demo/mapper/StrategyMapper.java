package com.example.demo.mapper;

import com.example.demo.dto.StrategyInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface StrategyMapper {
    List<StrategyInfo> getAvailableStrategies();
    // 方法定义
}
