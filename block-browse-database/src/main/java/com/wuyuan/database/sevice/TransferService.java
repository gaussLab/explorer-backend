package com.wuyuan.database.sevice;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.wuyuan.database.entity.Transfer;
import io.cosmos.util.CosmosUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@Repository
@Slf4j
public class TransferService {

    public static Web3j web3j;

    public static ContractService contractService;

    public static JSONObject getTransfer(JSONObject transfer, String chainName, String url){
        JSONObject transaction = new JSONObject();

        JSONObject txResponse = transfer.getJSONObject("tx_response");
        JSONObject tx = transfer.getJSONObject("tx");

        transaction.put("txhash",txResponse.getString("txhash"));
        transaction.put("timestamp",txResponse.getString("timestamp"));
        transaction.put("gas_used",txResponse.getString("gas_used"));
        transaction.put("gas_wanted",txResponse.getString("gas_wanted"));
        transaction.put("height",txResponse.getString("height"));
        transaction.put("code",txResponse.getIntValue("code"));
        JSONArray fee = txResponse.getJSONObject("tx").getJSONObject("auth_info").getJSONObject("fee").getJSONArray("amount");
        if (fee.size() > 0){
            String demno = fee.getJSONObject(0).getString("denom");

            if ("udga".equalsIgnoreCase(demno)){
                transaction.put("fee",new BigDecimal(fee.getJSONObject(0).getString("amount"))
                        .divide(new BigDecimal("1000000")).toPlainString());
                transaction.put("feeCoinName","dga");
            }else {
                transaction.put("fee",new BigDecimal(fee.getJSONObject(0).getString("amount"))
                        .divide(new BigDecimal("1000000")).toPlainString());
                transaction.put("feeCoinName",chainName);
            }
        }else {
            transaction.put("fee",BigDecimal.ZERO);
            transaction.put("feeCoinName",chainName);
        }
        transaction.put("memo",tx.getJSONObject("body").getString("memo"));

        JSONArray typeArray = tx.getJSONObject("body").getJSONArray("messages");
        JSONArray messages = new JSONArray();

        typeArray.stream().forEach( typeMessage -> {
            JSONObject value = JSON.parseObject(JSON.toJSONString(typeMessage), Feature.DisableSpecialKeyDetect);
            String type = value.getString("@type");
            JSONObject txJson = new JSONObject();
            switch (type) {
                case Transfer.transfer:
                    txJson.put("type","transfer");
                    txJson.put("from",value.getString("sender"));
                    txJson.put("to",value.getString("receiver"));
                    JSONObject token = value.getJSONObject("token");
                    String denom = token.getString("denom");
                    String amount = token.getString("amount");
                    txJson.put("chainName",chainName);
                    txJson.put("amount",amount);
                    if (denom.startsWith("ibc/")){
                        String hash = denom.substring(4);
                        String result = CosmosUtil.getTransferTokenByHash(url,hash);
                        if (result != null){
                            JSONObject denomTrace = JSON.parseObject(result);
                            String coinName = denomTrace.getJSONObject("denom_trace").getString("base_denom");
                            txJson.put("coinName",coinName);
                        }else {
                            txJson.put("coinName",denom);
                        }
                    }else {
                        txJson.put("coinName",denom);
                    }
                    JSONObject timeoutHeight = value.getJSONObject("timeout_height");
                    txJson.put("timeoutNum",timeoutHeight.getLongValue("revision_height"));
                    JSONArray logs = txResponse.getJSONArray("logs");
                    if (logs.size() > 0){
                        logs.toJavaList(JSONObject.class).stream().forEach(log ->{
                            log.getJSONArray("events").toJavaList(JSONObject.class).stream().forEach(event -> {
                                if("send_packet".equals(event.getString("type"))){
                                    JSONArray attributes = event.getJSONArray("attributes");
                                    attributes.toJavaList(JSONObject.class).stream().forEach( attribute -> {
                                        //todo 获取里面的跨链信息
                                        String key = attribute.getString("key");
                                        switch (key){
                                            case "packet_sequence":
                                                txJson.put("packet_sequence",attribute.getString("value"));
                                                break;
                                            case "packet_src_port":
                                                txJson.put("packet_src_port",attribute.getString("value"));
                                                break;
                                            case "packet_src_channel":
                                                txJson.put("packet_src_channel",attribute.getString("value"));
                                                break;
                                            case "packet_dst_port":
                                                txJson.put("packet_dst_port",attribute.getString("value"));
                                                break;
                                            case "packet_dst_channel":
                                                txJson.put("packet_dst_channel",attribute.getString("value"));
                                                break;
                                            case "packet_connection":
                                                txJson.put("packet_connection",attribute.getString("value"));
                                                break;
                                        }
                                    });
                                }
                            });
                        });
                    }else {
                        txJson.put("packet_src_port",value.getString("source_port"));
                        txJson.put("packet_src_channel",value.getString("source_channel"));
                    }
                    break;
                case Transfer.recvPacket:
                    txJson.put("type","recvPacket");
                    JSONObject packet = value.getJSONObject("packet");
                    txJson.put("packet_sequence",packet.getString("sequence"));
                    timeoutHeight = packet.getJSONObject("timeout_height");
                    txJson.put("timeoutNum",timeoutHeight.getLongValue("revision_height"));
                    break;
                case Transfer.aknowledgement:
                    txJson.put("type","aknowledgement");
                    packet = value.getJSONObject("packet");
                    txJson.put("packet_sequence",packet.getString("sequence"));
                    timeoutHeight = packet.getJSONObject("timeout_height");
                    txJson.put("timeoutNum",timeoutHeight.getLongValue("revision_height"));
                    break;
                case Transfer.timeOut:
                    txJson.put("type","timeOut");
                    packet = value.getJSONObject("packet");
                    txJson.put("packet_sequence",packet.getString("sequence"));
                    timeoutHeight = packet.getJSONObject("timeout_height");
                    txJson.put("timeoutNum",timeoutHeight.getLongValue("revision_height"));
                    break;

                case Transfer.DepositClaim:
                    txJson.put("type","DepositClaim");
                    String receiver = value.getString("cosmos_receiver");
                    String ethSend = value.getString("ethereum_sender");
                    long ethBlockHeight = value.getLongValue("block_height");
                    long eventNonce = value.getLongValue("event_nonce");

                    txJson.put("ethSend",ethSend);
                    txJson.put("receiver",receiver);
                    txJson.put("ethBlockHeight",ethBlockHeight);
                    txJson.put("eventNonce",eventNonce);
                    String contractAddress = value.getString("token_contract");
                    txJson.put("contractAddress",contractAddress);
                    txJson.put("eventNonce",eventNonce);
                    JSONObject contract = getContract(contractAddress);
                    int decimal = contract.getIntValue("decimals");
                    amount = value.getBigDecimal("amount").divide(new BigDecimal(Math.pow(10,decimal))).setScale(decimal).toPlainString();
                    txJson.put("decimal",decimal);
                    txJson.put("amount",amount);
                    txJson.put("coinName",contract.getString("symbol"));
                    break;
//                case Transfer.ValsetConfirm:
//                    txJson.put("type","ValsetConfirm");
//                    long nonce = value.getLong("nonce");
//                    String orchestrator = value.getString("orchestrator");
//                    String ethAddress = value.getString("eth_address");
//                    txJson.put("nonce",nonce);
//                    txJson.put("orchestrator",orchestrator);
//                    txJson.put("ethAddress",ethAddress);
//
//                    break;
//                case Transfer.ValsetUpdatedClaim:
//                    txJson.put("type","ValsetUpdatedClaim");
//                    ethBlockHeight = value.getLongValue("block_height");
//                    eventNonce = value.getLongValue("event_nonce");
//                    long valsetNonce = value.getLong("valset_nonce");
//                    String ethereumAddress = value.getJSONArray("members").getJSONObject(0).getString("ethereum_address");
//                    orchestrator = value.getString("orchestrator");
//
//                    txJson.put("ethBlockHeight",ethBlockHeight);
//                    txJson.put("eventNonce",eventNonce);
//                    txJson.put("valsetNonce",valsetNonce);
//                    txJson.put("ethereum_address",ethereumAddress);
//                    txJson.put("orchestrator",orchestrator);
//                    break;
                case Transfer.SendToEth:
                    txJson.put("type","SendToEth");
                    String sender = value.getString("sender");
                    String ethDest = value.getString("eth_dest");

                    txJson.put("from",sender);
                    txJson.put("to",ethDest);

                    JSONObject amountJson = value.getJSONObject("amount");
                    JSONObject bridgeFeeJson = value.getJSONObject("bridge_fee");
                    denom = amountJson.getString("denom");

                    if (denom.startsWith("gravity")){
                        denom = denom.split("gravity")[1];
                        contract = getContract(denom);
                        int decimals = contract.getIntValue("decimals");
                        String symbol = contract.getString("symbol");
                        amount = amountJson.getBigDecimal("amount").divide(new BigDecimal(Math.pow(10,decimals))).setScale(decimals).toPlainString();
                        txJson.put("coinName",symbol);
                        txJson.put("amount",amount);
                        String bridgeFee = bridgeFeeJson.getBigDecimal("amount").divide(new BigDecimal(Math.pow(10,decimals))).setScale(decimals).toPlainString();
                        txJson.put("bridgeFee",bridgeFee);
                    }

                    logs = txResponse.getJSONArray("logs");
                    if (logs.size() > 0){
                        logs.toJavaList(JSONObject.class).forEach(log ->{
                            log.getJSONArray("events").toJavaList(JSONObject.class).stream().forEach(event -> {
                                if("withdrawal_received".equals(event.getString("type"))){
                                    JSONArray attributes = event.getJSONArray("attributes");
                                    for (int i = 0; i < attributes.size(); i++) {
                                        JSONObject attribute = attributes.getJSONObject(i);
                                        if ("outgoing_tx_id".equals(attribute.getString("key"))){
                                            long outGoingTxId = attribute.getLong("value");
                                            txJson.put("outgoingTxId",outGoingTxId);
                                        }
                                    }
                                }
                            });
                        });
                    }
                    break;
                case Transfer.ConfirmBatch://gauss跨eth打包确认
                    txJson.put("type","ConfirmBatch");
                    contractAddress = value.getString("token_contract");
                    contract = getContract(contractAddress);
                    txJson.put("contractAddress",contractAddress);
                    txJson.put("coinName",contract.getString("symbol"));

                    String ethAddress = value.getString("eth_signer");
                    String orchestrator = value.getString("orchestrator");
                    long nonce = value.getLong("nonce");

                    txJson.put("from",ethAddress);
                    txJson.put("batchNonce",nonce);
                    txJson.put("orchestrator",orchestrator);

                    logs = txResponse.getJSONArray("logs");
                    if (logs.size() > 0){
                        logs.toJavaList(JSONObject.class).stream().forEach(log ->{
                            log.getJSONArray("events").toJavaList(JSONObject.class).stream().forEach(event -> {
                                if("message".equals(event.getString("type"))){
                                    JSONArray attributes = event.getJSONArray("attributes");
                                    for (int i = 0; i < attributes.size(); i++) {
                                        JSONObject attribute = attributes.getJSONObject(i);
                                        if ("outgoing_tx_ids".equals(attribute.getString("key"))){
                                            String outgoingTxIds = attribute.getString("value");
                                            txJson.put("outgoingTxIds",outgoingTxIds);
                                        }
                                    }
                                }
                            });
                        });
                    }
                    break;
                case Transfer.RequestBatch:
                    txJson.put("type","RequestBatch");
                    sender = value.getString("sender");
                    txJson.put("from",sender);
                    denom = value.getString("denom");
                    if (denom.startsWith("gravity")){
                        denom = denom.split("gravity")[1];
                        contract = getContract(denom);
                        txJson.put("coinName",contract.getString("symbol"));
                    }
                    logs = txResponse.getJSONArray("logs");
                    if (logs.size() > 0){
                        logs.toJavaList(JSONObject.class).stream().forEach(log ->{
                            log.getJSONArray("events").toJavaList(JSONObject.class).stream().forEach(event -> {
                                if("outgoing_batch".equals(event.getString("type"))){
                                    JSONArray attributes = event.getJSONArray("attributes");
                                    for (int i = 0; i < attributes.size(); i++) {
                                        JSONObject attribute = attributes.getJSONObject(i);
                                        if ("nonce".equals(attribute.getString("key"))){
                                            long nonceBatch = attribute.getLong("value");
                                            txJson.put("batchNonce",nonceBatch);
                                        }
                                    }
                                }
                            });
                        });
                    }

                    break;
                case Transfer.WithdrawClaim:
                    txJson.put("type","WithdrawClaim");
                    eventNonce = value.getLongValue("event_nonce");
                    ethBlockHeight = value.getLongValue("block_height");
                    contractAddress = value.getString("token_contract");
                    long bathNonce = value.getLongValue("batch_nonce");
                    orchestrator = value.getString("orchestrator");
                    contract = getContract(contractAddress);
                    txJson.put("coinName",contract.getString("symbol"));
                    txJson.put("eventNonce",eventNonce);
                    txJson.put("ethBlockHeight",ethBlockHeight);
                    txJson.put("contractAddress",contractAddress);
                    txJson.put("orchestrator",orchestrator);
                    txJson.put("batchNonce",bathNonce);
                    break;
                default:
                    log.info(type+"-------1111");
                    break;
            }
            messages.add(txJson);
        });
        transaction.put("event",messages);
        return transaction;
    }

