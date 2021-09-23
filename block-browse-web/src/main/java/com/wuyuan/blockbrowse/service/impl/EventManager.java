package com.wuyuan.blockbrowse.service.impl;

import com.wuyuan.blockbrowse.controller.WebSocketHandler;
import com.wuyuan.blockbrowse.entity.EventPrams;
import com.wuyuan.blockbrowse.entity.EventThreadPools;
import com.wuyuan.blockbrowse.service.IEventMessage;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class EventManager implements IEventMessage {

    public static Map<String, ChannelGroup> eventsTable = new ConcurrentHashMap<>();

    @Override
    public synchronized void subscribe_event(EventPrams eventPrams, WebSocketHandler webSocketHandler) {
        ChannelGroup webSocketList = getWebSocketList(eventPrams.getPage());
        webSocketList.add(webSocketHandler.getCtx().channel());
//        EventThreadPools.add(eventPrams,this);
    }

    public synchronized static void unsubscribe_event(String page, WebSocketHandler webSocketHandler) {

        if (eventsTable != null && eventsTable.size() > 0 && eventsTable.containsKey(page)){
            ChannelGroup list = eventsTable.get(page);
            list.remove(webSocketHandler.getCtx().channel());
//            EventThreadPools.eventThreadMap.get(page).put();
        }
    }
    
    public static void unsubscribe_all_event(WebSocketHandler webSocketHandler){
        eventsTable.forEach((key,value) ->{
            ChannelGroup list = value;
            list.remove(webSocketHandler.getCtx().channel());
//            EventThreadPools.eventThreadMap.get(key).put();
        });
    }
    

    public static void dispatch_events(String page, String data) {
        ChannelGroup webSocketServerList = eventsTable.get(page);
        if(webSocketServerList != null){
            webSocketServerList.writeAndFlush(new TextWebSocketFrame(data));
        }
    }

    public ChannelGroup getWebSocketList(String page){
        ChannelGroup webSocketServerList = eventsTable.get(page);
        if(webSocketServerList == null){
            synchronized (this){
                webSocketServerList = eventsTable.get(page);
                if(webSocketServerList == null){
                    webSocketServerList = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
                    eventsTable.put(page, webSocketServerList);
                }
            }
        }
        return webSocketServerList;
    }
}
