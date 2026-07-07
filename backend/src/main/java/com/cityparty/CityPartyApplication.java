package com.cityparty;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.cityparty.module.**.mapper")
@SpringBootApplication
public class CityPartyApplication {

    public static void main(String[] args) {
        SpringApplication.run(CityPartyApplication.class, args);
    }
}
