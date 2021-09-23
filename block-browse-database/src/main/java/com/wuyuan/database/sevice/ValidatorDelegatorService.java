package com.wuyuan.database.sevice;

import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.result.UpdateResult;
import com.wuyuan.database.util.Collocation;
import com.wuyuan.database.util.ConfigUtil;
import com.wuyuan.database.util.PageModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class ValidatorDelegatorService {
    @Resource
    private MongoTemplate mongoTemplate;

//    private String chainName;

    private String collectionName;

    private String unCollectionName;

    @Resource
    private ConfigService configService;

    public void saveDelegator(JSONObject json){
        mongoTemplate.save(json,getCollectionName());
    }

    public void saveUnDelegator(JSONObject json){
        mongoTemplate.save(json,getUnCollectionName());
    }

    public void deleteDelegator(String operatorAddress){
        Query query = new Query(Criteria.where("delegation.validator_address").is(operatorAddress));
        mongoTemplate.remove(query,getCollectionName());
    }

    public void deleteDelegatorByAddress(String address,String operatorAddress){
        Criteria criteria = new Criteria();
        Criteria delegator = Criteria.where("delegation.delegator_address").is(address);
        Criteria validator = Criteria.where("delegation.validator_address").is(operatorAddress);
        criteria.andOperator(delegator,validator);
        Query query = new Query(criteria);
        mongoTemplate.remove(query,getCollectionName());
    }

    public void deleteUnDelegator(String operatorAddress){
        Query query = new Query(Criteria.where("validator_address").is(operatorAddress));
        mongoTemplate.remove(query,getUnCollectionName());
    }

    public long getDelegatorCount(String operatorAddress){
        Query query = new Query(Criteria.where("delegation.validator_address").is(operatorAddress));
        return mongoTemplate.count(query,getCollectionName());
    }

    public PageModel<JSONObject> getDelegatorByOperatorAddress(String operatorAddress, Integer pageIndex, Integer pageSize){
        Query query = new Query(Criteria.where("delegation.validator_address").is(operatorAddress));
        long count = mongoTemplate.count(query,getCollectionName());
        query.skip((pageIndex - 1) * pageSize);
        query.limit(pageSize);
        List<JSONObject> delegatorList = mongoTemplate.find(query,JSONObject.class,getCollectionName());
        delegatorList.stream().forEach( delegator -> {
            if (configService.getChainName().equals("usdg")){
                delegator.put("chainName","dga");
            }else {
                delegator.put("chainName",configService.getChainName());
            }
        });
        PageModel<JSONObject> pageModel = null;
        String chainName = configService.getChainName();
        if ("usdg".equals(chainName)){
            pageModel = new PageModel<>(delegatorList,pageIndex,pageSize,count,"dga");
        }else {
            pageModel = new PageModel<>(delegatorList,pageIndex,pageSize,count,chainName);
        }
        return pageModel;
    }

    public PageModel<JSONObject> getUnDelegatorByOperatorAddress(String operatorAddress, Integer pageIndex, Integer pageSize){
        Query query = new Query(Criteria.where("validator_address").is(operatorAddress));
        long count = mongoTemplate.count(query,getUnCollectionName());
        query.skip((pageIndex - 1) * pageSize);
        query.limit(pageSize);
        List<JSONObject> unDelegatorList = mongoTemplate.find(query,JSONObject.class,getUnCollectionName());
        unDelegatorList.stream().forEach( unDelegator -> {
            if (configService.getChainName().equals("usdg")){
                unDelegator.put("chainName","dga");
            }else {
                unDelegator.put("chainName",configService.getChainName());
            }
        });
        PageModel<JSONObject> pageModel = null;
        String chainName = configService.getChainName();
        if ("usdg".equals(chainName)){
            pageModel = new PageModel<>(unDelegatorList,pageIndex,pageSize,count,"dga");
        }else {
            pageModel = new PageModel<>(unDelegatorList,pageIndex,pageSize,count,chainName);
        }
        return pageModel;
    }

    public String getCollectionName(){
        if (collectionName == null){
            collectionName = configService.getChainName() + "_" + Collocation.collection_delegator;
        }
        return collectionName;
    }

//    public String getChainName(){
//        if (chainName == null){
//            chainName = configService.getConfig(ConfigUtil.chainNameKey);
//        }
//        return chainName;
//    }

    public String getUnCollectionName(){
        if (unCollectionName == null){
            unCollectionName = configService.getChainName() + "_" + Collocation.collection_un_delegator;
        }
        return unCollectionName;
    }

    public long isExist(String address,String validatorAddress) {
        Criteria criteria = new Criteria();
        Criteria delegator = Criteria.where("delegation.delegator_address").is(address);
        Criteria validator = Criteria.where("delegation.validator_address").is(validatorAddress);
        criteria.andOperator(delegator,validator);

        Query query = new Query(criteria);
        return mongoTemplate.count(query.limit(1),getCollectionName());
    }

    public JSONObject getDelegator(String address,String validatorAddress){
        Criteria criteria = new Criteria();
        Criteria delegator = Criteria.where("delegation.delegator_address").is(address);
        Criteria validator = Criteria.where("delegation.validator_address").is(validatorAddress);
        criteria.andOperator(delegator,validator);
        Query query = new Query(criteria);
        return mongoTemplate.findOne(query,JSONObject.class,getCollectionName());
    }

    public long isExistUn(String address,String validatorAddress) {
        Criteria criteria = new Criteria();
        Criteria delegator = Criteria.where("delegator_address").is(address);
        Criteria validator = Criteria.where("validator_address").is(validatorAddress);
        criteria.andOperator(delegator,validator);

        Query query = new Query(criteria);
        return mongoTemplate.count(query.limit(1),getUnCollectionName());
    }

//    public JSONObject getDelegator(String address,String validatorAddress){
//        Criteria criteria = new Criteria();
//        Criteria delegator = Criteria.where("delegation.delegator_address").is(address);
//        Criteria validator = Criteria.where("delegation.validator_address").is(validatorAddress);
//        criteria.andOperator(delegator,validator);
//        Query query = new Query(criteria);
//        return mongoTemplate.findOne(query,JSONObject.class,getCollectionName());
//    }
}
