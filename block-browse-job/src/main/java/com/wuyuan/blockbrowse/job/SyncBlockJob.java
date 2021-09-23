package com.wuyuan.blockbrowse.job;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wuyuan.database.entity.GlobalBlock;
import com.wuyuan.database.sevice.*;
import com.wuyuan.database.util.Collocation;
import com.wuyuan.database.util.ConfigUtil;
import io.cosmos.util.BalanceUtil;
import io.cosmos.util.CosmosUtil;
import io.cosmos.util.Sha256;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Component
@EnableScheduling
@Slf4j
public class SyncBlockJob {
    @Resource
    private BlockService blockService;
    @Resource
    private MongoSevice mongoSevice;

    @Resource
    private ConfigService configService;

    @Resource
    private AddressService addressService;

    @Resource
    private ValidatorDelegatorService validatorDelegatorService;

//    private String chainName;
//
//    private String chainPrefix;
//
//    private String coinName;

    private Long startNum;

    @Scheduled(fixedDelay = 1000)
    public void syncBlock() {
        log.info("start SyncBlockJob");
        GlobalBlock globalBlock = blockService.getGlobal(null);
        long newBlockNum = CosmosUtil.getNewBlock(configService.getConfig(ConfigUtil.chainUrlKey));
        startNum = new BigDecimal(configService.getConfig(ConfigUtil.coinStartNumKey)).longValue();
        if (globalBlock != null) {
            startNum = globalBlock.getBlockNum();
        }else{
            globalBlock =new GlobalBlock();
            globalBlock.setChainName(configService.getChainName());
            globalBlock.setBlockNum(startNum);
        }
        for (long i = startNum; i < newBlockNum; i++) {
            JSONObject b = CosmosUtil.getBlockByHeight(configService.getConfig(ConfigUtil.chainUrlKey),i);
            JSONArray txs = b.getJSONObject("block").getJSONObject("data").getJSONArray("txs");
            if(txs!=null && txs.size()>0){
                saveTransaction(txs,newBlockNum);
            }

            if (mongoSevice.getBlock(String.valueOf(i), configService.getChainName()+"_" + Collocation.collection_block) == 0) {
                mongoSevice.save(b, configService.getChainName()+"_" + Collocation.collection_block);
            }else {
            }

            if (i != 0){
                blockService.updateBlock(String.valueOf(i-1),b.getJSONObject("block").getJSONObject("last_commit").getJSONArray("signatures"));
            }
            globalBlock.setBlockNum(i);
            globalBlock=blockService.saveGlobal(null,globalBlock);
            log.info("block"+i+"同步完成");
        }
    }

    public void saveTransaction(JSONArray txs,Long newBlockNum){
        List<String>  transactions = Collections.synchronizedList(txs.toJavaList(String.class));
        transactions.parallelStream().forEach( tx -> {
            String hash= Sha256.tx2Sha256(tx).toUpperCase();
            if(mongoSevice.getTransaction(hash,configService.getChainName()+"_"+ Collocation.collection_transaction) == 0){
                JSONObject transfer = CosmosUtil.getTxByhash(configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix(),hash);
                log.info("save tx");
                mongoSevice.save(transfer,configService.getChainName()+"_" + Collocation.collection_transaction);

                log.info("save address");
                saveAddress(transfer,newBlockNum);
            }
        });
    }

