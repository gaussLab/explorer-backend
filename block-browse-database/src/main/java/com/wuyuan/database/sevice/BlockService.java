package com.wuyuan.database.sevice;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.result.UpdateResult;
import com.wuyuan.database.entity.GlobalBlock;
import com.wuyuan.database.util.Collocation;
import com.wuyuan.database.util.ConfigUtil;
import com.wuyuan.database.util.PageModel;
import com.wuyuan.database.util.TimeUtil;
import cosmos.Block;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BlockService {

//    private String chainName;

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private ValidatorService validatorService;

    @Resource
    private ConfigService configService;

    private String collectionName;

    private Long firstBlockTime;

    private String minBlockNum;

    public void saveBlock(Block blcok){
        mongoTemplate.save(blcok,getCollectionName());
    }

    public JSONObject getBlockByBlockNumber(String blockHeight){
        if (StringUtils.isNotBlank(blockHeight)){
            Query query=new Query(Criteria.where("block.header.height").is(blockHeight.trim()));
            query.fields().exclude("_id");
            JSONObject block = mongoTemplate.findOne(query,JSONObject.class,getCollectionName());
            if (block != null){
                block.put("blockMax",getMaxBlockNumber());
                return getBlock(block);
            }
            return block;
        }
        return null;
    }

    public Long getCountByTime(String time){
        Query query = new Query(Criteria.where("block.header.time").gte(time));
        return mongoTemplate.count(query,getCollectionName());
    }

    public PageModel<JSONObject> getBlockList(Integer pageIndex,Integer pageSize){
        Criteria cr = new Criteria();

        if (pageIndex == null || pageIndex == 0){
            pageIndex = 1;
        }
        if (pageSize == null || pageSize == 0){
            pageSize = 10;
        }
        Query query = new Query(cr);
        long count = mongoTemplate.getCollection(getCollectionName()).estimatedDocumentCount();
        query.skip((pageIndex - 1) * pageSize);
        query.limit(pageSize);
        query.with(Sort.by(
                Sort.Order.desc("_id"))
        );
        List<JSONObject> blockList = mongoTemplate.find(query,JSONObject.class,getCollectionName());
        blockList = blockList.stream().map( block -> {
           return getBlock(block);
        }).collect(Collectors.toList());
        PageModel<JSONObject> pageModel=new PageModel<>(blockList,pageIndex,pageSize,count);
        return pageModel;
    }

    public List<JSONObject> getTopTenBlock(String blockNumber,Integer pageIndex,Integer pageSize){
        Criteria cr = new Criteria();
        cr.andOperator(Criteria.where("block.header.height").lte(blockNumber));
        if (pageIndex == null || pageIndex == 0){
            pageIndex = 1;
        }
        if (pageSize == null || pageSize == 0){
            pageSize = 10;
        }
        Query query = new Query(cr);
//        Document document = Collation.of("zh").toDocument();
//        document.put("numericOrdering",true);
//        query.collation(Collation.from(document));
//        long count = mongoTemplate.count(query,getCollectionName());
        query.skip((pageIndex - 1) * pageSize);
        query.limit(pageSize);
        query.with(Sort.by(
                Sort.Order.desc("_id"))
        );
        List<JSONObject> blockList = mongoTemplate.find(query,JSONObject.class,getCollectionName());
        blockList = blockList.stream().map( block -> {
            return getBlock(block);
        }).collect(Collectors.toList());

        return blockList;
    }

    public String getMaxBlockNumber(){
        Query query = new Query();

//        Document document = Collation.of("zh").toDocument();
//        document.put("numericOrdering",true);
//        query.collation(Collation.from(document));
        query.fields().include("block.header.height");
        query.with(Sort.by(
                Sort.Order.desc("_id")
        ));
        query.limit(1);
        JSONObject block = JSON.parseObject(mongoTemplate.findOne(query,String.class,getCollectionName()));
        Integer blockNumber = null;
        if (block != null){
            blockNumber = block.getJSONObject("block").getJSONObject("header").getInteger("height") - 1;
            return blockNumber.toString();
        }
        return null;
    }

    public String getMinBlockNumber(){
        Query query = new Query();
        query.fields().include("block.header.height");
        query.with(Sort.by(
                Sort.Order.asc("_id")
        ));
        query.limit(1);
        JSONObject block = JSON.parseObject(mongoTemplate.findOne(query,String.class,getCollectionName()));
        String blockNumber = null;
        if (block != null){
            blockNumber = block.getJSONObject("block").getJSONObject("header").getString("height");
            return blockNumber;
        }
        return null;
    }

    public GlobalBlock saveGlobal(String chainName,GlobalBlock globalBlock){
       if (StringUtils.isBlank(chainName)){
           return mongoTemplate.save(globalBlock,configService.getChainName()+"_"+Collocation.collection_global);
       }
       return mongoTemplate.save(globalBlock,chainName+"_"+Collocation.collection_global);
    }

    public GlobalBlock getGlobal(String chainName){
        if (StringUtils.isBlank(chainName)){
            Query query=new Query(Criteria.where("chainName").is(configService.getChainName()));
            GlobalBlock globalBlock = mongoTemplate.findOne(query,GlobalBlock.class,configService.getChainName()+"_"+Collocation.collection_global);
            return globalBlock;
        }
        Query query=new Query(Criteria.where("chainName").is(chainName));
        GlobalBlock globalBlock = mongoTemplate.findOne(query,GlobalBlock.class,chainName+"_"+Collocation.collection_global);
        return globalBlock;
    }

    public JSONObject getBlock(JSONObject blockJson){
        JSONObject block = new JSONObject();
        JSONObject header = blockJson.getJSONObject("block").getJSONObject("header");
        block.put("height",header.getString("height"));
        block.put("block_hash",blockJson.getJSONObject("block_id").getString("hash"));
        block.put("time_stamp",header.getString("time"));
        String address = header.getString("proposer_address");
        JSONObject var = validatorService.getValidatorByAddress(address);
        block.put("validator",var.getJSONObject("description").getString("moniker"));
        block.put("operatorAddress",var.getString("operator_address"));
        block.put("validator_coin",var.getString("icon"));
        if (blockJson.containsKey("blockMax")){
            block.put("blockMax",blockJson.getString("blockMax"));
        }
        if (blockJson.getJSONArray("commit_signatures") != null){
            block.put("validator_num",blockJson.getJSONArray("commit_signatures").size());
        }
        block.put("tx_num",blockJson.getJSONObject("block").getJSONObject("data").getJSONArray("txs").size());
        return block;
    }

    public UpdateResult updateBlock(String blockNum, JSONArray json){
        Query query = new Query(Criteria.where("block.header.height").is(blockNum));
        JSONObject block = mongoTemplate.findOne(query,JSONObject.class,getCollectionName());
        if (block != null){
            query = new Query(Criteria.where("_id").is(block.getString("_id")));
            Update update = new Update();
            update.set("commit_signatures",json);
            return mongoTemplate.updateFirst(query,update,getCollectionName());
        }
        return null;
    }

    public JSONObject averageBlockTime (String blockMaxNum){
        JSONObject averageJson = new JSONObject();
        String date = getBlockByBlockNumber(blockMaxNum).getString("time_stamp");
        Long maxTime = TimeUtil.getUTCTime(date);
        BigDecimal tatal = BigDecimal.ONE;
        if (firstBlockTime == null){
            if (minBlockNum == null){
                minBlockNum = getMinBlockNumber();
            }
            String time = getBlockByBlockNumber(minBlockNum).getString("time_stamp");
            firstBlockTime = TimeUtil.getUTCTime(time);
        }
        tatal = new BigDecimal(Integer.parseInt(blockMaxNum)-Integer.parseInt(minBlockNum));
        String averageTime = (new BigDecimal(maxTime)
                .subtract(new BigDecimal(firstBlockTime)))
                .divide(tatal,6, RoundingMode.HALF_UP)
                .divide(new BigDecimal(1000),2,RoundingMode.HALF_UP).toPlainString();
        averageJson.put("averageTime",averageTime);
        Long timeStamp = maxTime - 1000*60;
//        Long minCount = getCountByTime(TimeUtil.getUtcDate(timeStamp));
        String minuteTime = new BigDecimal(60).divide(new BigDecimal(12),2,RoundingMode.HALF_UP).toPlainString();
        averageJson.put("minuteTime",minuteTime);
        timeStamp = maxTime - 1000*60*60;
        Long hourCount = getCountByTime(TimeUtil.getUtcDate(timeStamp));
        String hourTime = ((new BigDecimal(maxTime))
                .subtract(new BigDecimal(timeStamp)))
                .divide(new BigDecimal(hourCount),6, RoundingMode.HALF_UP)
                .divide(new BigDecimal(1000),2,RoundingMode.HALF_UP).toPlainString();
        averageJson.put("hourTime",hourTime);
        timeStamp = maxTime - 1000*60*60*24;
        Long dayCount = getCountByTime(TimeUtil.getUtcDate(timeStamp));
        String dayTime = (new BigDecimal(maxTime)
                .subtract(new BigDecimal(timeStamp)))
                .divide(new BigDecimal(dayCount),6, RoundingMode.HALF_UP)
                .divide(new BigDecimal(1000),2,RoundingMode.HALF_UP).toPlainString();
        averageJson.put("dayTime",dayTime);
        return  averageJson;
    }

    public String getCollectionName(){
        if (collectionName == null){
            collectionName = configService.getChainName()+"_"+Collocation.collection_block;
        }
        return collectionName;
    }

//    public String getChainName(){
//        if (chainName == null){
//            chainName = configService.getConfig(ConfigUtil.chainNameKey);
//        }
//        return chainName;
//    }

 }
