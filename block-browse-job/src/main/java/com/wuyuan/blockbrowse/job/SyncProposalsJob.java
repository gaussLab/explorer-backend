package com.wuyuan.blockbrowse.job;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wuyuan.database.sevice.ConfigService;
import com.wuyuan.database.sevice.VoteService;
import com.wuyuan.database.util.ConfigUtil;
import io.cosmos.util.CosmosUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Iterator;
import java.util.List;

@Component
@EnableScheduling
@Slf4j
public class SyncProposalsJob {

//    private String chainPrefix;

    @Resource
    private VoteService voteService;

    @Resource
    private ConfigService configService;

    @Scheduled(fixedDelay = 1000 * 60)
    public void getProposals(){
        log.info("提案 启动");
        JSONObject proposalsJson = CosmosUtil.getAllProposals(configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix(),100);
        JSONArray proposalsList = proposalsJson.getJSONArray("proposals");
        if (proposalsJson.getJSONObject("pagination").getIntValue("total") != proposalsList.size()){
            proposalsList = CosmosUtil.getAllProposals(configService.getConfig(ConfigUtil.chainUrlKey)+"/"+configService.getChainPrefix(),proposalsJson.getJSONObject("pagination").getInteger("total")).getJSONArray("proposals");
        }
        if (proposalsList != null && proposalsList.size() > 0){
            List<String> proposalIdList = voteService.getProposalIdList();

            proposalsList.stream().forEach(proposal -> {
                JSONObject proposalData = (JSONObject) proposal;
                String proposalId = proposalData.getString("proposal_id");
                Iterator<String> iterator = proposalIdList.iterator();
                while (iterator.hasNext()){
                    if (iterator.next().equals(proposalId)){
                        iterator.remove();
                    }
                }
                if (voteService.isExistProposal(proposalData.getString("proposal_id")) == 0){
                    voteService.saveProposal(proposalData);
                }else {
                    voteService.updateProposal(proposalId,proposalData);
                    return;
                }
            });
            if (proposalIdList.size() > 0){
                for (String id : proposalIdList) {
                    JSONObject proposal = voteService.getProposalByProposalId(id);
                    proposal.put("status","PROPOSAL_STATUS_REMOVED");
                    voteService.updateProposal(id,proposal);
                }
            }
        }
        log.info("提案同步完成");
    }

//    public String getChainPrefix(){
//        if (chainPrefix == null){
//            chainPrefix = configService.getConfig(ConfigUtil.chainPrefixKey);
//        }
//        return chainPrefix;
//    }
}
