package com.wuyuan.database.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.BasicDBObject;
import io.cosmos.util.BalanceUtil;
import io.cosmos.util.CosmosUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

public class te {

    static String  url = "https://node.gaussdex.io/fec/gauss";
    public static BigDecimal getAddressInfo(String address){
        JSONObject balance = CosmosUtil.getAllBalance(url,address);

        if (balance == null){
            return null;
        }
        BigDecimal am = BigDecimal.ZERO;
        JSONArray balances = balance.getJSONArray("balances");
        for (int i = 0; i < balances.size(); i++) {
            if ("ufec".equalsIgnoreCase(balances.getJSONObject(i).getString("denom"))){
                String value = balances.getJSONObject(i).getString("amount");
                if (value == null){
                    balance.put("amount", BigDecimal.ZERO.toPlainString());
                }else {
                    am = am.add(new BigDecimal(value).divide(new BigDecimal(1000000)));
                    balance.put("amount",new BigDecimal(value).divide(new BigDecimal(1000000)).toPlainString());
                }
            }else {
                balance.put("amount",BigDecimal.ZERO.toPlainString());
            }
        }
        System.out.println(balance.getString("amount")+"---------------------amount");

        JSONArray delegator = BalanceUtil.getDelegatorTxByAddress(url,address);
        BigDecimal delegatorBalance = BigDecimal.ZERO;
        if (delegator != null && delegator.size() > 0){
            for (int i = 0; i < delegator.size(); i++) {
                JSONObject amount = delegator.getJSONObject(i);
                if("uigpc".equalsIgnoreCase(amount.getJSONObject("balance").getString("denom"))){
                    BigDecimal b = new BigDecimal(amount.getJSONObject("balance").getString("amount")).divide(new BigDecimal("1000000"));
                    delegatorBalance = delegatorBalance.add(b);
                }
            }
        }
        am = am.add(delegatorBalance);
        JSONObject unBonding = CosmosUtil.getUnBondingBalance(url,address);
        balance.put("address",address);
        balance.put("delegator_tx",delegator);
        balance.put("delegator_balance",delegatorBalance);
        System.out.println(delegatorBalance+"-----------------delegatorBalance");
        BigDecimal unDelegatorBalance = BigDecimal.ZERO;
        JSONArray unDelegator = BalanceUtil.getUnDelegators(url,address);
        if (unDelegator != null && unDelegator.size() > 0){
            for (int i = 0; i < unDelegator.size(); i++) {
                JSONObject amount = unDelegator.getJSONObject(i);
                JSONArray entries = amount.getJSONArray("entries");
                if (entries != null && entries.size() > 0){
                    for (int j = 0; j < entries.size(); j++) {
                        BigDecimal ba =new BigDecimal(entries.getJSONObject(j).getString("balance")).divide(new BigDecimal("1000000"));
                        unDelegatorBalance = unDelegatorBalance.add(ba);
                    }
                }
            }
        }
        balance.put("unbonding_tx",unBonding);
        balance.put("unBonding_balance",unDelegatorBalance);
        am = am.add(unDelegatorBalance);
        System.out.println(unDelegatorBalance + "--------------unDelegatorBalance");



//        BigDecimal rewards = BigDecimal.ZERO;
//        String s = isV(address);
//        if (StringUtils.isBlank(s)){
//            rewards = BalanceUtil.getRewardBalance(address,url);
//        }else {
//            rewards = BalanceUtil.getValidatorRewardBalance(s,url,"uigpc");
//        }
//        System.out.println(rewards+"-----------------------rewards");
//        am = am.add(rewards);
        return am;
    }


    public static String isV(String add){
        String res = null;
        try {
            res = HttpUtils.sendGetData("https://dataapi.gaussdex.io/V3/igpc/getValAddress?accAddress="+ add,"UTF-8");
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        if (res.indexOf("200") != -1){
           return JSON.parseObject(res).getJSONObject("result").getString("validatorsAddress");
       }
       return null;
    }


    public static void main(String[] args) {
        String res = HttpUtils.httpGet("http://192.168.2.13:8005/api/address/getAddressByAssets?pageIndex=1&pageSize=100");
        JSONArray r = JSON.parseObject(res).getJSONObject("result").getJSONArray("records");
        List<JSONObject> list = r.toJavaList(JSONObject.class);
        BigDecimal total = BigDecimal.ZERO;

        for (JSONObject add:list) {
            System.out.println(add);
            BigDecimal address = add.getBigDecimal("totalAmount");
            total = total.add(address);
        }
        System.out.println(total);
    }
}
