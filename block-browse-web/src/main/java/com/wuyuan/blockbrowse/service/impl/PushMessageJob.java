package com.wuyuan.blockbrowse.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.wuyuan.blockbrowse.controller.WebSocketHandler;
import com.wuyuan.blockbrowse.entity.EventPrams;
import com.wuyuan.database.sevice.AddressService;
import com.wuyuan.database.sevice.BlockService;
import com.wuyuan.database.sevice.TransactionService;
import com.wuyuan.database.sevice.ValidatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class PushMessageJob {

    @Resource
    private BlockService blockService;

    @Resource
    private TransactionService transactionService;

    @Resource
    private ValidatorService validatorService;

    @Resource
    private AddressService addressService;

    public BigDecimal totalTxNum;

//    public synchronized void startPush(EventPrams eventPrams){
//        switch (eventPrams.getPage().toLowerCase()){
//            case "homepage":
//                pushMessage(eventPrams.getPage().toLowerCase(),eventPrams.getBlockNum().trim());
//                break;
//        }
//    }

    public synchronized String pushMessage(String lastBlock,String page,String blockNum){

//        while (true){
        try {
            String blockMax = blockService.getMaxBlockNumber();
            log.info(blockMax+"----------------------max");
            WebSocketHandler.blockMax = blockMax;
            if (totalTxNum == null){
                totalTxNum = new BigDecimal(transactionService.getTXCount());
            }
            if (lastBlock == null){
                lastBlock = blockNum;
            }
            for (int i = Integer.parseInt(lastBlock)+1; i <= Integer.parseInt(blockMax); i++){
                log.info("区块" + i + "推送");
                JSONObject block = blockService.getBlockByBlockNumber(String.valueOf(i));

                Integer txSize = block.getIntValue("tx_num");
                List<JSONObject> transactionList = new ArrayList<>();
                if (txSize != null){
                    transactionList = transactionService.getTransaction(String.valueOf(i),null,null,null,null,null,null,1,txSize,null,null,null).getRecords();
                    totalTxNum = totalTxNum.add(new BigDecimal(transactionList.size()));
                }
                removeBlock();
                addBlock(block);
                String json = JSON.toJSONString(getHomePage(block,blockMax,totalTxNum,transactionList));
                EventManager.dispatch_events(page,json);
            }
            return blockMax;
//                Thread.sleep(5000);
        }catch (Exception e){
            log.info("发现错误 " + e.getMessage());
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error",e.getMessage());
            EventManager.dispatch_events(page,JSON.toJSONString(jsonObject));
        }
        return null;
//        }
    }

    public synchronized void addBlock(JSONObject block){
        QueueBlock.blockList.add(0,block);
    }

    public synchronized void removeBlock(){
        if (QueueBlock.blockList.size() == 10){
            QueueBlock.blockList.remove(9);
        }
    }

    public List<JSONObject> getHomePage(JSONObject block, String blockMax, BigDecimal totalTxNum, List<JSONObject> transactionList){
        block.put("tx",transactionList);
        block.put("averageBlockTime",blockService.averageBlockTime(blockMax));
        block.put("totalTxNum",totalTxNum);
        block.put("totalValidators",validatorService.getValidatorsCount(null));
        block.put("activeValidators",validatorService.getValidatorsCount("BOND_STATUS_BONDED"));
        block.put("coinInfo",addressService.getHomePageCoinInfo());
        List<JSONObject> blocks = new ArrayList<>();
        blocks.add(block);
        return blocks;
    }
}