    public void saveAddress(JSONObject transfer,Long newBlockNum){
        JSONArray txs = transfer.getJSONObject("tx").getJSONObject("body").getJSONArray("messages");
        JSONArray logs = transfer.getJSONObject("tx_response").getJSONArray("logs");
        txs.stream().forEach(tx ->{
            if (tx instanceof JSONObject){
                String address = null;
                switch (((JSONObject) tx).getString("@type")){
                    case TransactionService.msgSend:
                    case TransactionService.msgMintCoin:
                        address = ((JSONObject) tx).getString("from_address");
                        saveOrUpadte(address,newBlockNum,null);
                        address = ((JSONObject) tx).getString("to_address");
                        saveOrUpadte(address,newBlockNum,null);
                        break;
                    case TransactionService.msgTransfer:
                        address = ((JSONObject) tx).getString("sender");
                        saveOrUpadte(address,newBlockNum,null);
                        address = ((JSONObject) tx).getString("receiver");
                        saveOrUpadte(address,newBlockNum,null);
                        break;
                    case TransactionService.msgMulti:
                        JSONArray inputs = ((JSONObject) tx).getJSONArray("inputs");
                        if (inputs != null && inputs.size() > 0){
                            for (int i = 0; i < inputs.size(); i++) {
                                address = inputs.getJSONObject(i).getString("address");
                                saveOrUpadte(address,newBlockNum,null);
                            }
                        }
                        JSONArray outputs = ((JSONObject) tx).getJSONArray("outputs");
                        if (outputs != null && outputs.size() > 0){
                            for (int i = 0; i < outputs.size(); i++) {
                                address = outputs.getJSONObject(i).getString("address");
                                saveOrUpadte(address,newBlockNum,null);
                            }
                        }
                        break;
                    case TransactionService.msgDelegate:
                        address = ((JSONObject) tx).getString("delegator_address");
                        saveOrUpadte(address,newBlockNum,1);

                        Integer code = transfer.getJSONObject("tx_response").getInteger("code");
                        if (code == 0){
                            saveOrUpdateDelegator(address,(JSONObject) tx);
                        }
                        break;
                    case TransactionService.msgUnDelegate:
                        address = ((JSONObject) tx).getString("delegator_address");
                        saveOrUpadte(address,newBlockNum,1);
                        code = transfer.getJSONObject("tx_response").getInteger("code");
                        if (code == 0){
                            unDelegator(address,(JSONObject) tx);
                        }
                        break;
                    case TransactionService.msgCreateValidator:
                        address = ((JSONObject) tx).getString("delegator_address");
                        saveOrUpadte(address,newBlockNum,1);
                        code = transfer.getJSONObject("tx_response").getInteger("code");
                        if (code == 0){
                            saveOrUpdateDelegator(address,(JSONObject) tx);
                        }
                        break;
                    case TransactionService.msgDelegateReward:
                    case TransactionService.msgBeginRedelegate:
                    case TransactionService.msgAgreeOrderPair:
                    case TransactionService.msgCreateDefi:
                    case TransactionService.msgDefiDelegate:
                    case TransactionService.msgWithdrawDefiDelegatorReward:
                    case TransactionService.msgDefiUndelegate:
                        address = ((JSONObject) tx).getString("delegator_address");
                        saveOrUpadte(address,newBlockNum,null);
                        break;
                    case TransactionService.msgWithdrawAddress:
                        address = ((JSONObject) tx).getString("delegator_address");
                        saveOrUpadte(address,newBlockNum,null);
                        address = ((JSONObject) tx).getString("withdraw_address");
                        saveOrUpadte(address,newBlockNum,null);
                        break;
                    case TransactionService.msgVote:
                        address = ((JSONObject) tx).getString("voter");
                        saveOrUpadte(address,newBlockNum,null);
                        break;
                    case TransactionService.msgDeposit:
                        address = ((JSONObject) tx).getString("depositor");
                        saveOrUpadte(address,newBlockNum,null);
                        break;
                    case TransactionService.msgSubmitProposal:
                        address = ((JSONObject) tx).getString("proposer");
                        saveOrUpadte(address,newBlockNum,null);
                        break;
                    case TransactionService.msgIssueToken:
                    case TransactionService.msgUnlockToken:
                    case TransactionService.msgEditToken:
                        address = ((JSONObject) tx).getString("owner");
                        saveOrUpadte(address,newBlockNum,null);
                        break;
                    case TransactionService.msgTransferTokenOwner:
                        address = ((JSONObject) tx).getString("old_owner");
                        saveOrUpadte(address,newBlockNum,null);
                        address = ((JSONObject) tx).getString("new_owner");
                        saveOrUpadte(address,newBlockNum,null);
                        break;
                    case TransactionService.msgMintToken:
                        address = ((JSONObject) tx).getString("owner");
                        saveOrUpadte(address,newBlockNum,null);
                        address = ((JSONObject) tx).getString("to");
                        if (StringUtils.isNotBlank(address)){
                            saveOrUpadte(address,newBlockNum,null);
                        }
                        break;
                    case TransactionService.msgPlaceOrder:
                    case TransactionService.msgAddPledge:
                    case TransactionService.msgRedeemPledge:
                        address = ((JSONObject) tx).getString("owner_address");
                        saveOrUpadte(address,newBlockNum,null);
                        break;
                    case TransactionService.msgCreatePool:
                        address = ((JSONObject) tx).getString("owner_address");
                        saveOrUpadte(address,newBlockNum,null);
                        address = ((JSONObject) tx).getString("defi_address");
                        if (StringUtils.isNotBlank(address)){
                            saveOrUpadte(address,newBlockNum,null);
                        }
                        break;
                    case TransactionService.msgCreatePool2:
                        address = ((JSONObject) tx).getString("pool_creator_address");
                        if (StringUtils.isNotBlank(address)){
                            saveOrUpadte(address,newBlockNum,null);
                        }
                        break;
                    case TransactionService.msgCreateClient:
                    case TransactionService.msgUpdateClient:
                    case TransactionService.msgConnectionOpenInit:
                    case TransactionService.msgConnectionOpenConfirm:
                    case TransactionService.msgChannelOpenInit:
                    case TransactionService.msgTimeout:
                    case TransactionService.msgChannelOpenAck:
                    case TransactionService.msgConnectionOpenAck:
                    case TransactionService.msgChannelOpenTry:
                    case TransactionService.msgConnectionOpenTry:
                    case TransactionService.msgChannelOpenConfirm:
                    case TransactionService.msgAcknowledgement:
                        address = ((JSONObject) tx).getString("signer");
                        saveOrUpadte(address,newBlockNum,null);
                        break;
                    case TransactionService.msgRecvPacket:
                        address = ((JSONObject) tx).getString("signer");
                        saveOrUpadte(address,newBlockNum,null);
                        logs.stream().forEach(logEvent -> {
                            JSONArray events = JSON.parseObject(JSON.toJSONString(logEvent)).getJSONArray("events");
                            events.stream().forEach(event -> {
                                JSONObject txEvent = JSON.parseObject(JSON.toJSONString(event));
                                if (txEvent.containsKey("type") && txEvent.getString("type").equals("recv_packet")){
                                    JSONArray attributes = txEvent.getJSONArray("attributes");
                                    for (int i = 0; i < attributes.size(); i++) {
                                        JSONObject attribute = attributes.getJSONObject(i);
                                        String key = attribute.getString("key");
                                        if ("packet_data".equals(key)){
                                            String json = attribute.getString("value");
                                            if (StringUtils.isNotBlank(json)){
                                                JSONObject packetData = JSON.parseObject(json);
                                                log.info(packetData.getString("receiver") + "------跨链握手交易");
                                                saveOrUpadte(packetData.getString("receiver"),newBlockNum,null);
                                            }
                                        }
                                    }
                                }
                            });
                        });
                        break;
                    case TransactionService.msgBurnToken:
                        address = ((JSONObject) tx).getString("sender");
                        saveOrUpadte(address,newBlockNum,null);
                        break;
                    case TransactionService.msgBurnCoin:
                        address = ((JSONObject) tx).getString("address");
                        saveOrUpadte(address,newBlockNum,null);
                        break;
                    case TransactionService.msgRevokeOrder:
                        address = ((JSONObject) tx).getString("pool_address");
                        saveOrUpadte(address,newBlockNum,null);
                        address = ((JSONObject) tx).getString("owner_address");
                        saveOrUpadte(address,newBlockNum,null);
                        break;
                    case TransactionService.msgSwapWithinBatch:
                        address = ((JSONObject) tx).getString("swap_requester_address");
                        if (StringUtils.isNotBlank(address)){
                            saveOrUpadte(address,newBlockNum,null);
                        }
                        break;
                    case TransactionService.msgDepositWithinBatch:
                        address = ((JSONObject) tx).getString("depositor_address");
                        if (StringUtils.isNotBlank(address)){
                            saveOrUpadte(address,newBlockNum,null);
                        }
                        break;
                    case TransactionService.msgWithdrawWithinBatch:
                        address = ((JSONObject) tx).getString("withdrawer_address");
                        if (StringUtils.isNotBlank(address)){
                            saveOrUpadte(address,newBlockNum,null);
                        }
                        break;
                    case TransactionService.msgSetOrchestratorAddress:
                    case TransactionService.msgValsetConfirm:
                    case TransactionService.msgValsetUpdatedClaim:
                    case TransactionService.msgConfirmBatch:
                    case TransactionService.msgWithdrawClaim:
                        address = ((JSONObject) tx).getString("orchestrator");
                        if (StringUtils.isNotBlank(address)){
                            saveOrUpadte(address,newBlockNum,null);
                        }
                        break;
                    case TransactionService.msgDepositClaim:
                        address = ((JSONObject) tx).getString("orchestrator");
                        if (StringUtils.isNotBlank(address)){
                            saveOrUpadte(address,newBlockNum,null);
                        }
                        address = ((JSONObject) tx).getString("cosmos_receiver");
                        if (StringUtils.isNotBlank(address)){
                            saveOrUpadte(address,newBlockNum,null);
                        }
                        break;
                    case TransactionService.msgRequestBatch:
                    case TransactionService.msgSendToEth:
                        address = ((JSONObject) tx).getString("sender");
                        if (StringUtils.isNotBlank(address)){
                            saveOrUpadte(address,newBlockNum,null);
                        }
                        break;
                    default:
                        break;
                }
            }
        });
    }

