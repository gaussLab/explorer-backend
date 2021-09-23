package com.wuyuan.blockbrowse.utils;

public class DeviceUtil {
    private final static String[] agent = { "Android", "iPhone", "iPod","iPad", "Windows Phone", "MQQBrowser" }; //定义移动端请求的所有可能类型
 
    /**
     * 判断User-Agent 是不是来自于手机
     * @param userAgent
     * @return
     */
    public static boolean checkAgentIsMobile(String userAgent) {
        String user = userAgent.toLowerCase();
        boolean flag = false;
        if (!user.contains("windows nt") || (user.contains("windows nt") && user.contains("compatible; msie 9.0;"))) {
            // 排除 苹果桌面系统
            if (!user.contains("windows nt") && !user.contains("macintosh")) {
                for (String item : agent) {
                    System.out.println(item.toLowerCase());
                    if (user.contains(item.toLowerCase())) {
                        flag = true;
                        break;
                    }
                }
            }
        }
        return flag;
    }
 
}