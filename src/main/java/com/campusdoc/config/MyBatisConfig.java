package com.campusdoc.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.campusdoc.**.mapper")
public class MyBatisConfig {
}
