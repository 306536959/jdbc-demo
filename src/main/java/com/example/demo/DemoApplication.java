package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.mybatis.spring.annotation.MapperScan;

/**
 * Spring Boot 应用启动类
 * 继承 SpringBootServletInitializer 以支持外部 Servlet 容器部署（如 Tomcat）
 */
@SpringBootApplication
@MapperScan("com.example.demo.mapper")
public class DemoApplication extends SpringBootServletInitializer {

    /**
     * 配置 SpringApplicationBuilder
     * @param builder 构建器实例
     * @return 配置完成的构建器
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(DemoApplication.class);
    }

    /**
     * 应用主入口
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
