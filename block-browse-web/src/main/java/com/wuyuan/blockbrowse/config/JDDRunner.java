package com.wuyuan.blockbrowse.config;

import com.wuyuan.database.sevice.ConfigService;
import com.wuyuan.database.util.ConfigUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;

@Component
public class JDDRunner implements ApplicationRunner {
    @Resource
    private ConfigService configService;

    private Integer port;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        new NettyServer(getPort()).start();
    }

    public Integer getPort(){
        if (port == null){
            port = new BigDecimal(configService.getConfig(ConfigUtil.wsPortKey)).intValue();
        }
        return port;
    }
}