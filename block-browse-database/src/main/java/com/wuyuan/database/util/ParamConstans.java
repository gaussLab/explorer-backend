package com.wuyuan.database.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.cosmos.util.CosmosUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ParamConstans {
    public String value;
    public long time;
    public static Map<String,ParamConstans> param=new HashMap<>();

    public static boolean totalFlag = false;

    public static boolean coinFlag = false;

    public static boolean poolFlag = false;

    public static boolean communityFlag = false;

    public static boolean inflationFlag = false;

    public static String dbCoinValue = "0.0034";

    public static void put(ParamConstans paramConstans,String key){
        param.put(key,paramConstans);
    }

    public static ParamConstans getParamConstans(String key){
        ParamConstans params = param.get(key);

        return params;
    }

    public static String getTotalChane(String url,String prefix,String coinName){
        ParamConstans totalJson=ParamConstans.getParamConstans("totalSupply");
        String totalSupply = null;
        if (totalJson != null){
            if ( (StringUtils.isBlank(totalJson.value) || totalJson.time < System.currentTimeMillis()-5*1000*60) && !totalFlag ){
                    new Thread(() -> {
                        totalFlag = true;
                        getTotal(url,prefix,coinName);
                        totalFlag = false;
                    }).start();
            }
            totalSupply=totalJson.getValue();
        }else {
            totalSupply = getTotal(url,prefix,coinName);
        }
        return totalSupply;
    }

    public static String getCoinValueChane(String coinName,String coinValueUrl,String head){
        ParamConstans coinAmount=ParamConstans.getParamConstans("coinAmount");
        String coinValue = null;
        if(coinAmount==null){
            coinValue = coinValue(coinName,coinValueUrl,head);

        }else{
            if ( (StringUtils.isBlank(coinAmount.value) || coinAmount.time < System.currentTimeMillis()-5*1000*60) && !coinFlag){
                new Thread(() -> {
                    coinFlag = true;
                    coinValue(coinName,coinValueUrl,head);
                    coinFlag = false;
                }).start();
            }
            coinValue=coinAmount.getValue();
        }
        return coinValue;
    }

    public static String getPoolChane(String url,String prefix){
        ParamConstans poolJsonParam = ParamConstans.getParamConstans("poolJson");
        String poolJson = null;
        if(poolJsonParam == null){
            poolJson = getPool(url,prefix);
        }else{
            if ( (StringUtils.isBlank(poolJsonParam.value) || poolJsonParam.time < System.currentTimeMillis()-5*1000*60) && !poolFlag ){
                new Thread(() -> {
                    poolFlag = true;
                    getPool(url,prefix);
                    poolFlag = false;
                }).start();
            }
            poolJson=poolJsonParam.getValue();
        }
        return poolJson;
    }

    public static String getCommunityChane(String url,String prefix){
        ParamConstans communityPoolParam=ParamConstans.getParamConstans("communityPool");
        String communityPool = null;
        if(communityPoolParam == null){
            communityPool = getCommunity(url,prefix);
        }else{
            if ( (StringUtils.isBlank(communityPoolParam.value) || communityPoolParam.time < System.currentTimeMillis()-5*1000*60) && !communityFlag){
                new Thread(() -> {
                    communityFlag = true;
                    getCommunity(url,prefix);
                    communityFlag = false;
                }).start();
            }
            communityPool =communityPoolParam.getValue();
        }
        return communityPool;
    }

    public static String getInflationChane(String url,String prefix){
        String inflation = null;
        ParamConstans inflationParam=ParamConstans.getParamConstans("inflation");
        if(inflationParam == null){
            inflation = getInflation(url,prefix);
        }else {
            if ( (StringUtils.isBlank(inflationParam.value) || inflationParam.time < System.currentTimeMillis()-5*1000*60) && !inflationFlag){
                new Thread(() -> {
                    inflationFlag = true;
                    getInflation(url,prefix);
                    inflationFlag = false;
                }).start();
            }
            inflation=inflationParam.getValue();
        }
        return inflation;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public static String getInflation(String url,String prefix){
        String inflation = CosmosUtil.getInflation(url + "/" + prefix);
        ParamConstans paramConstans = new ParamConstans();
        paramConstans.setTime(System.currentTimeMillis());
        paramConstans.setValue(inflation);
        ParamConstans.put(paramConstans,"inflation");
        return inflation;
    }

    public static String getCommunity(String url,String prefix){
        String communityPool = CosmosUtil.getCommunity(url+"/"+prefix);
        ParamConstans paramConstans=new ParamConstans();
        paramConstans.setTime(System.currentTimeMillis());
        paramConstans.setValue(communityPool);
        ParamConstans.put(paramConstans,"communityPool");
        return communityPool;
    }

    public static String getPool(String url,String prefix){
        String poolJson= CosmosUtil.getPool(url+"/"+prefix);
        ParamConstans paramConstans=new ParamConstans();
        paramConstans.setTime(System.currentTimeMillis());
        paramConstans.setValue(poolJson);
        ParamConstans.put(paramConstans,"poolJson");
        return poolJson;
    }

    public static String getTotal(String url,String prefix,String coinName){
        String total = CosmosUtil.getTotal(url +"/"+prefix,coinName);
        ParamConstans paramConstans=new ParamConstans();
        paramConstans.setTime(System.currentTimeMillis());
        paramConstans.setValue(total);
        ParamConstans.put(paramConstans,"totalSupply");
        return total;
    }

    public static String coinValue(String coinName,String coinValueUrl,String head){
        String coinValue = null;
        if (!coinName.equalsIgnoreCase("usdg")){
            String a = getCoinValue(coinName.toUpperCase(),coinValueUrl,head);
            JSONObject data = null;
            try {
                data = JSON.parseObject(a);
            }catch (Exception e){

            }
            log.info("data"+data);
            if (null != data && data.containsKey("code") && data.getIntValue("code") == 200){
                String value = data.getJSONArray("data").getJSONObject(0).getString("tokenAmount");
                if (null != value){
                    coinValue = value;
                }
            }

            if (coinValue == null){
                coinValue = dbCoinValue;
            }
        }else {
            coinValue = "1";
        }
        ParamConstans paramConstans=new ParamConstans();
        paramConstans.setTime(System.currentTimeMillis());
        paramConstans.setValue(coinValue);
        ParamConstans.put(paramConstans,"coinAmount");
        return coinValue;
    }

    public static String getCoinValue(String coinName,String coinValueUrl,String head) {
        String uri = coinValueUrl;
        log.info("coinValueUrl"+uri);
        List<String> list = new ArrayList<>();
        list.add(coinName);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("tokenList",list);
        String data = null;
        try {
            data = HttpUtils.sendPostDataByJson(uri,JSON.toJSONString(jsonObject),head);
        }catch (Exception e){
            log.info("获取币种价格失败-----------");
        }
        return data;
    }
}
