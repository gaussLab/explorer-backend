package com.wuyuan.blockbrowse.service;

public interface IEventDispatch {
    void dispatch_events(String eventName, Object data);
}
