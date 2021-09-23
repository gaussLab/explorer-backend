package com.wuyuan.blockbrowse.job;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.wuyuan.database.sevice.ConfigService;
import com.wuyuan.database.sevice.MongoSevice;
import com.wuyuan.database.sevice.ValidatorDelegatorService;
import com.wuyuan.database.sevice.ValidatorService;
import com.wuyuan.database.util.Collocation;
import com.wuyuan.database.util.ConfigUtil;
import io.cosmos.util.BalanceUtil;
import io.cosmos.util.CosmosUtil;
import io.cosmos.util.PubkeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;

@Component
@EnableScheduling
@Slf4j
public class SyncValidatorsJob {
    @Resource
    private ValidatorService validatorService;

    @Resource
    private ValidatorDelegatorService validatorDelegatorService;

//    private String chainName;
//
//    private String bech32Prefix;
//
//    private String chainPrefix;


    @Resource
    private ConfigService configService;

    @Scheduled(fixedDelay = 1000 * 60)
    public void getValidators(){
        log.info("validators 启动");
        String bondedTokens = JSON.parseObject(CosmosUtil.getPool(configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix())).getJSONObject("pool").getString("bonded_tokens");
        BigDecimal bondedToken = new BigDecimal(bondedTokens).divide(new BigDecimal(Math.pow(10,6)));//获取总的委托金额

        String result = CosmosUtil.getAllvalidatorsets(configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix());

        JSONArray validators = JSON.parseObject(result, Feature.DisableSpecialKeyDetect).getJSONArray("validators");
        validators.stream().forEach(validator ->{
            if (validator instanceof JSONObject){
                String operatorAddress = ((JSONObject) validator).getString("operator_address");
                //获取该验证人的总委托数
//                String res = CosmosUtil.getAllDelegations(configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix(),operatorAddress);
//                String totalDelegations = JSON.parseObject(res).getJSONObject("pagination").getString("total");
                long totalDelegations = validatorDelegatorService.getDelegatorCount(operatorAddress);
                ((JSONObject) validator).put("totalDelegations",totalDelegations);
                //验证人图标
                String identity = ((JSONObject) validator).getJSONObject("description").getString("identity");
                if (StringUtils.isNotBlank(identity)){
                    String icon = CosmosUtil.getValidatorsetsIcon(identity);
                    if (StringUtils.isBlank(icon)){
                        String picture = configService.getConfig(ConfigUtil.pictureKey);
                        ((JSONObject) validator).put("icon",picture);
                    }else {
                        ((JSONObject) validator).put("icon",icon);
                    }
                }else {
                    String icon = configService.getConfig(ConfigUtil.pictureKey);
                    ((JSONObject) validator).put("icon",icon);
                }

                //投票权
                String tokenNumber = ((JSONObject) validator).getString("tokens");
                tokenNumber = new BigDecimal(tokenNumber).divide(new BigDecimal(Math.pow(10,6))).toPlainString();
                String votingPower = new BigDecimal(tokenNumber).divide(bondedToken,6, RoundingMode.HALF_UP).toPlainString();
                ((JSONObject) validator).put("votingPower",votingPower);
                //自我委托地址
                String selfDelegateAddress = CosmosUtil.getDelegatorByOperatorAddr(configService.getChainName(),operatorAddress);
                ((JSONObject) validator).put("selfDelegateAddress",selfDelegateAddress);
                //通过自我委托地址获取自己质押的金额
                JSONObject selfDelegateAmount = JSON.parseObject(CosmosUtil.getDelegatorInfo(configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix(),operatorAddress,selfDelegateAddress));
                if (selfDelegateAmount == null || selfDelegateAmount.containsKey("code")){
                    ((JSONObject) validator).put("selfDelegateAmount","0");
                }else {
                    String delegateAmount = new BigDecimal(selfDelegateAmount.getJSONObject("delegation_response").getJSONObject("balance").getString("amount")).divide(new BigDecimal(Math.pow(10,6))).toPlainString();//单位转换
                    ((JSONObject) validator).put("selfDelegateAmount",delegateAmount);
                }
                BigDecimal commissionRewards = BalanceUtil.getValidateatorReward(operatorAddress,configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix(),configService.getChainName());
                ((JSONObject) validator).put("commission_rewards",commissionRewards);

                String conAddress = PubkeyUtil.getAddressFromPubkey(((JSONObject) validator).getJSONObject("consensus_pubkey").getString("key"));
                ((JSONObject) validator).put("address",conAddress);
                try {
                    conAddress = PubkeyUtil.hexToBech32(conAddress,configService.getBech32Prefix());
                } catch (UnsupportedEncodingException e) {

                }
                ((JSONObject) validator).put("publicKey",conAddress);

                if (conAddress != null){
                    JSONObject signingInfo = JSON.parseObject(CosmosUtil.getSigningInfos(configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix(),conAddress));
                    if (null != signingInfo){
                        ((JSONObject) validator).put("val_signing_info",signingInfo);
                        String missedCounter = signingInfo.getJSONObject("val_signing_info").getString("missed_blocks_counter");

                        JSONObject slashingParams = JSON.parseObject(CosmosUtil.getSignedBlocksWindow(configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix()));
                        int blocksWindows = slashingParams.getJSONObject("params").getIntValue("signed_blocks_window");
                        String uptime = (new BigDecimal(blocksWindows).subtract(new BigDecimal(missedCounter))).divide(new BigDecimal(blocksWindows)).multiply(new BigDecimal(100)).toPlainString();

                        ((JSONObject) validator).put("uptime",uptime);
                    }
                }

                if (validatorService.isExistValidator(operatorAddress) >= 1){
                    validatorService.updateValidator(operatorAddress, ((JSONObject) validator));
                }else {
                    validatorService.save(((JSONObject) validator));
                }
            }
        });
        log.info("validators 同步完成");
    }

//    public String getChainName(){
//        if (chainName == null){
//            chainName = configService.getConfig(ConfigUtil.chainNameKey);
//        }
//        return chainName;
//    }
//
//    public String getChainPrefix(){
//        if (chainPrefix == null){
//            chainPrefix = configService.getConfig(ConfigUtil.chainPrefixKey);
//        }
//        return chainPrefix;
//    }
//
//    public String getBech32Prefix(){
//        if (bech32Prefix == null){
//            bech32Prefix = configService.getConfig(ConfigUtil.bech32PrefixKey);
//        }
//        return bech32Prefix;
//    }

}
