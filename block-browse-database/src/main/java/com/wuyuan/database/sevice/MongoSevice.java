package com.wuyuan.database.sevice;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.result.UpdateResult;
import com.wuyuan.database.entity.Transfer;
import com.wuyuan.database.util.ConfigUtil;
import io.cosmos.util.CosmosUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
@Service
public class MongoSevice {

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private ConfigService configService;

    @Transactional
    public void save(JSONObject json,String collocationName){
        mongoTemplate.save(json,collocationName);
    }

    public void instesList(List<JSONObject> list,String collocationName){
        mongoTemplate.insert(list,collocationName);
    }

    public long getBlock(String blockHeight,String collocationName){
        Query query=new Query(Criteria.where("block.header.height").is(blockHeight));
        return mongoTemplate.count(query.limit(1),collocationName);
    }

    public List<JSONObject> getTransactions(long blockHeight,String collocationName){
        Query query=new Query(Criteria.where("blockHeight").is(blockHeight));
        return mongoTemplate.find(query,JSONObject.class,collocationName);
    }

    public long getTransaction(String txhash,String collocationName){
        Query query=new Query(Criteria.where("tx_response.txhash").is(txhash));
        return mongoTemplate.count(query.limit(1),collocationName);
    }

//    public long getQueue(String txHash){
//        Query query = new Query()
//    }
    public JSONObject getCrossTx(JSONObject transferJson,String chainName){

        JSONObject transfer = transferJson.getJSONObject("first");
        JSONObject timeoutOrRecvPacket = transferJson.getJSONObject("second");
        JSONObject acknowTx = transferJson.getJSONObject("third");

        JSONObject crossTx = new JSONObject();

        String txHash = transfer.getString("txhash");
        crossTx.put("memo",transfer.getString("memo"));
        crossTx.put("fromHash",txHash);
        crossTx.put("timestamp",transfer.getString("timestamp"));
        crossTx.put("fee",transfer.getString("fee"));
        crossTx.put("feeCoinName",transfer.getString("feeCoinName"));

        JSONObject event = transfer.getJSONObject("event");
        crossTx.put("from",event.getString("from"));
        crossTx.put("to",event.getString("to"));
        crossTx.put("amount",event.getString("amount"));
        crossTx.put("coinName",event.getString("coinName"));
        crossTx.put("packet_sequence",event.getString("packet_sequence"));
        String packetSrcPort = event.getString("packet_src_port");
        String packetSrcChannel = event.getString("packet_src_channel");
        crossTx.put("packet_src_port",packetSrcPort);
        crossTx.put("packet_src_channel",packetSrcChannel);
        String packetDstPort = event.getString("packet_dst_port");
        String packetDstChannel = event.getString("packet_dst_channel");
        crossTx.put("packet_dst_port",packetDstPort);
        crossTx.put("packet_dst_channel",packetDstChannel);
        crossTx.put("packet_connection",event.getString("packet_connection"));

        JSONObject res = CosmosUtil.getIbcClient(configService.getConfig(chainName+"Url"),packetSrcChannel,packetSrcPort);
        if (res != null){
            crossTx.put("client",res.getJSONObject("identified_client_state").getString("client_id"));
        }else {
            crossTx.put("client",null);
        }
        if ("gauss".equals(chainName)){
            crossTx.put("type","Step Out");//跨出gauss
            crossTx.put("fromChain",chainName);
            crossTx.put("toChain",res.getJSONObject("identified_client_state").getJSONObject("client_state").getString("chain_id"));
        }else {
            crossTx.put("type","Step In");//跨入gauss
            crossTx.put("fromChain",chainName);
            crossTx.put("toChain","gauss");
        }

        Integer code = transfer.getInteger("code");
        if ( code == null || code != 0){
            crossTx.put("status","Failed");
//            crossTx.put("toHash","--");//code不等于0说明transfer是失败的
        }

        if (timeoutOrRecvPacket != null){
            String type = timeoutOrRecvPacket.getJSONObject("event").getString("type");
            if ("timeOut".equals(type)){
                crossTx.put("status","Failed");
            }else {
                crossTx.put("status","Success");
            }
            crossTx.put("toHash",timeoutOrRecvPacket.getString("txhash"));
        }

        return crossTx;
    }

//    public Long isExistTransfer(String txHash){
//        Query query = new Query(Criteria.where("first.tx_response.txhash").is(txHash));
//
//    }
}
