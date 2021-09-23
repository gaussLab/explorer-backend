package com.wuyuan.database.util;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateFormatUtil {
    public static void main(String[] args) throws ParseException {
        String time = "0x0000000000000000000000000000000000000000000000000000000000000017";
        if (time.startsWith("0x")){
            time = time.substring(2);
            System.out.println(time);
        }
        System.out.println(Integer.parseInt(time,16));
    }
	//13位毫秒时间戳  -->  yyyy-MM-dd HH:mm:ss
    public static String timeToFormat(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        return sdf.format(time);
    }
    //yyyy-MM-dd HH:mm:ss  -->  13位毫秒时间戳
    public static long timeToSecond(String date) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.parse(date).getTime();
    }

//    public static String stampToDate(long s){
//        //1630502354
//        String res;
//        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        Date date = new Date(s);
//        res = simpleDateFormat.format(date);
//        return res;
//    }
}