package com.wuyuan.blockbrowse.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.wuyuan.blockbrowse.entity.EventPrams;
import com.wuyuan.blockbrowse.service.impl.EventManager;
import com.wuyuan.blockbrowse.service.impl.PushMessageJob;
import com.wuyuan.blockbrowse.service.impl.QueueBlock;
import com.wuyuan.blockbrowse.service.impl.WebSocketChannelHandlerPool;
import com.wuyuan.blockbrowse.utils.GetBeanUtil;
import com.wuyuan.database.sevice.BlockService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Slf4j
public class WebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    public static String blockMax = new String();

    private PushMessageJob pushMessageJob;

    private BlockService blockService = GetBeanUtil.getBean(BlockService.class);

    private static EventManager eventManager = new EventManager();

    private ChannelHandlerContext ctx;

    private String page;

    private QueueBlock queueBlock = GetBeanUtil.getBean(QueueBlock.class);

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public void setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("与客户端建立连接，通道开启！");
        this.ctx = ctx;
        //添加到channelGroup通道组
        WebSocketChannelHandlerPool.channelGroup.add(ctx.channel());
        log.info("在线人数： "+WebSocketChannelHandlerPool.channelGroup.size());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("与客户端断开连接，通道关闭！");
        //添加到channelGroup 通道组
        WebSocketChannelHandlerPool.channelGroup.remove(ctx.channel());
        super.channelInactive(ctx);
        EventManager.unsubscribe_event(page,this);
        log.info("在线人数： "+WebSocketChannelHandlerPool.channelGroup.size());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //首次连接是FullHttpRequest，处理参数 by zhengkai.blog.csdn.net
        if (null != msg && msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            String uri = request.uri();
            Map paramMap=getUrlParams(uri);
            System.out.println("接收到的参数是："+ JSON.toJSONString(paramMap));
            //如果url包含参数，需要处理
            if(uri.contains("?")){
                String newUri=uri.substring(0,uri.indexOf("?"));
                System.out.println(newUri);
                request.setUri(newUri);
            }
        }else if(msg instanceof TextWebSocketFrame){
            //正常的TEXT消息类型
            TextWebSocketFrame frame=(TextWebSocketFrame)msg;
            log.info("客户端收到服务器数据：" +frame.text());
            EventPrams eventPrams = null;
            try {
                eventPrams = JSONObject.parseObject(frame.text(),EventPrams.class);
                if (eventPrams == null || eventPrams.getPage() == null){
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("code","-200");
                    jsonObject.put("message","json缺少必要参数page");
                    sendOne(this.getCtx(),JSON.toJSONString(jsonObject));
                    return;
                }
                switch (eventPrams.getPage().toLowerCase()){
                    case "homepage":
                        //todo 推送10个块
                        if (StringUtils.isBlank(blockMax)){
                            blockMax = blockService.getMaxBlockNumber();
                        }

                        sendOne(this.getCtx(),queueBlock.getBlockList(blockMax));
                        this.page = eventPrams.getPage().toLowerCase();
                        eventPrams.setPage(page);
                        eventPrams.setBlockNum(blockMax);
                        //todo 放入持续推送的线程中
                        eventManager.subscribe_event(eventPrams,this);
                        break;
                    default:
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("code","-200");
                        jsonObject.put("message","未知page");
                        sendOne(this.getCtx(),JSON.toJSONString(jsonObject));
                        break;
                }
            }catch (Exception e){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("code","-200");
                jsonObject.put("message","服务器繁忙");
                jsonObject.put("error",e.getMessage());
                sendOne(this.getCtx(),JSON.toJSONString(jsonObject));
                eventPrams =  JSONObject.parseObject(frame.text(),EventPrams.class);
                eventManager.subscribe_event(eventPrams,this);
                e.printStackTrace();
            }
        }
        super.channelRead(ctx, msg);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame textWebSocketFrame) throws Exception {

    }

    private void sendAllMessage(String message){
        //收到信息后，群发给所有channel
        WebSocketChannelHandlerPool.channelGroup.writeAndFlush( new TextWebSocketFrame(message));
    }

    public void sendOne(ChannelHandlerContext xt,String message){
        xt.channel().writeAndFlush(new TextWebSocketFrame(message));
    }

    public PushMessageJob getPushMessageJob(){
        if (pushMessageJob == null){
            pushMessageJob = GetBeanUtil.getBean(PushMessageJob.class);
        }
        return pushMessageJob;
    }



    private static Map getUrlParams(String url){
        Map<String,String> map = new HashMap<>();
        url = url.replace("?",";");
        if (!url.contains(";")){
            return map;
        }
        if (url.split(";").length > 0){
            String[] arr = url.split(";")[1].split("&");
            for (String s : arr){
                String key = s.split("=")[0];
                String value = s.split("=")[1];
                map.put(key,value);
            }
            return  map;

        }else{
            return map;
        }
    }

}
