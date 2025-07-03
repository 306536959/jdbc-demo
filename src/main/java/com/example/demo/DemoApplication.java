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

    static {
        try { Class.forName("com.ibm.db2.jcc.DB2Driver"); } catch (Exception ignored) {}
        try { Class.forName("com.mysql.cj.jdbc.Driver"); } catch (Exception ignored) {}
        try { Class.forName("org.opengauss.Driver"); } catch (Exception ignored) {}
        try { Class.forName("oracle.jdbc.OracleDriver"); } catch (Exception ignored) {}
        try { Class.forName("org.postgresql.Driver"); } catch (Exception ignored) {}
        try { Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver"); } catch (Exception ignored) {}
        try { Class.forName("dm.jdbc.driver.DmDriver"); } catch (Exception ignored) {}
        try { Class.forName("com.oceanbase.jdbc.Driver"); } catch (Exception ignored) {}
        System.out.println("所有主流数据库驱动已通过静态代码块强制注册");
    }

    /**
     * 应用主入口
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
