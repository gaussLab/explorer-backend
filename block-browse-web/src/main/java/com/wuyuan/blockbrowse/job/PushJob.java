package com.wuyuan.blockbrowse.job;

import com.wuyuan.blockbrowse.controller.WebSocketHandler;
import com.wuyuan.blockbrowse.service.impl.PushMessageJob;
import com.wuyuan.database.sevice.BlockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@EnableScheduling
@Slf4j
public class PushJob {
    @Resource
    private PushMessageJob pushMessageJob;

    private String blockMax;

    private String lastBlock;

    @Scheduled(fixedDelay = 1000)
    public void syncPush() {
        log.info("-------------------PushStart");
        blockMax = pushMessageJob.pushMessage(lastBlock,"homepage",WebSocketHandler.blockMax);
        lastBlock = blockMax;
    }
}
