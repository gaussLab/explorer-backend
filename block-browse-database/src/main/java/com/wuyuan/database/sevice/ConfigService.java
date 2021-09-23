package com.wuyuan.database.sevice;

import com.alibaba.fastjson.JSONObject;
import com.wuyuan.database.util.Collocation;
import com.wuyuan.database.util.ConfigUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class ConfigService {

    @Resource
    private MongoTemplate mongoTemplate;

    private String collocationName;

    private String chainName;

    private String chainPrefix;

    private String coinName;

    private String bech32Prefix;

    public void saveConfig(JSONObject configJson){
        mongoTemplate.save(configJson,getCollection());
    }

    public synchronized Long isExistConfig(String key){
        Query query = new Query(Criteria.where("key").is(key));
        return mongoTemplate.count(query.limit(1),getCollection());
    }

    public String getConfig(String key){
        Query query = new Query(Criteria.where("key").is(key));
        query.fields().exclude("_id");
        JSONObject config = mongoTemplate.findOne(query,JSONObject.class,getCollection());
        if (config == null){
            return  null;
        }
        return config.getString("value");
    }

    public void updateConfig(String key,JSONObject configJson){
        Query query = new Query(Criteria.where("key").is(key));
        Update update = new Update();
        update.set("value",configJson.getString("value"));
        mongoTemplate.updateFirst(query,update,getCollection());
    }

    public String getCollection(){
        if (collocationName == null){
            collocationName = Collocation.collection_config;
        }
        return collocationName;
    }

    public String getChainName(){
        if (chainName == null){
            chainName = getConfig(ConfigUtil.chainNameKey);
        }
        return chainName;
    }

    public String getChainPrefix(){
        if (chainPrefix == null){
            chainPrefix = getConfig(ConfigUtil.chainPrefixKey);
        }
        return chainPrefix;
    }

    public String getCoinName(){
        if (coinName == null){
            coinName = getConfig(ConfigUtil.coinNameKey);
        }
        return coinName;
    }

    public String getBech32Prefix(){
        if (bech32Prefix == null){
            bech32Prefix = getConfig(ConfigUtil.bech32PrefixKey);
        }
        return bech32Prefix;
    }

}
