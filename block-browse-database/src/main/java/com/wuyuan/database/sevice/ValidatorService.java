package com.wuyuan.database.sevice;

import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.result.UpdateResult;
import com.wuyuan.database.entity.Block;
import com.wuyuan.database.util.Collocation;
import com.wuyuan.database.util.ConfigUtil;
import com.wuyuan.database.util.PageModel;
import io.cosmos.util.BalanceUtil;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class ValidatorService {

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private BlockService blockService;

    @Resource
    private ConfigService configService;
//
//    private String chainName;
//
//    private String chainPrefix;

    private String collectionName;

    public Long isExistValidator(String operatorAddress){
        Query query = new Query(Criteria.where("operator_address").is(operatorAddress));
        return mongoTemplate.count(query.limit(1),getCollectionName());
    }

    public Long isExistValidatorBySelfAddress(String address){
        Query query = new Query(Criteria.where("selfDelegateAddress").is(address));
        return mongoTemplate.count(query.limit(1),getCollectionName());
    }

    public JSONObject getValidatorBySelfDelegateAddress(String address){
        Query query = new Query(Criteria.where("selfDelegateAddress").is(address));
        query.fields().include("operator_address");
        query.fields().exclude("_id");
        return mongoTemplate.findOne(query,JSONObject.class,getCollectionName());
    }

    public UpdateResult updateValidator(String operatorAddress, JSONObject validator){
        Query query = new Query(Criteria.where("operator_address").is(operatorAddress));
        Update update = new Update();
        update.set("jailed",validator.getBoolean("jailed"));
        update.set("status",validator.getString("status"));
        update.set("tokens",validator.getString("tokens"));
        update.set("delegator_shares",validator.getString("delegator_shares"));
        update.set("unbonding_time",validator.getString("unbonding_time"));
        update.set("commission_rewards",validator.getString("commission_rewards"));
        String icon = validator.getString("icon");
        if (StringUtils.isNotBlank(icon)){
            update.set("icon",icon);
        }
        JSONObject commission = validator.getJSONObject("commission").getJSONObject("commission_rates");
        update.set("description",validator.getJSONObject("description"));
        update.set("selfDelegateAmount",validator.getString("selfDelegateAmount"));
        update.set("commission.commission_rates.rate",commission.getString("rate"));
        update.set("commission.commission_rates.max_rate",commission.getString("max_rate"));
        update.set("commission.commission_rates.max_change_rate",commission.getString("max_change_rate"));
        update.set("commission.update_time",validator.getJSONObject("commission").getString("update_time"));
        update.set("min_self_delegation",validator.getString("min_self_delegation"));
        update.set("totalDelegations",validator.getString("totalDelegations"));
        update.set("uptime",validator.getString("uptime"));
        update.set("val_signing_info",validator.getJSONObject("val_signing_info"));
        update.set("publicKey",validator.getString("publicKey"));
        update.set("address",validator.getString("address"));
        update.set("votingPower",validator.getString("votingPower"));
        return mongoTemplate.updateFirst(query,update,getCollectionName());
    }



    public void save(JSONObject json){
        mongoTemplate.save(json,getCollectionName());
    }

    //根据验证人的状态查询验证人（积极（BOND_STATUS_BONDED）,不积极（BOND_STATUS_UNBONDED）,已经入狱的）
    public PageModel<JSONObject> getValidatorsByStatus(String status,String pramsName,Integer sortMethod,int pageIndex,int pageSize){
        Query query = null;
        switch (status.toUpperCase()){
            case "BOND_STATUS_BONDED":
                query = new Query(Criteria.where("status").is(status.toUpperCase()).and("jailed").is(false));
                break;
            case "BOND_STATUS_UNBONDED":
                query = new Query(Criteria.where("status").ne("BOND_STATUS_BONDED").and("jailed").is(false));
                break;
            case "JAILED":
                query = new Query(Criteria.where("jailed").is(true));
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + status.toUpperCase());
        }
        if (sortMethod != null && sortMethod == 1){
            query.with(Sort.by(
                    Sort.Order.desc(pramsName)
            ));
        }else if (sortMethod != null && sortMethod == 0){
            query.with(Sort.by(
                    Sort.Order.asc(pramsName)
            ));
        }
        if (!"description.moniker".equalsIgnoreCase(pramsName)){
            Document document = Collation.of("zh").toDocument();
            document.put("numericOrdering",true);
            query.collation(Collation.from(document));
        }
        long count = mongoTemplate.count(query,getCollectionName());

        query.skip((pageIndex - 1) * pageSize);
        query.limit(pageSize);
        List<JSONObject> validatorsList = mongoTemplate.find(query,JSONObject.class,getCollectionName());
        PageModel<JSONObject> pageModel=new PageModel<>(validatorsList,pageIndex,pageSize,count,configService.getChainName());
        String chainName = configService.getChainName();
        if ("usdg".equals(chainName)){
            pageModel = new PageModel<>(validatorsList,pageIndex,pageSize,count,"dga");
        }else {
            pageModel = new PageModel<>(validatorsList,pageIndex,pageSize,count,chainName);
        }
        return pageModel;
    }

    public long getValidatorsCount(String status){
        Query query = new Query();
        if (StringUtils.isNotBlank(status)){
            Criteria criteria = Criteria.where("status").is(status.toUpperCase()).and("jailed").is(false);
            query.addCriteria(criteria);
        }
        return mongoTemplate.count(query,getCollectionName());
    }


    public JSONObject getValidatorsByOperator(String operatorAddress){
        Query query = new Query(Criteria.where("operator_address").is(operatorAddress));
        JSONObject validator = mongoTemplate.findOne(query,JSONObject.class,getCollectionName());
        if (validator == null){
            return null;
        }
        //错过的块json
        JSONObject signInfo = validator.getJSONObject("val_signing_info");
        if (signInfo != null){
            String startHeight = signInfo.getJSONObject("val_signing_info").getString("start_height");
            String missCount = signInfo.getJSONObject("val_signing_info").getString("missed_blocks_counter");
            String blockMaxNum = blockService.getMaxBlockNumber();
            if (blockMaxNum != null){
                startHeight = new BigDecimal(blockMaxNum).subtract(new BigDecimal(startHeight)).toPlainString();
                validator.put("miss_info",missCount+" in "+startHeight +" blocks");
            }
        }
        String chainName = configService.getChainName();
        if ("usdg".equals(chainName)){
            chainName = "dga";
        }
        validator.put("chainName",chainName);
        //佣金收益
        BigDecimal commissionRewards =BalanceUtil.getValidateatorReward(operatorAddress,configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix(),chainName);
        if (commissionRewards != null){
            validator.put("commission_rewards",commissionRewards);
        }else {
            commissionRewards = BigDecimal.ZERO;
            validator.put("commission_rewards",commissionRewards);
        }
        //佣金分配
        JSONObject commissionReallocation = validator.getJSONObject("commission_reallocation");
        if (commissionReallocation != null){
            BigDecimal rate = new BigDecimal(validator.getJSONObject("commission").getJSONObject("commission_rates").getString("rate"));
            BigDecimal reserveRate = new BigDecimal(commissionReallocation.getString("reserve_rate"));
            BigDecimal reallocatedRate = new BigDecimal(commissionReallocation.getString("reallocated_rate"));
            BigDecimal selfRate = rate.multiply(reserveRate);
            BigDecimal inviteRate = (rate.subtract(selfRate)).multiply(reallocatedRate);
            BigDecimal otherRate = (rate.subtract(selfRate)).multiply(BigDecimal.ONE.subtract(reallocatedRate));
            validator.put("selfRate",selfRate);
            validator.put("inviteRate",inviteRate);
            validator.put("otherRate",otherRate);
            BigDecimal selfRewards = commissionRewards.multiply(reserveRate).divide(BigDecimal.ONE,6, RoundingMode.HALF_UP);
            validator.put("selfRewards",selfRewards);
            validator.put("redistributionRewards",commissionRewards.subtract(selfRewards));
        }else {
            validator.put("selfRate",0);
            validator.put("inviteRate",0);
            validator.put("otherRate",0);
            validator.put("selfRewards",0);
            validator.put("redistributionRewards",0);
        }
        return validator;
    }

    public List<JSONObject> getOtherValidatorCommissionByOperator(String operatorAddress){
        Query query = new Query(Criteria.where("operator_address").ne(operatorAddress).and("jailed").is(false).and("status").is("BOND_STATUS_BONDED"));
        query.fields().include("commission.commission_rates.rate");
        query.fields().include("tokens");
        query.fields().include("description.moniker");
        return mongoTemplate.find(query,JSONObject.class,getCollectionName());
    }

    public JSONObject getValidatorByAddress(String address){
        Query query = new Query(Criteria.where("address").is(address));
        query.fields().include("description.moniker");
        query.fields().include("icon");
        query.fields().include("operator_address");
        query.fields().exclude("_id");
        return mongoTemplate.findOne(query,JSONObject.class,getCollectionName());
    }

    public List<String> getValidatorAddress(){
        Query query = new Query();
        query.fields().include("operator_address");
        List<JSONObject> v = mongoTemplate.find(query,JSONObject.class,getCollectionName());
        return v.stream().map( jsonObject -> {
            return jsonObject.getString("operator_address");
        }).collect(Collectors.toList());
    }

    //todo 最好存缓存，设置一个存在时间
    public List<JSONObject> getValidators(){
        Query query = new Query();
        query.fields().include("operator_address");
        query.fields().include("description.moniker");
        query.fields().include("selfDelegateAmount");
        query.fields().include("icon");
        query.fields().exclude("_id");
        return mongoTemplate.find(query,JSONObject.class,getCollectionName());
    }

    public String getCollectionName(){
        if (collectionName == null){
            collectionName = configService.getChainName() + "_" + Collocation.collection_validators;
        }
        return collectionName;
    }

//    public String getChainName(){
//        if (chainName == null){
//            chainName = configService.getConfig(ConfigUtil.chainNameKey);
//        }
//        return chainName;
//    }
//
//    public String getChainPrefix(){
//        if (chainPrefix == null){
//            chainPrefix = configService.getConfig(ConfigUtil.chainPrefixKey);
//        }
//        return chainPrefix;
//    }

}
