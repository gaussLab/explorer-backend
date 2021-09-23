package com.wuyuan.blockbrowse;

import com.spring4all.mongodb.EnableMongoPlus;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@EnableMongoPlus
@SpringBootApplication
@ComponentScan(basePackages = {"com.wuyuan"})
public class CrossChainApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(CrossChainApiApplication.class,args);
    }
}
