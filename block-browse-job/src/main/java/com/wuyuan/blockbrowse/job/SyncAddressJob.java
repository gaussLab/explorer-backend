package com.wuyuan.blockbrowse.job;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wuyuan.database.sevice.AddressService;
import com.wuyuan.database.sevice.ConfigService;
import com.wuyuan.database.sevice.ValidatorService;
import com.wuyuan.database.util.ConfigUtil;
import io.cosmos.util.BalanceUtil;
import io.cosmos.util.CosmosUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@EnableScheduling
@Slf4j
public class SyncAddressJob {
    @Resource
    private AddressService addressService;
    @Resource
    private ConfigService configService;
    @Resource
    private ValidatorService validatorService;

//    private String chainPrefix;
//
//    private String coinName;

    private static AtomicInteger threadNum = new AtomicInteger(0);

    private static ExecutorService pool = new ThreadPoolExecutor(1,20,1000, TimeUnit.MILLISECONDS,new LinkedBlockingQueue<Runnable>(), Executors.defaultThreadFactory());

    int count = 500;

    @Scheduled(fixedDelay = 60*1000*30)
    public void addressTotal(){
        log.info("address的totalAmount同步开始");
        if (threadNum.get() != 0){
            log.info("存在上次线程");
            return;
        }
        Integer totalAddress = addressService.getAddressCount().intValue();
        log.info(totalAddress+"-----------");
        int num = 1;
        if (totalAddress%count > 0){
            num = totalAddress/count + 1;
        }else {
            num = totalAddress/count;
        }

        for (int i = 0; i < num; i++) {
            int finalI = i;
            threadNum.incrementAndGet();
            log.info("线程数+1-------------"+threadNum.get());
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    List<JSONObject> addressList = addressService.getAddress(finalI +1,count);
                    addressList.stream().forEach( addressJson ->{
                        String address = addressJson.getString("address");
                        addressJson = getAddressInfo(address,addressJson);
                        addressService.updateAddress(address,addressJson);
 //                       JSONObject balance = CosmosUtil.getBalance(configService.getConfig(ConfigUtil.chainUrlKey) + "/" + configService.getChainPrefix(),address,configService.getCoinName());
                        BigDecimal amount = addressJson.getBigDecimal("amount");
//                        if (balance != null){
//                            amount = balance.getJSONObject("balance").getBigDecimal("amount").divide(new BigDecimal(1000000)).toPlainString();
//                        }else {
//                            amount = addressJson.getString("amount");
//                        }
//                        BigDecimal rewards = BigDecimal.ZERO;
//                        if (validatorService.isExistValidatorBySelfAddress(address) == 0){
//                            rewards = BalanceUtil.getRewardBalance(address,configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix());
//                        }else {
//                            JSONObject validator = validatorService.getValidatorBySelfDelegateAddress(address);
//                            String validatorAddress = validator.getString("operator_address");
//                            rewards = BalanceUtil.getValidatorRewardBalance(validatorAddress,configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix(),configService.getCoinName());
//                        }
                        String delegateBalance = addressJson.getString("delegator_balance");
                        String unBondingBalance = addressJson.getString("unBonding_balance");
                        if (amount == null){
                            amount = BigDecimal.ZERO;
                        }
                        if (!configService.getCoinName().equals("uusdg")){
                            amount = amount.add(new BigDecimal(delegateBalance)).add(new BigDecimal(unBondingBalance));
                        }
                        addressJson.put("totalAmount",amount);
                        addressService.updateAddressTotal(address,addressJson);
                    });
                    threadNum.decrementAndGet();
                    log.info("当前线程数-------" + threadNum.get());
                }
            });
        }
        log.info("address的totalAmount同步完成");
    }

    public JSONObject getAddressInfo(String address,JSONObject addressJson){
        JSONObject balance = CosmosUtil.getAllBalance(configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix(),address);
        if (balance == null){
            return addressJson;
        }

        JSONArray balances = balance.getJSONArray("balances");
        addressJson.put("balances",balances);
        for (int i = 0; i < balances.size(); i++) {
            if (configService.getCoinName().equalsIgnoreCase(balances.getJSONObject(i).getString("denom"))){
                String value = balances.getJSONObject(i).getString("amount");
                if (value == null){
                    addressJson.put("amount",BigDecimal.ZERO.toPlainString());
                }else {
                    addressJson.put("amount",new BigDecimal(value).divide(new BigDecimal(1000000)).toPlainString());
                }
            }
        }
//        JSONArray delegator = BalanceUtil.getDelegatorTxByAddress(configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix(),address);
//        BigDecimal delegatorBalance = BigDecimal.ZERO;
//        if (delegator != null && delegator.size() > 0){
//            for (int i = 0; i < delegator.size(); i++) {
//                JSONObject amount = delegator.getJSONObject(i);
//                if(configService.getCoinName().equalsIgnoreCase(amount.getJSONObject("balance").getString("denom"))){
//                    BigDecimal b = new BigDecimal(amount.getJSONObject("balance").getString("amount")).divide(new BigDecimal("1000000"));
//                    delegatorBalance = delegatorBalance.add(b);
//                }
//            }
//        }
//        JSONObject unBonding = CosmosUtil.getUnBondingBalance(configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix(),address);
//        balance.put("address",address);
//        balance.put("delegator_tx",delegator);
//        balance.put("delegator_balance",delegatorBalance);

//        BigDecimal unDelegatorBalance = BigDecimal.ZERO;
//        JSONArray unDelegator = BalanceUtil.getUnDelegators(configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix(),address);
//        if (unDelegator != null && unDelegator.size() > 0){
//            for (int i = 0; i < unDelegator.size(); i++) {
//                JSONObject amount = unDelegator.getJSONObject(i);
//                JSONArray entries = amount.getJSONArray("entries");
//                if (entries != null && entries.size() > 0){
//                    for (int j = 0; j < entries.size(); j++) {
//                        BigDecimal ba =new BigDecimal(entries.getJSONObject(j).getString("balance")).divide(new BigDecimal("1000000"));
//                        unDelegatorBalance = unDelegatorBalance.add(ba);
//                    }
//                }
//            }
//        }
//        balance.put("unbonding_tx",unBonding);
//        balance.put("unBonding_balance",unDelegatorBalance);
        return addressJson;
    }
}