    public synchronized void saveOrUpadte(String address,Long newBlockNum,Integer isDelegator){

        if (StringUtils.isBlank(address) || !address.startsWith(configService.getChainName()) || address.length() > 45){
            return;
        }
        if (addressService.isExistAddress(address) > 0){
            if (Long.valueOf(addressService.getAddressBlockNumber(address)) <= newBlockNum){
                JSONObject addressjSON = null;
                if (isDelegator != null){
                    addressjSON = getAddressInfo(address,newBlockNum);

                }else {
                    JSONObject data = addressService.getAddressByAddress(address);
                    addressjSON = getAddressJson(data,address,newBlockNum);
                }
                if (addressjSON != null){
                    addressService.updateAddress(address,addressjSON);
                }

            }
        }else {
            JSONObject addressJSON = getAddressJson(null,address,newBlockNum);
            if (addressJSON != null){
                addressService.saveAddress(addressJSON);
            }
        }
    }

    public void saveOrUpdateDelegator(String address,JSONObject tx){
        JSONObject delegator = null;
        String validatorAddress = tx.getString("validator_address");
        JSONObject amount = tx.getJSONObject("amount");
        BigDecimal shares = amount.getBigDecimal("amount");
        if (validatorDelegatorService.isExist(address,validatorAddress) == 0){
            String recommanderAddress = tx.getString("recommander_address");
            delegator = new JSONObject();
            delegator.put("balance",amount);
            JSONObject delegation = new JSONObject();
            delegation.put("shares",shares);
            delegation.put("delegator_address",address);
            delegation.put("validator_address",validatorAddress);
            delegation.put("recommander_address",recommanderAddress);
            delegator.put("delegation",delegation);
            validatorDelegatorService.saveDelegator(delegator);
            return;
        }
        delegator = validatorDelegatorService.getDelegator(address,validatorAddress);
        shares = shares.add(delegator.getJSONObject("balance").getBigDecimal("amount"));
        amount.put("amount",shares);
        delegator.put("balance",amount);
        //更新
        validatorDelegatorService.saveDelegator(delegator);
    }

