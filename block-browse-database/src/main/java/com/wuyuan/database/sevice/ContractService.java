package com.wuyuan.database.sevice;

import com.alibaba.fastjson.JSONObject;
import com.wuyuan.database.util.Collocation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Queue;

@Service
@Slf4j
public class ContractService {
    @Resource
    private MongoTemplate mongoTemplate;

    private String collectionName;

    public long isExistContract(String contractAddress){
        Query query = new Query(Criteria.where("contractAddress").is(contractAddress));
        return mongoTemplate.count(query.limit(1),getCollectionName());
    }

    public void saveContract(JSONObject contract){
        mongoTemplate.save(contract,getCollectionName());
    }

    public JSONObject getContract(String contractAddress){
        Query query = Query.query(Criteria.where("contractAddress").is(contractAddress));
        return mongoTemplate.findOne(query,JSONObject.class,getCollectionName());
    }

    public String getCollectionName(){
        if (collectionName == null){
            collectionName = Collocation.COLLECTION_CONTRACT;
        }
        return collectionName;
    }
}
