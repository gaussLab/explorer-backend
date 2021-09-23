package com.wuyuan.database.sevice;

import com.alibaba.fastjson.JSONObject;
import com.wuyuan.database.util.Collocation;
import com.wuyuan.database.util.ConfigUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.util.List;

@Slf4j
@Service
public class ETHBatchService {

    @Resource
    private MongoTemplate mongoTemplate;

    private String collocationName;

    private String ConfirmBatch = "ConfirmBatch";

    public void saveBatch(JSONObject tx){
        mongoTemplate.save(tx,getCollocationName());
    }

    public long isExistBatch(String hash){
        Query query = new Query(Criteria.where("hash").is(hash));
        return mongoTemplate.count(query.limit(1),getCollocationName());
    }

    public long isExistByNonce(long batchNonce){
        Query query = new Query(Criteria.where("batchNonce").is(batchNonce));
        return mongoTemplate.count(query.limit(1),getCollocationName());
    }

    public JSONObject getConfirmBatch(long batchNonce){
        Criteria criteria = new Criteria();
        Criteria batch = Criteria.where("batchNonce").is(batchNonce);
        Criteria type = Criteria.where("type").is(ConfirmBatch);
        criteria.andOperator(batch,type);
        Query query = new Query(criteria);
        return mongoTemplate.findOne(query,JSONObject.class,getCollocationName());
    }

    public void deleteBatch(long batchNonce){
        Query query = new Query(Criteria.where("batchNonce").is(batchNonce));
        mongoTemplate.remove(query,getCollocationName());
    }

    public String getCollocationName(){
        if (collocationName == null){
            collocationName = Collocation.COLLECTION_ETH_BATCH;
        }
        return collocationName;
    }


}