    public void unDelegator(String address,JSONObject tx){
        String validatorAddress = tx.getString("validator_address");
        JSONObject amount = tx.getJSONObject("amount");
        BigDecimal undelegate = amount.getBigDecimal("amount");

        JSONObject delegator = validatorDelegatorService.getDelegator(address,validatorAddress);
        JSONObject balance = delegator.getJSONObject("balance");
        BigDecimal delegateAmount = balance.getBigDecimal("amount").subtract(undelegate);
        if(delegateAmount.compareTo(BigDecimal.ZERO) == 0){
            validatorDelegatorService.deleteDelegatorByAddress(address,validatorAddress);
        }else {
            balance.put("amount",delegateAmount);
            JSONObject delegation = delegator.getJSONObject("delegation");
            delegation.put("shares",delegateAmount);

            delegator.put("delegation",delegation);
            delegator.put("balance",balance);

            validatorDelegatorService.saveDelegator(delegator);
        }
    }

    public JSONObject getAddressJson(JSONObject addressJson,String address,Long newBlockNum){
        JSONObject balance = CosmosUtil.getAllBalance(configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix(),address);
        JSONArray balances = balance.getJSONArray("balances");
        if (addressJson == null){
            addressJson = new JSONObject();
            addressJson.put("address",address);
        }
        addressJson.put("balances",balances);
        for (int i = 0; i < balances.size(); i++) {
            if (configService.getCoinName().equalsIgnoreCase(balances.getJSONObject(i).getString("denom"))){
                String value = balances.getJSONObject(i).getString("amount");
                if (value == null){
                    addressJson.put("amount",BigDecimal.ZERO.toPlainString());
                }else {
                    addressJson.put("amount",new BigDecimal(value).divide(new BigDecimal(1000000)).toPlainString());
                }
            }
        }
        addressJson.put("newBlockNum",newBlockNum);
        return updateAddressTotal(addressJson);
    }

