package com.wuyuan.blockbrowse.service;


import com.wuyuan.blockbrowse.controller.WebSocketHandler;
import com.wuyuan.blockbrowse.entity.EventPrams;
import com.wuyuan.blockbrowse.service.impl.PushMessageJob;


public interface IEventMessage {

    void subscribe_event(EventPrams eventPrams, WebSocketHandler webSocketHandler);

//    void unsubscribe_event(String page, WebSocketHandler WebSocketServer);

}
