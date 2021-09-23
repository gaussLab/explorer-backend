package com.wuyuan.blockbrowse.job;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wuyuan.database.entity.GlobalBlock;
import com.wuyuan.database.sevice.*;
import com.wuyuan.database.util.Collocation;
import com.wuyuan.database.util.ConfigUtil;
import com.wuyuan.database.util.GetIdUtil;
import io.cosmos.util.CosmosUtil;
import io.cosmos.util.Sha256;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@EnableScheduling
public class SyncIgpcJob {

    @Resource
    private BlockService blockService;

    @Resource
    private MongoSevice mongoSevice;

    @Resource
    private ConfigService configService;

    @Resource
    private TxsQueueService txsQueueService;

    private String chainName = "igpc";

    private Long startNum;

    @Scheduled(fixedDelay = 1000)
    public void syncBlock() {
        log.info("start SyncIgpcJob");
        GlobalBlock globalBlock = blockService.getGlobal(chainName);
        long newBlockNum = CosmosUtil.getNewBlock(configService.getConfig(ConfigUtil.igpcUrl));
        startNum = new BigDecimal(configService.getConfig(ConfigUtil.coinStartNumKey)).longValue();
        if (globalBlock != null) {
            startNum = globalBlock.getBlockNum();
        }else{
            globalBlock =new GlobalBlock();
            globalBlock.setChainName(chainName);
            globalBlock.setBlockNum(startNum);
        }

        if (startNum != 1){
            startNum = startNum+1;
        }
        for (long i = startNum; i < newBlockNum; i++) {
            JSONObject b = CosmosUtil.getBlockByHeight(configService.getConfig(ConfigUtil.igpcUrl),i);
            JSONArray txs = b.getJSONObject("block").getJSONObject("data").getJSONArray("txs");
            if(txs!=null && txs.size()>0){
                saveTransaction(txs);
            }
            globalBlock.setBlockNum(i);
            globalBlock=blockService.saveGlobal(chainName,globalBlock);
            log.info("IGPC"+i+"同步完成");
        }
    }

    public void saveTransaction(JSONArray txs){
        List<String> transactions = txs.toJavaList(String.class);
        transactions.stream().forEach(txJson ->{
            String hash= Sha256.tx2Sha256(txJson).toUpperCase();

            JSONObject transfer = CosmosUtil.getTxByhash(configService.getConfig(ConfigUtil.igpcUrl)+"/gauss",hash);
            if (null != transfer){
                JSONObject tx = TransferService.getTransfer(transfer,chainName,configService.getConfig(ConfigUtil.igpcUrl));
                JSONArray events = tx.getJSONArray("event");
                events.toJavaList(JSONObject.class).stream().forEach(event -> {
                    tx.put("event",event);

                    String type = event.getString("type");
                    if (StringUtils.isBlank(type)) {
                        return;
                    }
                    switch (type){
                        case "transfer":
                            Integer code = tx.getInteger("code");
                            if (code == null || code != 0){
                                JSONObject cross = new JSONObject();
                                cross.put("first",tx);
                                JSONObject crossTx = mongoSevice.getCrossTx(cross,chainName);
                                crossTx.put("id", GetIdUtil.getId());
                                mongoSevice.save(crossTx,Collocation.collection_transaction);
                            }else {
                                String sequence = event.getString("packet_sequence");
                                long timeOut = event.getLongValue("timeoutNum");
                                if (txsQueueService.isExist(sequence,timeOut) > 0){
                                    JSONObject queueJson = txsQueueService.getTx(sequence,timeOut);
                                    if (queueJson != null){
                                        queueJson.put("first",tx);
                                        txsQueueService.save(queueJson);
                                        txsQueueService.dealQue(queueJson);
                                    }
                                }else {
                                    JSONObject cross = new JSONObject();
                                    cross.put("first",tx);
                                    txsQueueService.save(cross);
                                }
                            }
                            break;
                        case "aknowledgement":
                            String sequence = event.getString("packet_sequence");
                            long timeOut = event.getLongValue("timeoutNum");
                            if (txsQueueService.isExist(sequence,timeOut) > 0){
                                JSONObject queueJson = txsQueueService.getTx(sequence,timeOut);
                                if (queueJson != null){
                                    queueJson.put("third",tx);
                                    txsQueueService.save(queueJson);
                                    txsQueueService.dealQue(queueJson);
                                }
                            }else {
                                JSONObject cross = new JSONObject();
                                cross.put("third",tx);
                                txsQueueService.save(cross);
                            }
                            break;
                        case "recvPacket":
                        case "timeOut":
                            sequence = event.getString("packet_sequence");
                            timeOut = event.getLongValue("timeoutNum");
                            if (txsQueueService.isExist(sequence,timeOut) > 0){
                                JSONObject queueJson = txsQueueService.getTx(sequence,timeOut);
                                if (queueJson != null){
                                    queueJson.put("second",tx);
                                    txsQueueService.save(queueJson);
                                    txsQueueService.dealQue(queueJson);
                                }
                            }else {
                                JSONObject cross = new JSONObject();
                                cross.put("second",tx);
                                txsQueueService.save(cross);
                            }
                            break;
                        default:
                            break;
                    }
                });

            }

        });
    }
}
