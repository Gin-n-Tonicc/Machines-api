package com.machines.machines_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

//http://localhost:8080/swagger-ui/index.html
@SpringBootApplication
@EnableScheduling
public class MachinesApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MachinesApiApplication.class, args);
    }

}
