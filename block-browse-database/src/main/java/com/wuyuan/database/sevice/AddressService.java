package com.wuyuan.database.sevice;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.BasicDBObject;
import com.mongodb.client.result.UpdateResult;
import com.wuyuan.database.util.*;
import io.cosmos.util.BalanceUtil;
import io.cosmos.util.CosmosUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AddressService {
    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private TransactionService transactionService;

    @Resource
    private ConfigService configService;

    @Resource
    private TokenService tokenService;

    @Resource
    private ValidatorService validatorService;

    private String collocationName;

//    private String chainName;

    private String totalName;

//    private String chainPrefix;

//    private String coinName;


    public synchronized Long isExistAddress(String address){
        Query query = new Query(Criteria.where("address").is(address));
        return mongoTemplate.count(query.limit(1),getCollocationName());
    }

    public String getAddressBlockNumber(String address){
        Query query = new Query(Criteria.where("address").is(address));
        query.fields().include("newBlockNum");
        return mongoTemplate.findOne(query,JSONObject.class,getCollocationName()).getString("newBlockNum");
    }

    public synchronized void saveAddress(JSONObject addressJson){
        mongoTemplate.save(addressJson,getCollocationName());
    }

    public JSONObject getAddressByAddress(String address){
        Query query = new Query(Criteria.where("address").is(address));
        return mongoTemplate.findOne(query,JSONObject.class,getCollocationName());
    }

    public UpdateResult updateAddress(String address,JSONObject addressJson){
        Query query = new Query(Criteria.where("address").is(address));
        JSONObject jsonObject = mongoTemplate.findOne(query,JSONObject.class,getCollocationName());
        if (jsonObject != null){
            query = new Query(Criteria.where("_id").is(jsonObject.getString("_id")));
            Update update = new Update();
            update.set("balances",addressJson.getJSONArray("balances"));
            if (StringUtils.isNotBlank(addressJson.getString("newBlockNum"))){
                update.set("newBlockNum",addressJson.getString("newBlockNum"));
            }
            update.set("delegator_tx",addressJson.getJSONArray("delegator_tx"));
            update.set("unbonding_tx",addressJson.getJSONObject("unbonding_tx"));
            update.set("delegator_balance",addressJson.getString("delegator_balance"));
            update.set("unBonding_balance",addressJson.getString("unBonding_balance"));
            update.set("amount",addressJson.getString("amount"));
            update.set("totalAmount",addressJson.getBigDecimal("totalAmount"));
            return mongoTemplate.updateFirst(query,update,getCollocationName());
        }
        return null;
    }

    public UpdateResult updateAddressTotal(String address,JSONObject addressJson){
        Query query = new Query(Criteria.where("address").is(address));
        JSONObject jsonObject = mongoTemplate.findOne(query,JSONObject.class,getCollocationName());
        if (jsonObject != null){
            query = new Query(Criteria.where("_id").is(jsonObject.getString("_id")));
            Update update = new Update();
            update.set("totalAmount",addressJson.getBigDecimal("totalAmount"));
            return mongoTemplate.updateFirst(query,update,getCollocationName());
        }
        return null;
    }

    public void updateBalance(){
        Query query = new Query();
        List<JSONObject> addressList = mongoTemplate.find(query,JSONObject.class,getCollocationName());
        addressList.stream().forEach( address ->{
            String walletAddress = address.getString("address");
            if (address.getString("amount") == null){
                JSONArray balance = address.getJSONArray("balances");
                for (int i = 0; i < balance.size(); i++) {
                    if (configService.getCoinName().equals(balance.getJSONObject(i).getString("denom"))){
                        String amount = balance.getJSONObject(i).getString("amount");
                        if (amount != null){
                            address.put("amount",new BigDecimal(amount).divide(new BigDecimal(1000000)).toPlainString());
                            updateAddress(walletAddress,address);
                        }else {
                            address.put("amount",BigDecimal.ZERO.toPlainString());
                            updateAddress(walletAddress,address);
                        }
                    }
                }
            }
        });
    }

    public JSONObject getAddressInfo(String address){
        Query query = new Query(Criteria.where("address").is(address));
        query.fields().exclude("pagination");
        JSONObject addressJson = mongoTemplate.findOne(query,JSONObject.class,getCollocationName());
        JSONObject threadJson = addressJson;
        BigDecimal rewards = BigDecimal.ZERO;
        if (addressJson != null){
            if (validatorService.isExistValidatorBySelfAddress(address) == 0){
                rewards = BalanceUtil.getRewardBalance(address,configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix());
            }else {
                JSONObject validator = validatorService.getValidatorBySelfDelegateAddress(address);
                String validatorAddress = validator.getString("operator_address");
                rewards = BalanceUtil.getValidatorRewardBalance(validatorAddress,configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix(),configService.getCoinName());
            }

            addressJson.put("coinName",configService.getChainName());
            addressJson.put("chainName",configService.getChainName());
            JSONArray tokenJson = new JSONArray();
            JSONArray transferJson = new JSONArray();

            addressJson.getJSONArray("balances").stream().forEach(balance ->{
                LinkedHashMap<String,Object> b = (LinkedHashMap<String, Object>) balance;
                String denom = b.get("denom").toString();
                if (StringUtils.isBlank(denom)){
                    return;
                }
                if (denom.equals(configService.getCoinName())){

                    String amount = b.get("amount").toString();
                    String money = new BigDecimal(amount).divide(new BigDecimal(1000000)).toPlainString();
                    addressJson.put("balance",money);
                    new Thread(){
                        @Override
                        public void run() {
                            JSONObject balance = CosmosUtil.getAllBalance(configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix(),address);
                            if (balance == null){
                                return;
                            }
                            JSONArray balances = balance.getJSONArray("balances");
                            threadJson.put("balances",balances);
                            threadJson.put("amount",money);
                            String delegateBalance = threadJson.getString("delegator_balance");
                            String unBondingBalance = threadJson.getString("unBonding_balance");
                            BigDecimal totalAmount = BigDecimal.ZERO;
                            if (!configService.getCoinName().equals("uusdg")){
                                totalAmount = new BigDecimal(money).add(new BigDecimal(delegateBalance)).add(new BigDecimal(unBondingBalance));
                            }else {
                                totalAmount = new BigDecimal(money);
                            }
                            threadJson.put("totalAmount",totalAmount);
                            saveAddress(threadJson);
                        }
                    }.start();

                }else if(denom.equals("udga")){
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("tokenName","dga");
                    jsonObject.put("amount",new BigDecimal(b.get("amount").toString()).divide(new BigDecimal(1000000)).toPlainString());
                    tokenJson.add(jsonObject);
                }else if (denom.startsWith("ibc/")){
                    JSONObject jsonObject = new JSONObject();
                    String hash = denom.substring(4);
                    String result = CosmosUtil.getTransferTokenByHash(configService.getConfig(ConfigUtil.chainUrlKey),hash);
                    if (result != null){
                        JSONObject denomTrace = JSON.parseObject(result);
                        String coinName = denomTrace.getJSONObject("denom_trace").getString("base_denom");
                        jsonObject.put("amount",b.get("amount").toString());
                        jsonObject.put("coinName",coinName);
                        transferJson.add(jsonObject);
                    }else {
                        jsonObject.put("amount",b.get("amount").toString());
                        jsonObject.put("coinName",denom);
                        transferJson.add(jsonObject);
                    }

                }else {
                    JSONObject jsonObject = new JSONObject();
                    String tokenName = b.get("denom").toString();
                    JSONObject tokens = tokenService.getTokenByUnit(tokenName);
                    if (tokens != null && tokens.getInteger("decimals") != null){
                        jsonObject.put("tokenName",tokens.getString("symbol"));
                        jsonObject.put("amount",new BigDecimal(b.get("amount").toString()).divide(new BigDecimal(Math.pow(10,tokens.getIntValue("decimals")))).toPlainString());
                    }else {
                        jsonObject.put("tokenName",tokenName);
                        jsonObject.put("amount",b.get("amount").toString());
                    }
                    tokenJson.add(jsonObject);
                }
            });
            if (StringUtils.isBlank(addressJson.getString("balance"))){
                addressJson.put("balance","0");
            }
            addressJson.put("transferBalances",transferJson);
            addressJson.put("tokenBalances",tokenJson);
            addressJson.put("rewards",rewards.toPlainString());
            return addressJson;
        }
        return addressJson;
    }

    public List<JSONObject> getAddress(Integer pageIndex,Integer pageSize){
        Query query = new Query();
        query.skip((pageIndex - 1) * pageSize);
        query.limit(pageSize);
        return mongoTemplate.find(query,JSONObject.class,getCollocationName());
    }

    public PageModel<JSONObject> getAddressByAssets(Integer pageIndex,Integer pageSize){
        if (pageIndex == null || pageIndex == 0){
            pageIndex = 1;
        }
        if (pageSize == null || pageSize == 0){
            pageSize = 20;
        }
        Document document = Collation.of("zh").toDocument();
        document.put("numericOrdering",true);
        Query query = new Query();
        query.fields().exclude("pagination");
        long count = mongoTemplate.count(query,getCollocationName());
        query.skip((pageIndex - 1) * pageSize);
        query.limit(pageSize);
        query.collation(Collation.from(document));
        query.with(Sort.by(
                Sort.Order.desc("totalAmount"))
        );
        List<JSONObject> addressList = mongoTemplate.find(query,JSONObject.class,getCollocationName());
//        String totalJson = getTotalAmountSum();
//        String totalCoin = null;
//        if (totalJson != null){
//            totalCoin = totalJson.getString("total");
//            new Thread(){
//                @Override
//                public void run() {
//                    String total = CosmosUtil.getTotal(configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix(),configService.getCoinName());
//                    saveTotalSupply(total);
//                }
//            }.start();
//        }else {
//            String total = CosmosUtil.getTotal(configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix(),configService.getCoinName());
//            saveTotalSupply(total);
//            totalCoin = total;
//        }

        String finalTotalCoin = getTotalAmountSum();
        List<JSONObject> safeAddress = Collections.synchronizedList(addressList);
        safeAddress.parallelStream().forEach(address -> {
            String amount = address.getString("totalAmount");
            String rate = new BigDecimal(amount).divide(new BigDecimal(finalTotalCoin),6, RoundingMode.HALF_UP).toPlainString();
            address.put("amount",amount);
            address.put("coinName",configService.getChainName());
            address.put("chainName",configService.getChainName());
            address.put("rate",rate);
        });
        PageModel<JSONObject> pageModel=new PageModel<>(addressList,pageIndex,pageSize,count,configService.getChainName());
        return pageModel;
    }

    public JSONObject getCoinRange(){
        //todo 优化
        List<JSONObject> addressList = getAddressByAssets(1,1000).getRecords();
        JSONObject coinRange = new JSONObject();
        BigDecimal topFive = BigDecimal.ZERO;
        BigDecimal topFiveCoin = BigDecimal.ZERO;
        BigDecimal topTen = BigDecimal.ZERO;
        BigDecimal topTenCoin = BigDecimal.ZERO;
        BigDecimal topFifty = BigDecimal.ZERO;
        BigDecimal topFiftyCoin = BigDecimal.ZERO;
        BigDecimal topHun = BigDecimal.ZERO;
        BigDecimal topHunCoin = BigDecimal.ZERO;
        BigDecimal topFiveHun = BigDecimal.ZERO;
        BigDecimal topFiveHunCoin = BigDecimal.ZERO;
        BigDecimal topThousand = BigDecimal.ZERO;
        BigDecimal topThousandCoin = BigDecimal.ZERO;

        addressList.stream().forEach( balance -> {
            String amount = balance.getString("amount");
            if (amount == null){
                balance.put("amount",BigDecimal.ZERO.toPlainString());
            }
        });
        for (int i = 0; i < addressList.size(); i++) {
            if (i < 5){
                topFive = topFive.add(new BigDecimal(addressList.get(i).getString("rate")));
                topFiveCoin = topFiveCoin.add(new BigDecimal(addressList.get(i).getString("amount")));
            }else if (i >= 5 && i < 10){
                topTen = topTen.add(new BigDecimal(addressList.get(i).getString("rate")));
                topTenCoin = topTenCoin.add(new BigDecimal(addressList.get(i).getString("amount")));
            }else if (i >= 10 && i < 50){
                topFifty = topFifty.add(new BigDecimal(addressList.get(i).getString("rate")));
                topFiftyCoin = topFiftyCoin.add(new BigDecimal(addressList.get(i).getString("amount")));
            }else if (i >= 50 && i < 100){
                topHun = topHun.add(new BigDecimal(addressList.get(i).getString("rate")));
                topHunCoin = topHunCoin.add(new BigDecimal(addressList.get(i).getString("amount")));
            }else if (i >= 100 && i < 500){
                topFiveHun = topFiveHun.add(new BigDecimal(addressList.get(i).getString("rate")));
                topFiveHunCoin = topFiveHunCoin.add(new BigDecimal(addressList.get(i).getString("amount")));
            }else if (i >= 500 && i < 1000){
                topThousand = topThousand.add(new BigDecimal(addressList.get(i).getString("rate")));
                topThousandCoin = topThousandCoin.add(new BigDecimal(addressList.get(i).getString("amount")));
            }
        }

        BigDecimal allcoin = topFiveCoin.add(topTenCoin).add(topFiftyCoin).add(topHunCoin).add(topFiveHunCoin).add(topThousandCoin);

        String result = getTotalAmountSum();
        BigDecimal otherAddressCoin = new BigDecimal(
                result )
                .subtract(allcoin);
        BigDecimal otherAddress = otherAddressCoin.divide(new BigDecimal(result),6,RoundingMode.HALF_UP);

        if (otherAddressCoin.compareTo(BigDecimal.ZERO) == -1){
           BigDecimal total = new BigDecimal(result).add(new BigDecimal(-1).multiply(otherAddressCoin).setScale(6));
           saveTotalSupply(total.toPlainString());
           otherAddressCoin = BigDecimal.ZERO;
           otherAddress = BigDecimal.ZERO;
        }
        Long totalNum = getAddressCount();
        int a = ( (totalNum>0) == true?1:0) + ((totalNum > 5) == true ? 1:0) + ((totalNum > 10) == true ? 1:0) + ((totalNum > 50) == true ? 1:0)
                + ((totalNum > 100) == true ? 1: 0) + ((totalNum > 500) == true ? 1:0) + ((totalNum > 1000) == true ? 1:0);

        switch (a){
            case 0:
                coinRange.put("otherNum",0);
                coinRange.put("topThousandNum",0);
                coinRange.put("topFiveHunNum",0);
                coinRange.put("topHunNum",0);
                coinRange.put("topFiftyNum",0);
                coinRange.put("topTenNum",0);
                coinRange.put("topFiveNum",0);
                break;
            case 1:
                coinRange.put("otherNum",0);
                coinRange.put("topThousandNum",0);
                coinRange.put("topFiveHunNum",0);
                coinRange.put("topHunNum",0);
                coinRange.put("topFiftyNum",0);
                coinRange.put("topTenNum",0);
                coinRange.put("topFiveNum",totalNum);
                break;
            case 2:
                coinRange.put("otherNum",0);
                coinRange.put("topThousandNum",0);
                coinRange.put("topFiveHunNum",0);
                coinRange.put("topHunNum",0);
                coinRange.put("topFiftyNum",0);
                coinRange.put("topTenNum",totalNum - 5);
                coinRange.put("topFiveNum",5);
                break;
            case 3:
                coinRange.put("otherNum",0);
                coinRange.put("topThousandNum",0);
                coinRange.put("topFiveHunNum",0);
                coinRange.put("topHunNum",0);
                coinRange.put("topFiftyNum",totalNum - 10);
                coinRange.put("topTenNum",5);
                coinRange.put("topFiveNum",5);
                break;
            case 4:
                coinRange.put("otherNum",0);
                coinRange.put("topThousandNum",0);
                coinRange.put("topFiveHunNum",0);
                coinRange.put("topHunNum",totalNum - 50);
                coinRange.put("topFiftyNum",40);
                coinRange.put("topTenNum",5);
                coinRange.put("topFiveNum",5);
                break;
            case 5:
                coinRange.put("otherNum",0);
                coinRange.put("topThousandNum",0);
                coinRange.put("topFiveHunNum",totalNum - 100);
                coinRange.put("topHunNum",50);
                coinRange.put("topFiftyNum",40);
                coinRange.put("topTenNum",5);
                coinRange.put("topFiveNum",5);
                break;
            case 6:
                coinRange.put("otherNum",0);
                coinRange.put("topThousandNum",totalNum -500);
                coinRange.put("topFiveHunNum",400);
                coinRange.put("topHunNum",50);
                coinRange.put("topFiftyNum",40);
                coinRange.put("topTenNum",5);
                coinRange.put("topFiveNum",5);
                break;
            case 7:
                coinRange.put("otherNum",totalNum - 1000);
                coinRange.put("topThousandNum",500);
                coinRange.put("topFiveHunNum",400);
                coinRange.put("topHunNum",50);
                coinRange.put("topFiftyNum",40);
                coinRange.put("topTenNum",5);
                coinRange.put("topFiveNum",5);
                break;
        }

        coinRange.put("topFive",topFive);
        coinRange.put("topTen",topTen);
        coinRange.put("topFifty",topFifty);
        coinRange.put("topHun",topHun);
        coinRange.put("topFiveHun",topFiveHun);
        coinRange.put("topThousand",topThousand);
        coinRange.put("otherAddress",otherAddress);
        coinRange.put("topFiveCoin",topFiveCoin);
        coinRange.put("topTenCoin",topTenCoin);
        coinRange.put("topFiftyCoin",topFiftyCoin);
        coinRange.put("topHunCoin",topHunCoin);
        coinRange.put("topFiveHunCoin",topFiveHunCoin);
        coinRange.put("topThousandCoin",topThousandCoin);
        coinRange.put("otherAddressCoin",otherAddressCoin.toPlainString());
        coinRange.put("coinName",configService.getChainName());
        coinRange.put("chainName",configService.getChainName());
        coinRange.put("totalNum",totalNum);
        return coinRange;
    }

    public PageModel<LinkedHashMap<String,Object>> getAddressDelegateInfo(String address,String type,Integer pageIndex,Integer pageSize){
        Query query = new Query(Criteria.where("address").is(address));
        Object a = mongoTemplate.findOne(query,Object.class,getCollocationName());
        long count = 0;
        JSONArray addressList = null;
        JSONObject addressInfo = JSON.parseObject(JSON.toJSONString(a));
        switch (type.trim().toLowerCase()){
            case "delegator":
                query.fields().slice("delegator_tx",(pageIndex - 1)*pageSize,pageSize);
                if (addressInfo != null){
                    count = addressInfo.getJSONArray("delegator_tx").size();
                }
                JSONObject addressJson = mongoTemplate.findOne(query,JSONObject.class,getCollocationName());
                if (addressJson != null){
                    addressList = addressJson.getJSONArray("delegator_tx");
                }
                break;
            case "undelegator":
                query.fields().slice("unbonding_tx.unbonding_responses",(pageIndex - 1)*pageSize,pageSize);
                if (addressInfo != null){
                    count = addressInfo.getJSONObject("unbonding_tx").getJSONArray("unbonding_responses").size();
                }
                addressList = mongoTemplate.findOne(query,JSONObject.class,getCollocationName()).getJSONObject("unbonding_tx").getJSONArray("unbonding_responses");
                break;
        }
        List<LinkedHashMap<String,Object>> txList = null;
        if (addressList != null && addressList.size() > 0){
            txList = addressList.stream().map( tx ->{

                LinkedHashMap<String,Object> txs = (LinkedHashMap<String, Object>) tx;
                if (((LinkedHashMap<?, ?>) tx).containsKey("entries")){
                    List<LinkedHashMap<String,Object>> entries = (List<LinkedHashMap<String, Object>>) txs.get("entries");
                    String validatorAddress = (String) txs.get("validator_address");
                    JSONObject moniker = transactionService.getValidatorMoniker(String.valueOf(validatorAddress));
                    txs.put("moniker",moniker);
                    if (entries.size() > 0){
                        BigDecimal value = BigDecimal.ZERO;
                        for (int i = 0; i < entries.size(); i++) {
                            value = value.add(new BigDecimal(entries.get(i).get("balance").toString()));
                            txs.put("endtime",entries.get(i).get("completion_time").toString());
                        }
                        txs.put("balance",value.divide(new BigDecimal(1000000)).toPlainString());
                    }
                    txs.put("coinName",configService.getChainName());
                    txs.put("chainName",configService.getChainName());
                }else {
                    LinkedHashMap<String,Object> validator = (LinkedHashMap<String, Object>) txs.get("delegation");
                    LinkedHashMap<String,Object> balance = (LinkedHashMap<String, Object>) txs.get("balance");
                    if (balance != null){
                        String amount = new BigDecimal(balance.get("amount").toString()).divide(new BigDecimal(1000000)).toPlainString();
                        balance.put("amount",amount);
                    }
                    String validatorAddress = (String) validator.get("validator_address");
                    JSONObject moniker = transactionService.getValidatorMoniker(String.valueOf(validatorAddress));
                    txs.put("moniker",moniker);
                    txs.put("balance",balance);
                    if (configService.getChainName().equals("usdg")){
                        txs.put("coinName","dga");
                    }else {
                        txs.put("coinName",configService.getChainName());
                    }
                    txs.put("chainName",configService.getChainName());
                }
                return txs;
            }).collect(Collectors.toList());
        }
        PageModel<LinkedHashMap<String,Object>> pageModel = null;
        if (configService.getChainName().equals("usdg")){
            pageModel = new PageModel<>(txList,pageIndex,pageSize,count,"dga");
        }else {
            pageModel = new PageModel<>(txList,pageIndex,pageSize,count,configService.getChainName());
        }
        return pageModel;
    }

    public JSONObject getCoinInfo(){
        String url =configService.getConfig(ConfigUtil.chainUrlKey);
        String chainPrefix = configService.getChainPrefix();

        JSONObject coinInfo = new JSONObject();
        String totalSupply = getTotalAmountSum();

        String poolJson = ParamConstans.getPoolChane(url,chainPrefix);
        JSONObject pool = JSON.parseObject(poolJson);
        String communityPool = ParamConstans.getCommunityChane(url,chainPrefix);
//        String inflation = CosmosUtil.getInflation(configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix());
        BigDecimal bonded = new BigDecimal(pool.getJSONObject("pool").getString("bonded_tokens")).divide(new BigDecimal("1000000"));
        BigDecimal notBonded = new BigDecimal(pool.getJSONObject("pool").getString("not_bonded_tokens")).divide(new BigDecimal("1000000"));

//        String onlineBonded = bonded.divide(new BigDecimal(totalSupply),6,RoundingMode.HALF_UP).toPlainString();
        String chainName = configService.getChainName();
        coinInfo.put("TotalSupply",totalSupply);
        if ("usdg".equals(chainName)) {
            coinInfo.put("CommunityPool",BigDecimal.ZERO);
            coinInfo.put("Bonded",BigDecimal.ZERO);
        }else {
            coinInfo.put("CommunityPool",communityPool);
            coinInfo.put("Bonded",bonded.toPlainString());
        }
//        coinInfo.put("onlineVotePower",onlineBonded);
        coinInfo.put("Circulation",getBalanceSum());
//        coinInfo.put("Inflation",inflation);
//        coinInfo.put("coinAmount",coinValue);
//        coinInfo.put("totalMarketValue",new BigDecimal(totalSupply).multiply(new BigDecimal(coinValue)).toPlainString());
        coinInfo.put("coinName",configService.getChainName());
        coinInfo.put("chainName",configService.getChainName());
        return coinInfo;
    }

    public JSONObject getHomePageCoinInfo(){
        JSONObject coinInfo = new JSONObject();

        String url =configService.getConfig(ConfigUtil.chainUrlKey);
        String chainName = configService.getChainName();
        String chainPrefix = configService.getChainPrefix();
        String coinName = configService.getCoinName();
        String dbCoinValue = configService.getConfig(ConfigUtil.coinValueKey);
        if (StringUtils.isNotBlank(dbCoinValue)){
            ParamConstans.dbCoinValue = dbCoinValue;
        }
        String coinValue = ParamConstans.getCoinValueChane(chainName.toUpperCase(),configService.getConfig(ConfigUtil.coinValueUrlKey),configService.getConfig(ConfigUtil.httpHeadKey));
        String totalSupply = ParamConstans.getTotalChane(url,chainPrefix,coinName);
        String poolJson = ParamConstans.getPoolChane(url,chainPrefix);
        JSONObject pool = JSON.parseObject(poolJson);
        String communityPool = ParamConstans.getCommunityChane(url,chainPrefix);
        String inflation = ParamConstans.getInflationChane(url,chainPrefix);

        BigDecimal bonded = new BigDecimal(pool.getJSONObject("pool").getString("bonded_tokens")).divide(new BigDecimal("1000000"));
        BigDecimal notBonded = new BigDecimal(pool.getJSONObject("pool").getString("not_bonded_tokens")).divide(new BigDecimal("1000000"));

        String onlineBonded = bonded.divide(new BigDecimal(totalSupply),6,RoundingMode.HALF_UP).toPlainString();
        coinInfo.put("TotalSupply",totalSupply);
        coinInfo.put("CommunityPool",communityPool);
        coinInfo.put("Bonded",bonded.toPlainString());
        coinInfo.put("onlineVotePower",onlineBonded);
        coinInfo.put("Circulation",bonded.add(notBonded).add(new BigDecimal(communityPool)).toPlainString());
        coinInfo.put("Inflation",inflation);
        coinInfo.put("coinAmount",coinValue);
        coinInfo.put("totalMarketValue",new BigDecimal(totalSupply).multiply(new BigDecimal(coinValue)).toPlainString());
        coinInfo.put("coinName",configService.getChainName());
        coinInfo.put("chainName",configService.getChainName());
        return coinInfo;
    }

    public JSONObject getTotal(){
        Query query = new Query(Criteria.where("id").is(1));
        JSONObject total = mongoTemplate.findOne(query,JSONObject.class,getTotalCollection());
        return total;
    }

    public Long getAddressCount(){
        Query query = new Query();
        return mongoTemplate.count(query,getCollocationName());
    }

    public void saveTotalSupply(String total){
        JSONObject totalJson = new JSONObject();
        totalJson.put("total",total);
        totalJson.put("id",1);
        if (isExistTotal(1) > 0){
            updateTotal(1,totalJson);
        }else {
            mongoTemplate.save(totalJson,getTotalCollection());
        }
    }

    public UpdateResult updateTotal(Integer id,JSONObject totalJson){
        Query query = new Query(Criteria.where("id").is(id));
        Update update = new Update();
        update.set("total",totalJson.getString("total"));
        return mongoTemplate.updateFirst(query,update,getTotalCollection());
    }

    public Long isExistTotal(Integer id){
        Query query = new Query(Criteria.where("id").is(id));
        return mongoTemplate.count(query.limit(1),getTotalCollection());
    }

    public String getCoinValue(String coinName) {
        String uri = configService.getConfig(ConfigUtil.coinValueUrlKey);
        log.info("url"+uri);
        List<String> list = new ArrayList<>();
        list.add(coinName);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("tokenList",list);
        String data = null;
        try {
            data = HttpUtils.sendPostDataByJson(uri,JSON.toJSONString(jsonObject),configService.getConfig(ConfigUtil.httpHeadKey));
        }catch (Exception e){
            log.info("获取币种价格失败-----------");
        }
        return data;
    }

    public String getTotalAmountSum(){

        Aggregation aggregation = Aggregation.newAggregation(Aggregation.group().count().as("count").sum(ConvertOperators.ToDecimal.toDecimal("$totalAmount")).as("sum"));
        AggregationResults<BasicDBObject> aggregateResult = mongoTemplate.aggregate(aggregation,getCollocationName(), BasicDBObject.class);
        List<BasicDBObject> results = aggregateResult.getMappedResults();

        log.info(results.toString());
        if (results == null || results.size() == 0){
            return "0";
        }
        return results.get(0).getString("sum");
    }

    public String getBalanceSum(){

        Aggregation aggregation = Aggregation.newAggregation(Aggregation.group().sum(ConvertOperators.ToDecimal.toDecimal("$amount")).as("sum"));
        AggregationResults<BasicDBObject> aggregateResult = mongoTemplate.aggregate(aggregation,getCollocationName(), BasicDBObject.class);
        List<BasicDBObject> results = aggregateResult.getMappedResults();
        log.info(results.toString());
        return results.get(0).getString("sum");
    }

    public String getTotalCollection(){
        if (totalName == null){
            totalName = configService.getChainName()+"_"+Collocation.collection_total;
        }
        return totalName;
    }

    public String getCollocationName(){
        if (collocationName == null){
            collocationName = configService.getChainName() + "_" + Collocation.collection_address;
        }
        return collocationName;
    }

}
