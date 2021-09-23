package com.wuyuan.blockbrowse.entity;

import com.wuyuan.blockbrowse.service.impl.EventManager;
import com.wuyuan.blockbrowse.service.impl.PushMessageJob;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.atomic.AtomicInteger;

@Data
@NoArgsConstructor
public class EventThread implements Runnable{
    private EventManager eventManager;
    private EventPrams eventPrams;//表示页面
    private AtomicInteger ref;
    private PushMessageJob pushMessageJob;

    private volatile boolean flag = true;

    public EventThread(EventPrams eventPrams,EventManager eventManager,PushMessageJob pushMessageJob){
        this.eventManager = eventManager;
        this.eventPrams = eventPrams;
        this.ref = new AtomicInteger(1);
        this.pushMessageJob = pushMessageJob;
    }

    //当有监听相同事件的服务加入时+1
    public void hold(){
        ref.getAndIncrement();
    }

    public void put(){
        ref.getAndDecrement();
//        System.out.println(ref);
//        if(ref.get() == 0 ){
//            //ref==0时代表没有人在监听了，这时候先把监听事件关闭，然后关闭这个线程
//            release();
//        }
    }

//    private void release(){
////        pushMessageJob.closePush();
//        EventThreadPools.eventThreadMap.remove(eventPrams.getPage());
//    }


    @Override
    public void run() {
//        pushMessageJob.startPush(eventPrams);
    }
}
