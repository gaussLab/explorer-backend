package com.wuyuan.database.sevice;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.google.gson.JsonArray;
import com.mongodb.client.result.UpdateResult;
import com.wuyuan.database.util.Collocation;
import com.wuyuan.database.util.ConfigUtil;
import com.wuyuan.database.util.PageModel;
import io.cosmos.util.CosmosUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VoteService {
    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private TransactionService transactionService;

    @Resource
    private ConfigService configService;

//    private String chainName;

    private String collectionName;
//
//    private String chainPrefix;

    private List<JSONObject> proposalList;

    public void saveProposal(JSONObject jsonObject){
        mongoTemplate.save(jsonObject,getCollectionName());
    }

    public Long isExistProposal(String proposalId){
        Query query = new Query(Criteria.where("proposal_id").is(proposalId));
        return mongoTemplate.count(query,getCollectionName());
    }


    public JSONObject getProposalByProposalId(String proposalId){
        Query query = new Query(Criteria.where("proposal_id").is(proposalId));
        JSONObject proposal = getType(mongoTemplate.findOne(query,JSONObject.class,getCollectionName()));
        if (proposal == null){
            return null;
        }
        List<JSONObject> tx = transactionService.getTransaction(null,null,"submitproposal",null,proposalId,null,0,1,1,null,null,null).getRecords();

        if (tx != null && tx.size() > 0){
            JSONArray messageArray = tx.get(0).getJSONArray("event");
            String hash = tx.get(0).getString("txhash");
            proposal.put("txhash",hash);
            messageArray.stream().forEach( typeMessage -> {
                JSONObject value = JSON.parseObject(JSON.toJSONString(typeMessage), Feature.DisableSpecialKeyDetect);
                String type = value.getString("type");
                if ("Submit Proposal".equals(type)){
                    proposal.put("proposer",value.getString("from"));
                }
            });
        }
        BigDecimal yes = BigDecimal.ZERO;
        BigDecimal abstain = BigDecimal.ZERO;
        BigDecimal no = BigDecimal.ZERO;
        BigDecimal no_with_veto = BigDecimal.ZERO;
        if (proposal.getString("status").equals("PROPOSAL_STATUS_VOTING_PERIOD")){
            JSONObject result = JSON.parseObject(CosmosUtil.getProposalsTally(configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix(),proposal.getInteger("proposal_id")));
            if (result != null && result.containsKey("tally")){
                yes = new BigDecimal(result.getJSONObject("tally").getString("yes"));
                abstain = new BigDecimal(result.getJSONObject("tally").getString("abstain"));
                no = new BigDecimal(result.getJSONObject("tally").getString("no"));
                no_with_veto = new BigDecimal(result.getJSONObject("tally").getString("no_with_veto"));
                proposal.put("final_tally_result",result.getJSONObject("tally"));
            }
        }else {
            yes = new BigDecimal(proposal.getJSONObject("final_tally_result").getString("yes"));
            abstain = new BigDecimal(proposal.getJSONObject("final_tally_result").getString("abstain"));
            no = new BigDecimal(proposal.getJSONObject("final_tally_result").getString("no"));
            no_with_veto = new BigDecimal(proposal.getJSONObject("final_tally_result").getString("no_with_veto"));
        }

        BigDecimal totalVote = yes.add(abstain).add(no).add(no_with_veto);
        BigDecimal total = totalVote.divide(new BigDecimal(1000000));
        proposal.put("totalVote",total.toPlainString());
        proposal.put("mTotalVote",total.divide(new BigDecimal(1000000),0,RoundingMode.UP));
        if (proposal.getString("status").equals("PROPOSAL_STATUS_VOTING_PERIOD")){
            String bondedTokens = JSON.parseObject(CosmosUtil.getPool(configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix())).getJSONObject("pool").getString("bonded_tokens");
            String currentTurnout  = totalVote.divide(new BigDecimal(bondedTokens),4,RoundingMode.HALF_UP).toPlainString();
            BigDecimal boned = new BigDecimal(bondedTokens).divide(new BigDecimal(1000000));
            proposal.put("bondedTokens",boned.toPlainString());
            proposal.put("mBondedTokens",boned.divide(new BigDecimal(1000000),0,RoundingMode.UP));
            proposal.put("CurrentTurnout",currentTurnout);
            proposal.put("Quorum","40%");
        }
        BigDecimal yesRate = BigDecimal.ZERO;
        BigDecimal noRate = BigDecimal.ZERO;
        BigDecimal noWithVoteRate = BigDecimal.ZERO;
        BigDecimal abstainRate = BigDecimal.ZERO;
        if (totalVote != BigDecimal.ZERO){
           yesRate = yes.divide(totalVote,4,RoundingMode.HALF_UP);
           noRate = no.divide(totalVote,4,RoundingMode.HALF_UP);
           noWithVoteRate = no_with_veto.divide(totalVote,4,RoundingMode.HALF_UP);
           abstainRate = new BigDecimal(1).subtract(yesRate).subtract(noRate).subtract(noWithVoteRate);
        }
        proposal.put("coinName",configService.getChainName());
        proposal.put("chainName",configService.getChainName());
        proposal.put("yesRate",yesRate);
        proposal.put("noRate",noRate);
        proposal.put("noWithVoteRate",noWithVoteRate);
        proposal.put("abstainRate",abstainRate);
        return proposal;
    }

    public PageModel<JSONObject> getProposalList(Integer pageIndex,Integer pageSize){
        Query query = new Query();
        long count = mongoTemplate.count(query,getCollectionName());
        query.skip((pageIndex - 1) * pageSize);
        query.limit(pageSize);
        query.with(Sort.by(
                Sort.Order.desc("submit_time"))
        );
        List<JSONObject> list = mongoTemplate.find(query,JSONObject.class,getCollectionName());
        list.stream().forEach(proposal -> {
            getType(proposal);
        });
        PageModel<JSONObject> pageModel=new PageModel<>(list,pageIndex,pageSize,count,configService.getChainName());
        return pageModel;
    }

    public List<String> getProposalIdList() {
        Query query = new Query();
        query.fields().exclude("_id");
        query.fields().include("proposal_id");
        List<JSONObject> list = mongoTemplate.find(query, JSONObject.class, getCollectionName());
        return list.stream().map(proposal -> {
            return proposal.getString("proposal_id");
        }).collect(Collectors.toList());
    }

    public UpdateResult updateProposal(String proposalId,JSONObject json){
        Query query = new Query(Criteria.where("proposal_id").is(proposalId));
        Update update = new Update();
        update.set("voting_end_time",json.getString("voting_end_time"));
        update.set("voting_start_time",json.getString("voting_start_time"));
        update.set("total_deposit",json.getJSONArray("total_deposit"));
        update.set("status",json.getString("status"));
        if (json.containsKey("final_tally_result")){
            update.set("final_tally_result",json.getJSONObject("final_tally_result"));
        }
        update.set("deposit_end_time",json.getString("deposit_end_time"));
        if (json.containsKey("total_deposit")){
            update.set("total_deposit",json.getJSONArray("total_deposit"));
        }
        return mongoTemplate.updateFirst(query,update,getCollectionName());
    }

    public String getCollectionName(){
        if (collectionName == null){
            collectionName = configService.getChainName() + "_" + Collocation.collection_proposal;
        }
        return collectionName;
    }

    public JSONObject getType(JSONObject proposal){
        if (proposal == null){
            return  null;
        }
        String type = proposal.getJSONObject("content").getString("@type");
        type = type.substring(type.indexOf("v1beta1.") + 8);
        type = type.substring(0,type.length() - 8);
        proposal.put("type",type);
        return proposal;
    }

    public List<JSONObject> getProposalList(){
        if (proposalList == null){
            Query query = new Query();
            query.fields().exclude("_id");
            query.fields().include("proposal_id");
            query.fields().include("content");
            proposalList = mongoTemplate.find(query,JSONObject.class,getCollectionName());
            proposalList.stream().forEach( proposal ->{
                getType(proposal);
            });
        }
        return proposalList;
    }

//    public String getChainName(){
//        if (chainName == null){
//            chainName = configService.getConfig(ConfigUtil.chainNameKey);
//        }
//        return chainName;
//    }

//    public String getChainPrefix(){
//        if (chainPrefix == null){
//            chainPrefix = configService.getConfig(ConfigUtil.chainPrefixKey);
//        }
//        return chainPrefix;
//    }
}
