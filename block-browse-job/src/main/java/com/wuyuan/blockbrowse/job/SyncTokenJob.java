package com.wuyuan.blockbrowse.job;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.wuyuan.database.sevice.ConfigService;
import com.wuyuan.database.sevice.TokenService;
import com.wuyuan.database.util.ConfigUtil;
import io.cosmos.util.CosmosUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@EnableScheduling
@Slf4j
public class SyncTokenJob {

    @Resource
    private ConfigService configService;

//    private String chainPrefix;

    @Resource
    private TokenService tokenService;

    @Scheduled(fixedDelay = 1000*60)
    public void syncToken(){
        log.info("token开始同步");
        JSONObject allToken = JSON.parseObject(CosmosUtil.getTokens(configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix(),100), Feature.DisableSpecialKeyDetect);
        if (allToken != null && allToken.getJSONObject("pagination").getIntValue("total") > 100){
            allToken = JSON.parseObject(CosmosUtil.getTokens(configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix(),allToken.getJSONObject("pagination").getIntValue("total")),Feature.DisableSpecialKeyDetect);
        }
        if (allToken != null){
            JSONArray tokens = allToken.getJSONArray("Tokens");
            tokens.stream().forEach(token ->{
                if (token instanceof JSONObject){
                    if (tokenService.isExistValidator(((JSONObject) token).getString("symbol")) > 0){
                        tokenService.updateToken(((JSONObject) token).getString("symbol"), (JSONObject) token);
                    }else {
                        tokenService.saveToken((JSONObject) token);
                    }
                }
            });
        }
        log.info("token同步结束");
    }

//    public String getChainPrefix(){
//        if (chainPrefix == null){
//            chainPrefix = configService.getConfig(ConfigUtil.chainPrefixKey);
//        }
//        return chainPrefix;
//    }
}
