package com.wuyuan.database.sevice;


import com.alibaba.fastjson.JSONObject;
import com.wuyuan.database.util.Collocation;
import com.wuyuan.database.util.PageModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
public class CrossChainService {
    @Resource
    private MongoTemplate mongoTemplate;

    public PageModel<JSONObject> getCrossList(Integer pageIndex,Integer pageSize,String chainName,String address,Long id,String status){
        Query query = new Query();
        Criteria criteria = new Criteria();
        if (StringUtils.isNotBlank(chainName)){
            Criteria crossIn = Criteria.where("toChain").is(chainName.toLowerCase());
            Criteria crossOut = Criteria.where("fromChain").is(chainName.toLowerCase());
            criteria.orOperator(crossIn,crossOut);
        }

        if (StringUtils.isNotBlank(address)){
            Criteria cr = new Criteria();
            Criteria from = Criteria.where("from").is(address);
            Criteria to = Criteria.where("to").is(address);
            cr.orOperator(from,to);
            criteria.andOperator(cr);
        }

        if (StringUtils.isNotBlank(status)){
            query.addCriteria(Criteria.where("status").is(status));
        }
        if (id != null){
            query.addCriteria(Criteria.where("id").lte(id));
        }
        query.addCriteria(criteria);
        long count = 0;
        if (StringUtils.isBlank(chainName) && StringUtils.isBlank(address) && id == null && StringUtils.isBlank(status)){
            count = mongoTemplate.getCollection(Collocation.collection_transaction).estimatedDocumentCount();
        }else {
            count = mongoTemplate.count(query,Collocation.collection_transaction);
        }
        query.skip((pageIndex - 1) * pageSize);
        query.limit(pageSize);
        query.with(Sort.by(
                Sort.Order.desc("_id"))
        );
        List<JSONObject> list = mongoTemplate.find(query,JSONObject.class, Collocation.collection_transaction);
        PageModel<JSONObject> pageModel=new PageModel<>(list,pageIndex,pageSize,count);
        return pageModel;
    }

    public JSONObject getCrossTxBySequence(String packetSequence,String hash, String chainName){
        Query query = new Query(Criteria.where("packet_sequence").is(packetSequence));
        if (StringUtils.isNotBlank(hash)){
                query.addCriteria(Criteria.where("fromHash").is(hash));
        }
        return mongoTemplate.findOne(query,JSONObject.class,Collocation.collection_transaction);

    }

    public PageModel<JSONObject> getCrossListBySequence(String packetSequence, String chainName,Integer pageIndex,Integer pageSize){
        Criteria criteria = new Criteria();
        Criteria from = Criteria.where("fromChain").is(chainName.toLowerCase());
        Criteria to = Criteria.where("toChain").is(chainName.toLowerCase());
        criteria.orOperator(from,to);
        Query query = new Query(Criteria.where("packet_sequence").is(packetSequence).andOperator(criteria));
        long count = mongoTemplate.count(query,Collocation.collection_transaction);
        query.skip((pageIndex - 1) * pageSize);
        query.limit(pageSize);
        query.with(Sort.by(
                Sort.Order.desc("_id"))
        );
        List<JSONObject> list = mongoTemplate.find(query,JSONObject.class,Collocation.collection_transaction);
        PageModel<JSONObject> pageModel = new PageModel<>(list,pageIndex,pageSize,count);
        return pageModel;
    }

    public JSONObject getCrossTxByHash(String hash){
        Criteria criteria = new Criteria();
        Criteria crossFrom = Criteria.where("fromHash").is(hash);
        Criteria crossTo = Criteria.where("toHash").is(hash);
        criteria.orOperator(crossFrom,crossTo);

//        Criteria criteri2 = new Criteria();
//        Criteria chainFrom = Criteria.where("fromChain").is(chainName);
//        Criteria chainTo = Criteria.where("toChain").is(chainName);
//        criteri2.orOperator(chainFrom,chainTo);
        Query query = new Query(criteria);
//        query.addCriteria(criteri2);
        return mongoTemplate.findOne(query,JSONObject.class,Collocation.collection_transaction);
    }
}
