package com.wuyuan.blockbrowse;

import com.spring4all.mongodb.EnableMongoPlus;
import com.wuyuan.blockbrowse.config.NettyServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;

@EnableMongoPlus
@SpringBootApplication
@ComponentScan(basePackages = {"com.wuyuan"})
@ServletComponentScan
public class BlockBrowseWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlockBrowseWebApplication.class, args);
    }

}
