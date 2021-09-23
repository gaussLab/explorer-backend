package com.wuyuan.blockbrowse.job;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wuyuan.database.sevice.ConfigService;
import com.wuyuan.database.sevice.ValidatorDelegatorService;
import com.wuyuan.database.sevice.ValidatorService;
import com.wuyuan.database.util.ConfigUtil;
import io.cosmos.util.CosmosUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
@EnableScheduling
@Slf4j
public class SyncDelegretorJob {
    @Resource
    private ValidatorService validatorService;

    @Resource
    private ValidatorDelegatorService validatorDelegatorService;

    @Resource
    private ConfigService configService;

//    private String chainPrefix;


    @Scheduled(cron = "0 0 3 * * ? ")
    public void getDelegator(){
        log.info("delegretor 启动");
        List<String> validatorsAddress = validatorService.getValidatorAddress();
        validatorsAddress.stream().forEach( operatorAddress -> {
//            JSONObject tx = CosmosUtil.getTotalDelegations(configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix(),operatorAddress,100000);
//            JSONArray delegationResponses = tx.getJSONArray("delegation_responses");
//            if (delegationResponses.size() != tx.getJSONObject("pagination").getIntValue("total")){
//                delegationResponses = CosmosUtil.getTotalDelegations(configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix(),operatorAddress,tx.getJSONObject("pagination").getIntValue("total")).getJSONArray("delegation_responses");
//            }
//            validatorDelegatorService.deleteDelegator(operatorAddress);
//            delegationResponses.stream().forEach( delegator ->{
//                if (delegator instanceof JSONObject){
//                    JSONObject transfer = (JSONObject) delegator;
//                    validatorDelegatorService.saveDelegator(transfer);
//                }
//            });

            JSONObject unTx = CosmosUtil.getTotalUnDelegations(configService.getConfig(ConfigUtil.chainUrlKey) + "/"+configService.getChainPrefix(),operatorAddress,100000);
            JSONArray unDelegationResponses = unTx.getJSONArray("unbonding_responses");
            if (unDelegationResponses.size() != unTx.getJSONObject("pagination").getIntValue("total")){
                unDelegationResponses = CosmosUtil.getTotalUnDelegations(configService.getConfig(ConfigUtil.chainUrlKey) + "/"+configService.getChainPrefix(),operatorAddress,unTx.getJSONObject("pagination").getIntValue("total")).getJSONArray("delegation_responses");
            }
            validatorDelegatorService.deleteUnDelegator(operatorAddress);
            unDelegationResponses.stream().forEach(unDelegator -> {
                if (unDelegator instanceof JSONObject){
                    JSONObject unTransfer = (JSONObject) unDelegator;
                    validatorDelegatorService.saveUnDelegator(unTransfer);
                }
            });
        });
        log.info("delegretort同步完成");
    }

//    public String getChainPrefix(){
//        if (chainPrefix == null){
//            chainPrefix = configService.getConfig(ConfigUtil.chainPrefixKey);
//        }
//        return chainPrefix;
//    }
}