    public JSONObject getAddressInfo(String address,Long newBlockNum){
        JSONObject balance = CosmosUtil.getAllBalance(configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix(),address);
        if (balance == null){
            return null;
        }

        JSONArray balances = balance.getJSONArray("balances");
        for (int i = 0; i < balances.size(); i++) {
            if (configService.getCoinName().equalsIgnoreCase(balances.getJSONObject(i).getString("denom"))){
                String value = balances.getJSONObject(i).getString("amount");
                if (value == null){
                    balance.put("amount",BigDecimal.ZERO.toPlainString());
                }else {
                    balance.put("amount",new BigDecimal(value).divide(new BigDecimal(1000000)).toPlainString());
                }
            }else {
                balance.put("amount",BigDecimal.ZERO.toPlainString());
            }
        }

        JSONArray delegator = BalanceUtil.getDelegatorTxByAddress(configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix(),address);
        BigDecimal delegatorBalance = BigDecimal.ZERO;
        if (delegator != null && delegator.size() > 0){
            for (int i = 0; i < delegator.size(); i++) {
                JSONObject amount = delegator.getJSONObject(i);
                if(configService.getCoinName().equalsIgnoreCase(amount.getJSONObject("balance").getString("denom"))){
                    BigDecimal b = new BigDecimal(amount.getJSONObject("balance").getString("amount")).divide(new BigDecimal("1000000"));
                    delegatorBalance = delegatorBalance.add(b);
                }
            }
        }

        balance.put("delegator_tx",delegator);
        balance.put("delegator_balance",delegatorBalance);

        JSONObject unBonding = CosmosUtil.getUnBondingBalance(configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix(),address);
        BigDecimal unDelegatorBalance = BigDecimal.ZERO;
        JSONArray unDelegator = BalanceUtil.getUnDelegators(configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix(),address);
        if (unDelegator != null && unDelegator.size() > 0){
            for (int i = 0; i < unDelegator.size(); i++) {
                JSONObject amount = unDelegator.getJSONObject(i);
                JSONArray entries = amount.getJSONArray("entries");
                if (entries != null && entries.size() > 0){
                    for (int j = 0; j < entries.size(); j++) {
                        BigDecimal ba =new BigDecimal(entries.getJSONObject(j).getString("balance")).divide(new BigDecimal("1000000"));
                        unDelegatorBalance = unDelegatorBalance.add(ba);
                    }
                }
            }
        }
        balance.put("unbonding_tx",unBonding);
        balance.put("unBonding_balance",unDelegatorBalance);

        balance.put("address",address);
        balance.put("newBlockNum",newBlockNum);
        return updateAddressTotal(balance);
    }

    public JSONObject updateAddressTotal(JSONObject addressJson){
        String address = addressJson.getString("address");
        String amount = addressJson.getString("amount");
//        BigDecimal rewards = BigDecimal.ZERO;
//        if (validatorService.isExistValidatorBySelfAddress(address) == 0){
//            rewards = BalanceUtil.getRewardBalance(address,configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix());
//        }else {
//            JSONObject validator = validatorService.getValidatorBySelfDelegateAddress(address);
//            String validatorAddress = validator.getString("operator_address");
//            rewards = BalanceUtil.getValidatorRewardBalance(validatorAddress,configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix(),configService.getCoinName());
//        }
        String delegateBalance = addressJson.getString("delegator_balance");
        if (StringUtils.isBlank(delegateBalance)){
            delegateBalance = "0";
            addressJson.put("delegator_balance",0);
        }
        String unBondingBalance = addressJson.getString("unBonding_balance");
        if (StringUtils.isBlank(unBondingBalance)){
            unBondingBalance = "0";
            addressJson.put("unBonding_balance",0);
        }
        if (StringUtils.isBlank(amount)){
            amount = "0";
            addressJson.put("amount",amount);
        }
        if (!configService.getCoinName().equals("uusdg")){
//            amount = new BigDecimal(amount).add(rewards).add(new BigDecimal(delegateBalance)).add(new BigDecimal(unBondingBalance)).toPlainString();
            amount = new BigDecimal(amount).add(new BigDecimal(delegateBalance)).add(new BigDecimal(unBondingBalance)).toPlainString();
        }
        addressJson.put("totalAmount",amount);
        return addressJson;
    }

}
