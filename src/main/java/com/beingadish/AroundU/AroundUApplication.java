package com.beingadish.AroundU;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.beingadish.AroundU")
@EntityScan(basePackages = "com.beingadish.AroundU")
public class AroundUApplication {

    public static void main(String[] args) {
        SpringApplication.run(AroundUApplication.class, args);
    }
}
