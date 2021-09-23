package com.wuyuan.database.sevice;

import com.alibaba.fastjson.JSONObject;
import com.wuyuan.database.util.Collocation;
import com.wuyuan.database.util.GetIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Slf4j
public class TxsQueueService {
    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private MongoSevice mongoSevice;

    private String collocationName;

    public Long isExist(String packetSequence,long timeOut){
        Criteria criteria = new Criteria();
        Criteria first = Criteria.where("first.event.packet_sequence").is(packetSequence);
        first.andOperator(Criteria.where("first.event.timeoutNum").is(timeOut));

        Criteria second = Criteria.where("second.event.packet_sequence").is(packetSequence);
        second.andOperator(Criteria.where("second.event.timeoutNum").is(timeOut));

        Criteria third = Criteria.where("third.event.packet_sequence").is(packetSequence);
        third.andOperator(Criteria.where("third.event.timeoutNum").is(timeOut));

        criteria.orOperator(first,second,third);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.count(query.limit(1),getCollocationName());
    }


    public JSONObject getTx(String packetSequence,long timeOut){
        Criteria criteria = new Criteria();
        Criteria first = Criteria.where("first.event.packet_sequence").is(packetSequence).andOperator(Criteria.where("first.event.timeoutNum").is(timeOut));
        Criteria second = Criteria.where("second.event.packet_sequence").is(packetSequence).andOperator(Criteria.where("second.event.timeoutNum").is(timeOut));
        Criteria third = Criteria.where("third.event.packet_sequence").is(packetSequence).andOperator(Criteria.where("third.event.timeoutNum").is(timeOut));
        criteria.orOperator(first,second,third);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query,JSONObject.class,getCollocationName());
    }

    public void save(JSONObject tx){
        mongoTemplate.save(tx,getCollocationName());
    }

    public String getCollocationName(){
        if (collocationName == null){
            collocationName = Collocation.collection_queue;
        }
        return collocationName;
    }

    @Transactional
    public void deleteQueueTx(String packetSequence){
        Query query = new Query(Criteria.where("first.event.packet_sequence").is(packetSequence));
        mongoTemplate.remove(query,getCollocationName());
    }

    public void dealQue(JSONObject queueTx){
        JSONObject first = queueTx.getJSONObject("first");
        JSONObject second = queueTx.getJSONObject("second");
        JSONObject third = queueTx.getJSONObject("third");
        if (second == null){
            return;
        }
        String isCanSave = second.getJSONObject("event").getString("type");
        if ("timeOut".equals(isCanSave) && first != null){
            String chainName = first.getJSONObject("event").getString("chainName");
            JSONObject cross = mongoSevice.getCrossTx(queueTx,chainName);
            cross.put("id", GetIdUtil.getId());
            mongoSevice.save(cross,Collocation.collection_transaction);
            deleteQueueTx(first.getJSONObject("event").getString("packet_sequence"));
            return;
        }

        if (first != null && second != null && third != null){
            String chainName = first.getJSONObject("event").getString("chainName");
            JSONObject cross = mongoSevice.getCrossTx(queueTx,chainName);
            cross.put("id", GetIdUtil.getId());
            mongoSevice.save(cross,Collocation.collection_transaction);
            deleteQueueTx(first.getJSONObject("event").getString("packet_sequence"));
        }
    }
}
