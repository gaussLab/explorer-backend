package com.wuyuan.database.sevice;

import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.result.UpdateResult;
import com.wuyuan.database.util.Collocation;
import com.wuyuan.database.util.ConfigUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class TokenService {
    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private ConfigService configService;

//    private String chainName;
//
//    private String coinName;

    private String collectionName;

    public void saveToken(JSONObject token){
        mongoTemplate.save(token,getCollectionName());
    }

    public UpdateResult updateToken(String symbol,JSONObject token){
        Query query = new Query(Criteria.where("symbol").is(symbol));
        JSONObject tokens = mongoTemplate.findOne(query,JSONObject.class,getCollectionName());
        if (tokens != null){
            query = new Query(Criteria.where("_id").is(tokens.getString("_id")));
            Update update = new Update();
            update.set("mintable",token.getBoolean("mintable"));
            update.set("owner",token.getString("owner"));
            update.set("initial_supply",token.getString("initial_supply"));
            return mongoTemplate.updateFirst(query,update,getCollectionName());
        }
        return null;
    }

    public JSONObject getTokenByUnit(String smallUnit){
        Query query = new Query(Criteria.where("smallest_unit").is(smallUnit));
        return mongoTemplate.findOne(query,JSONObject.class,getCollectionName());
    }

    public List<JSONObject> getTokenByAddress(String address){
        Query query = new Query(Criteria.where("owner").is(address));
        return mongoTemplate.find(query,JSONObject.class,getCollectionName());
    }

    public JSONObject getTokenBySymbol(String symbol){
        Query query = new Query(Criteria.where("symbol").is(symbol));
        return mongoTemplate.findOne(query,JSONObject.class,getCollectionName());
    }

    public Long isExistValidator(String symbol){
        Query query = new Query(Criteria.where("symbol").is(symbol));
        return mongoTemplate.count(query.limit(1),getCollectionName());
    }

    public String getCollectionName(){
        if (collectionName == null){
            collectionName = configService.getChainName() + "_" + Collocation.collection_token;
        }
        return collectionName;
    }
//
//    public String getChainName(){
//        if (chainName == null){
//            chainName = configService.getConfig(ConfigUtil.chainNameKey);
//        }
//        return chainName;
//    }
//
//    public String getCoinName(){
//        if (coinName == null){
//            coinName = configService.getConfig(ConfigUtil.coinNameKey);
//        }
//        return coinName;
//    }
}
