package com.wuyuan.blockbrowse.entity;



import com.wuyuan.blockbrowse.service.impl.EventManager;
import com.wuyuan.blockbrowse.service.impl.PushMessageJob;

import java.util.HashMap;
import java.util.Map;

public class EventThreadPools {

    //TODO 存入redis，多个服务互相读取，是否要线程安全，
    public static Map<String,EventThread> eventThreadMap = new HashMap<>();

    public synchronized static void add(EventPrams eventPrams, EventManager eventManager, PushMessageJob pushMessageJob){
        EventThread eventThread = null;
        if (eventThreadMap.size() > 0 && eventThreadMap.containsKey(eventPrams.getPage())){
            eventThread = eventThreadMap.get(eventPrams.getPage());
            eventThread.hold(); //int++ // 注销时要up释放
        }else {
            eventThread = new EventThread(eventPrams,eventManager,pushMessageJob);
            eventThreadMap.put(eventPrams.getPage(),eventThread);
            Thread thread = new Thread(eventThread,eventPrams.getPage());
            thread.start();
        }
    }

}
