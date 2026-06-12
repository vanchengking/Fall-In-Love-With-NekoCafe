package com.nekocafe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * NekoCafe 智慧餐饮预约平台后端入口（Spring Boot 3 + MyBatis-Plus + Redis）。
 * mapper 扫描见 {@link com.nekocafe.config.MyBatisConfig}。
 */
@SpringBootApplication
public class NekoServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(NekoServerApplication.class, args);
    }
}
