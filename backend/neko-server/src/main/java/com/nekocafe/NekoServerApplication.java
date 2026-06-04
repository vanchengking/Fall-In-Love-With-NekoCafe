package com.nekocafe;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * NekoCafe 智慧餐饮预约平台后端入口（Spring Boot 3 + MyBatis-Plus + Redis）。
 */
@SpringBootApplication
@MapperScan("com.nekocafe.mapper")
public class NekoServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(NekoServerApplication.class, args);
    }
}
