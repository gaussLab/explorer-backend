package com.wuyuan.database.sevice;

import com.alibaba.fastjson.JSONObject;
import com.wuyuan.database.util.Collocation;
import com.wuyuan.database.util.DateFormatUtil;
import com.wuyuan.database.util.PageModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import javax.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ETHTransferTxService {

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private Web3j web3j;

    @Resource
    private ETHBatchService ethBatchService;

    private String collectionName;

    public long isExistTx(String fromHash,String toHash){
        Criteria criteria = new Criteria();
        Criteria from = Criteria.where("fromHash").is(fromHash);
        Criteria to = Criteria.where("toHash").is(toHash);
        criteria.andOperator(from,to);
        Query query = new Query(criteria);
        return mongoTemplate.count(query.limit(1),getCollectionName());
    }

    public void saveTx(JSONObject tx){
        mongoTemplate.save(tx,getCollectionName());
    }

    public String getCollectionName(){
        if (collectionName == null){
            collectionName = Collocation.COLLECTION_ETH_TRANSFER;
        }
        return collectionName;
    }

    public PageModel<JSONObject> getCrossList(Integer pageIndex, Integer pageSize, String address,String hash,String status) {
        Query query = new Query();

        if (StringUtils.isNotBlank(address)){
            Criteria criteria = new Criteria();
            Criteria from = Criteria.where("from").is(address);
            Criteria to = Criteria.where("to").is(address);
            Criteria sendFrom = Criteria.where("txList.from").is(address);
            Criteria sendTo = Criteria.where("txList.to").is(address);
            criteria.orOperator(from,to,sendFrom,sendTo);
            query.addCriteria(criteria);
        }

        if (StringUtils.isNotBlank(hash)){
            Criteria criteria = new Criteria();
            Criteria crossFrom = Criteria.where("fromHash").is(hash);
            Criteria crossTo = Criteria.where("toHash").is(hash);
            Criteria listHash = Criteria.where("txList.hash").is(hash);
            criteria.orOperator(crossFrom,crossTo,listHash);
            query.addCriteria(criteria);
        }

        if (StringUtils.isNotBlank(status)){
            query.addCriteria(Criteria.where("status").is(status));
        }

        long count = 0;
        if (StringUtils.isBlank(address) && StringUtils.isBlank(hash) && StringUtils.isBlank(status)){
            count = mongoTemplate.getCollection(getCollectionName()).estimatedDocumentCount();
        }else {
            count = mongoTemplate.count(query,getCollectionName());
        }
        query.skip((pageIndex - 1) * pageSize);
        query.limit(pageSize);
        query.with(Sort.by(
                Sort.Order.desc("_id"))
        );

        List<JSONObject> txList = mongoTemplate.find(query,JSONObject.class,getCollectionName());
        PageModel<JSONObject> pageModel=new PageModel<>(txList,pageIndex,pageSize,count);
        return pageModel;
    }

    public PageModel<JSONObject> getCrossTxByHash(String hash,Integer pageIndex,Integer pageSize){
        Criteria criteria = new Criteria();
        Criteria crossFrom = Criteria.where("fromHash").is(hash);
        Criteria crossTo = Criteria.where("toHash").is(hash);
        Criteria listHash = Criteria.where("txList.hash").is(hash);
        criteria.orOperator(crossFrom,crossTo,listHash);

        Query query = new Query(criteria);


        long count = mongoTemplate.count(query,getCollectionName());
        query.skip((pageIndex - 1) * pageSize);
        query.limit(pageSize);
        query.with(Sort.by(
                Sort.Order.desc("_id"))
        );
        List<JSONObject> list = mongoTemplate.find(query,JSONObject.class,getCollectionName());
        PageModel<JSONObject> pageModel=new PageModel<>(list,pageIndex,pageSize,count);
        return pageModel;
    }



    public List<JSONObject> getSendToEth(Long[] outgoingTxIds){
        Query query = new Query(Criteria.where("outgoingTxId").in(outgoingTxIds));
        return mongoTemplate.find(query,JSONObject.class,getCollectionName());
    }

    public void dealEthTOGauss(JSONObject tx, EthBlock.Block ethBlock){
        JSONObject ethSendGauss = new JSONObject();
        ethSendGauss.put("type","Step In");
        ethSendGauss.put("fromChain","eth");
        ethSendGauss.put("toChain","gauss");

        Integer code = tx.getInteger("code");
        if (code != null && code == 0){
            ethSendGauss.put("status","Success");
        }else {
            ethSendGauss.put("status","Failed");
        }

        String toHash = tx.getString("txhash");
        ethSendGauss.put("toHash",toHash);

        JSONObject event = tx.getJSONObject("event");
        String ethSender = event.getString("ethSend");
        String receiver = event.getString("receiver");
        String contractAddress = event.getString("contractAddress");
        String amount = event.getString("amount");
        String coinName = event.getString("coinName");
        BigInteger ethBlockHeight = event.getBigInteger("ethBlockHeight");
        BigInteger eventNonce = event.getBigInteger("eventNonce");

        ethSendGauss.put("eventNonce",eventNonce);
        ethSendGauss.put("ethBlockHeight",ethBlockHeight);
        ethSendGauss.put("from",ethSender);
        ethSendGauss.put("to",receiver);
        ethSendGauss.put("contractAddress",contractAddress);
        ethSendGauss.put("amount",amount);
        ethSendGauss.put("coinName",coinName);

        BigInteger timeStamp = ethBlock.getTimestamp();
        String date = DateFormatUtil.timeToFormat(timeStamp.longValue() * 1000);//毫秒转日期
        ethSendGauss.put("timeStamp",date);

        List<EthBlock.TransactionResult> ethBlockTransactions = Collections.synchronizedList(ethBlock.getTransactions());

        ethBlockTransactions.parallelStream().forEach( ethTx -> {
            if (ethTx instanceof EthBlock.TransactionObject){
                String fromHash = ((EthBlock.TransactionObject) ethTx).getHash();
                String method ="";
                if(((EthBlock.TransactionObject) ethTx).getInput().length()>=10){
                    method = ((EthBlock.TransactionObject) ethTx).getInput().substring(0, 10);
                }

                if("0x1ffbe7f9".equalsIgnoreCase(method)){
                    try {
                        TransactionReceipt transactionReceipt = web3j.ethGetTransactionReceipt(fromHash).sendAsync().get().getTransactionReceipt().get();
                        String to = transactionReceipt.getTo();//用来找eventNonce

                        String data = "";
                        List<Log> logList = transactionReceipt.getLogs();
                        for (int i = 0; i < logList.size(); i++) {
                            Log log = logList.get(i);
                            String address =log.getAddress();
                            if (contractAddress.equalsIgnoreCase(address) ){
                                data = log.getData();
                                break;
                            }
                        }

                        String toData = null;
                        for (int i = 0; i < logList.size(); i++) {
                            Log log = logList.get(i);
                            String address =log.getAddress();
                            if (to.equalsIgnoreCase(address) ){
                                toData = log.getData();
                                break;
                            }
                        }

                        if (StringUtils.isNotBlank(toData) && toData.contains(data)){
                            toData = toData.substring(toData.indexOf(data)+data.length());
                            if (toData.length() != 64){
                                throw new IllegalStateException("Unexpected value: " + fromHash);
                            }

                            if (eventNonce.compareTo(getEventNonce(toData)) == 0){
                                String fee = new BigDecimal(((EthBlock.TransactionObject) ethTx).getGas()).divide(new BigDecimal(Math.pow(10,18))).toPlainString();
                                ethSendGauss.put("fee",fee);
                                ethSendGauss.put("feeCoinName","eth");
                                ethSendGauss.put("fromHash",fromHash);
                                if (isExistTx(fromHash,toHash) == 0){
                                    saveTx(ethSendGauss);
                                    System.out.println(ethSendGauss);
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }

                }
            }
        });
    }

    public void dealSendToEth(JSONObject tx,Integer code,String hash){
        JSONObject sendToEth = new JSONObject();

        String from = tx.getString("from");
        String to = tx.getString("to");
        String amount = tx.getString("amount");
        String coinName = tx.getString("coinName");
        String fee = tx.getString("fee");
        String feeCoinName = tx.getString("feeCoinName");
        long timeStamp = tx.getDate("timeStamp").getTime();
        String date = DateFormatUtil.timeToFormat(timeStamp);

        sendToEth.put("from",from);
        sendToEth.put("to",to);
        sendToEth.put("amount",amount);
        sendToEth.put("coinName",coinName);
        sendToEth.put("fee",fee);
        sendToEth.put("feeCoinName",feeCoinName);
        sendToEth.put("timeStamp",date);

        sendToEth.put("fromHash",hash);
        sendToEth.put("toHash",null);
        sendToEth.put("fromChain","gauss");
        sendToEth.put("toChain","eth");
        sendToEth.put("type","Step Out");
        sendToEth.put("bridgeFee",tx.getString("bridgeFee"));
        sendToEth.put("outgoingTxId",tx.getLongValue("outgoingTxId"));

        if (code == null || code != 0){
            sendToEth.put("status","Failed");
        }else {
            sendToEth.put("status","Pending");
        }

        if(isExistTx(hash,null) == 0){
            saveTx(sendToEth);
            System.out.println(sendToEth);
        }
    }

    public void dealWithdrawClaim(JSONObject tx,long batchNonce){
        JSONObject event = tx.getJSONObject("event");
        BigInteger ethBlockHeight = event.getBigInteger("ethBlockHeight");
        BigInteger eventNonce = event.getBigInteger("eventNonce");
        try {
            EthBlock.Block ethBlock = web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(ethBlockHeight), true).send().getBlock();
            List<EthBlock.TransactionResult> ethBlockTransactions = Collections.synchronizedList(ethBlock.getTransactions());
            ethBlockTransactions.parallelStream().forEach( ethTx -> {
                if (ethTx instanceof EthBlock.TransactionObject){
                    String toHash = ((EthBlock.TransactionObject) ethTx).getHash();
                    String method ="";
                    if(((EthBlock.TransactionObject) ethTx).getInput().length()>=10){
                        method = ((EthBlock.TransactionObject) ethTx).getInput().substring(0, 10);
                    }

                    if("0x83b435db".equalsIgnoreCase(method)){
                        try {
                            TransactionReceipt transactionReceipt = web3j.ethGetTransactionReceipt(toHash).sendAsync().get().getTransactionReceipt().get();
                            String to = transactionReceipt.getTo();//用来找eventNonce

                            List<Log> logList = transactionReceipt.getLogs();
                            String data = null;
                            for (Log logTx:logList) {
                                String address = logTx.getAddress();
                                if (address.equalsIgnoreCase(to)){
                                    data = logTx.getData();
                                }
                            }
                            if (StringUtils.isNotBlank(data)){
                                if (data.length() != 66){
                                    throw new IllegalStateException("Unexpected value: " + toHash);
                                }
                                if (eventNonce.compareTo(getEventNonce(data)) == 0){
                                    JSONObject confirmBatch = ethBatchService.getConfirmBatch(batchNonce);
                                    if (confirmBatch == null){
                                        throw new IllegalStateException("Unexpected value: " + toHash);
                                    }
                                    String outGoingTxIds = confirmBatch.getString("outgoingTxIds");
                                    Long[] idList = getTxId(outGoingTxIds);
                                    List<JSONObject> senToEthList = getSendToEth(idList);
                                    if (senToEthList != null && !senToEthList.isEmpty()){
                                        senToEthList.stream().forEach(sendToEth -> {
                                            sendToEth.put("status","Success");
                                            sendToEth.put("toHash",toHash);
                                            sendToEth.put("eventNonce",eventNonce);
                                            sendToEth.put("ethBlockHeight",ethBlockHeight);
                                            sendToEth.put("contractAddress",event.getString("contractAddress"));
                                            saveTx(sendToEth);
                                            ethBatchService.deleteBatch(batchNonce);
                                            System.out.println(sendToEth);
                                        });
//                                        JSONObject gaussToEth = new JSONObject();
//                                        gaussToEth.put("txList",senToEthList);
//                                        gaussToEth.put("type","Step Out");
//                                        gaussToEth.put("fromChain","gauss");
//                                        gaussToEth.put("toChain","eth");
//                                        gaussToEth.put("status","Success");
//                                        String fromHash = tx.getString("txhash");
//                                        gaussToEth.put("fromHash",fromHash);
//                                        gaussToEth.put("toHash",toHash);
//                                        gaussToEth.put("from",null);
//                                        gaussToEth.put("to",null);
//                                        gaussToEth.put("eventNonce",eventNonce);
//                                        gaussToEth.put("ethBlockHeight",ethBlockHeight);
//                                        gaussToEth.put("contractAddress",event.getString("contractAddress"));
//                                        gaussToEth.put("fee",tx.getString("fee"));
//                                        gaussToEth.put("feeCoinName",tx.getString("feeCoinName"));
//                                        gaussToEth.put("coinName",event.getString("coinName"));
//                                        Long time = tx.getDate("timestamp").getTime();
//                                        String date = DateFormatUtil.timeToFormat(time);
//                                        gaussToEth.put("timeStamp",date);
//                                        if (isExistTx(fromHash,toHash) == 0){
//                                            saveTx(gaussToEth);
//                                            ethBatchService.deleteBatch(batchNonce);
//                                            ethTxsQueueService.deleteQueue(idList);
//                                            System.out.println(gaussToEth);
//                                        }

                                    }
                                }
                            }


                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }

                    }
                }
            });

        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public Long[] getTxId(String outGoingTxIds){
        String[] ids = outGoingTxIds.split(",");
        List<Long> list = Arrays.stream(ids).map( id -> Long.parseLong(id)).collect(Collectors.toList());
        return list.toArray(new Long[0]);
    }

    public BigInteger getEventNonce(String data){
        if (data.startsWith("0x")){
            data = data.substring(2);
        }
        return BigInteger.valueOf(Integer.parseInt(data,16));
    }

    public JSONObject getTxByHash(String fromHash, String toHash) {
        Query query = new Query();
        Criteria criteria = new Criteria();
        Criteria from = Criteria.where("fromHash").is(fromHash);
        Criteria to = Criteria.where("toHash").is(toHash);
        criteria.andOperator(from,to);

        query.addCriteria(criteria);
        return mongoTemplate.findOne(query,JSONObject.class,getCollectionName());
    }
}
