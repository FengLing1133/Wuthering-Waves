package com.wutheringwaves.gacha;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.wutheringwaves.gacha.mapper")
public class GachaApplication {
    public static void main(String[] args) {
        SpringApplication.run(GachaApplication.class, args);
    }
}
