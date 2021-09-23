package com.wuyuan.blockbrowse;

import com.spring4all.mongodb.EnableMongoPlus;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

@EnableMongoPlus
@SpringBootApplication
@ComponentScan(basePackages = {"com.wuyuan"})
public class BlockBrowseJobApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlockBrowseJobApplication.class, args);
    }

}
