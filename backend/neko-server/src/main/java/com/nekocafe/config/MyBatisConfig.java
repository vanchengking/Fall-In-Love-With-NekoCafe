package com.nekocafe.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis mapper 扫描。独立于启动类，避免 @WebMvcTest 等切片测试
 * 在无数据源环境下误注册 mapper（sqlSessionFactory 缺失导致上下文启动失败）。
 */
@Configuration
@MapperScan("com.nekocafe.mapper")
public class MyBatisConfig {
}
