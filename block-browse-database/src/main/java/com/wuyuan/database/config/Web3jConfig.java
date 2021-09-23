package com.wuyuan.database.config;

import com.wuyuan.database.sevice.ConfigService;
import com.wuyuan.database.util.ConfigUtil;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Configuration
public class Web3jConfig {

    @Resource
    private ConfigService configService;

    @Bean
    public Web3j web3j() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(30*1000, TimeUnit.MILLISECONDS);
        builder.writeTimeout(30*1000, TimeUnit.MILLISECONDS);
        builder.readTimeout(30*1000, TimeUnit.MILLISECONDS);
        OkHttpClient httpClient = builder.build();
        Web3j web3j = Web3j.build(new HttpService(configService.getConfig(ConfigUtil.ETHURL),httpClient,false));

        return web3j;
    }



}