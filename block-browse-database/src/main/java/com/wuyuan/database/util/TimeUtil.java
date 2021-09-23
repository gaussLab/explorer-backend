package com.wuyuan.database.util;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class TimeUtil {
    public static Date StrToDateFirst(String str) {
        DateFormat format = new SimpleDateFormat("yyyyMMdd");
        Date date = null;
        try {
            date = format.parse(str);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    // 字符串转日期(格式:"dd/MM/yyyy")
    public static Date StrToDateSecond(String str) {
        DateFormat format = new SimpleDateFormat("dd/MM/yyyy");
        Date date = null;
        try {
            date = format.parse(str);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    // 字符串转日期(格式:"yyyy-MM-dd")
    public static Date StrToDateThird(String str) {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date date = null;
        try {
            date = format.parse(str);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    // 字符串转日期(格式:"yyyy-MM-dd HH:mm:ss")
    public static Date StrToDateFourth(String str) {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = null;
        try {
            date = format.parse(str);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    // 字符串转Integer
    public static Integer StrToInteger(String str) {
        Integer integer = null;
        try {
            integer = Integer.valueOf(str);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return integer;
    }

    // 字符串转Double
    public static Double StrToDouble(String str) {
        Double double1 = 0.00;
        try {
            double1 = Double.parseDouble(str);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return double1;
    }

    // 字符串转时间戳
    public static Timestamp StrToTimeStamp(String str) {
        Timestamp timestamp = null;
        try {
            timestamp = Timestamp.valueOf(str);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return timestamp;
    }

    // 字符串转BigDecimal
    public static BigDecimal StrTiBigdecimal(String str) {
        BigDecimal bigDecimal = null;
        try {
            bigDecimal = new BigDecimal(str);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bigDecimal;
    }
    // [end]

    public static Date addOneDate(Date date){
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        calendar.add(calendar.DATE,1); //把日期往后增加一天,整数  往后推,负数往前移动
        return calendar.getTime();
    }

    public static Long getUTCTime(String date){
        Calendar cal = Calendar.getInstance();
        cal.setTime(getUTCTimeStamp(date));
        return Long.valueOf(cal.getTimeInMillis());
    }

    public static Date getUTCTimeStamp(String time){
        time = time.substring(0,time.indexOf("."));
        time = time+"Z";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        Date date = null;
        try {
            date = sdf.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public static String getUtcDate(Long timeStamp){
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return df.format(new Date(timeStamp));
    }


}