    public static JSONObject getContract(String contractAddress){
        JSONObject contract = null;
        if ( contractService.isExistContract(contractAddress) > 0){
            contract = contractService.getContract(contractAddress);

        }else {
            contract = new JSONObject();
            int decimals = getTokenDecimal(contractAddress);
            String symbol = getTokenSymbol(contractAddress);
            contract.put("decimals",decimals);//decmails
            contract.put("symbol",symbol);
            contract.put("contractAddress",contractAddress);
            contractService.saveContract(contract);
        }
        return contract;
    }

    /**
     * 获取代币精度
     *
     * @param contractAddress 代币合约地址
     * @return
     */
    public static int getTokenDecimal(String contractAddress){
        Function function = new Function("decimals", Arrays.asList(), Arrays.asList(new TypeReference<Uint8>() {
        }));
        int decimals = 0;
        EthCall ethCall = null;
        try {
            ethCall = web3j.ethCall(Transaction.createEthCallTransaction("0x0000000000000000000000000000000000000000", contractAddress, FunctionEncoder.encode(function)), DefaultBlockParameterName.LATEST).send();
        } catch (IOException exception) {
            log.error("【获取合约 {} Token 精度失败】", contractAddress, ethCall.getError().getMessage());
            exception.printStackTrace();
        }
        if (ethCall.hasError()) {
            log.error("【获取合约 {} Token 精度失败】", contractAddress, ethCall.getError().getMessage());
            return decimals;
        }
        List<Type> decode = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
        if (decode.isEmpty()){
            return decimals;
        }
        decimals = Integer.parseInt(decode.get(0).getValue().toString());
        log.info("decimals = " + decimals);
        return decimals;
    }

    /**
     * 获取代币符号(缩写的名称)
     *
     * @param contractAddress 代币合约地址
     * @return
     */
    public static String getTokenSymbol(String contractAddress){
        Function function = new Function("symbol", Arrays.asList(), Arrays.asList(new TypeReference<Utf8String>() {
        }));
        String tokenSymbol = contractAddress;
        EthCall ethCall = null;
        try {
            ethCall = web3j.ethCall(Transaction.createEthCallTransaction("0x0000000000000000000000000000000000000000", contractAddress, FunctionEncoder.encode(function)), DefaultBlockParameterName.LATEST).send();
        } catch (IOException exception) {
            log.error("【获取合约 {} Token Symbol失败】", contractAddress, ethCall.getError().getMessage());
            exception.printStackTrace();
        }
        if (ethCall.hasError()) {
            log.error("【获取合约 {} Token Symbol失败】", contractAddress, ethCall.getError().getMessage());
            return tokenSymbol;
        }
        List<Type> decode = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
        if (decode.isEmpty()){
            return tokenSymbol;
        }
        tokenSymbol = decode.get(0).getValue().toString();
        return tokenSymbol;
    }


}
