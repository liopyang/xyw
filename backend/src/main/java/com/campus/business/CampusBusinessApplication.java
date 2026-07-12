package com.campus.business;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@MapperScan("com.campus.business.mapper")
public class CampusBusinessApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusBusinessApplication.class, args);
    }
}
