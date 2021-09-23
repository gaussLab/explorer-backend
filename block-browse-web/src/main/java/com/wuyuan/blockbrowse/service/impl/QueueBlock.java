package com.wuyuan.blockbrowse.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wuyuan.blockbrowse.controller.WebSocketHandler;
import com.wuyuan.database.sevice.AddressService;
import com.wuyuan.database.sevice.BlockService;
import com.wuyuan.database.sevice.TransactionService;
import com.wuyuan.database.sevice.ValidatorService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Service
public class QueueBlock {
    @Resource
    private BlockService blockService;

    @Resource
    private TransactionService transactionService;

    @Resource
    private ValidatorService validatorService;

    @Resource
    private AddressService addressService;

    public static List<JSONObject> blockList = Collections.synchronizedList(new ArrayList<>(10));
//    public List<JSONObject> transactionList = Collections.synchronizedList(new ArrayList<>(10));

    public String getBlockList(String blockMax){
        System.out.println(blockMax+"------------");
        List<JSONObject> transactionList =  transactionService.getTxList();
         if (blockList!= null && blockList.size() != 10){
             List<JSONObject> blocks = blockService.getTopTenBlock(blockMax,1,10);
             blockList = blocks;
         }
        BigDecimal txNum = new BigDecimal(transactionService.getTXCount());
        String json = JSON.toJSONString(blockList);
        JSONArray array=JSONArray.parseArray(json);
        getHomePage(array.getJSONObject(0),blockMax,txNum,transactionList);
        System.out.println(array);
        return array.toJSONString();
    }

    public JSONObject getHomePage(JSONObject block,String blockMax,BigDecimal totalTxNum,List<JSONObject> transactionList){
        if (transactionList.size() == 0){
            transactionList=transactionService.getTxList();
        }
        block.put("tx",transactionList);
        block.put("averageBlockTime",blockService.averageBlockTime(blockMax));
        block.put("totalTxNum",totalTxNum);
        block.put("totalValidators",validatorService.getValidatorsCount(null));
        block.put("activeValidators",validatorService.getValidatorsCount("BOND_STATUS_BONDED"));
        block.put("coinInfo",addressService.getHomePageCoinInfo());
        return block;
    }

}
