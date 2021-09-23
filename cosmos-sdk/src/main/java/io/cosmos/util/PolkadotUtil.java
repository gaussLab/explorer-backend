package io.cosmos.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.cosmos.common.HttpUtils;

import java.io.IOException;
import java.math.BigDecimal;

public class PolkadotUtil {

    private static String url;

    public static void main(String[] args) {
        initUrl("http://103.84.84.130:8080");
        //18446744073709551616.0000000000
        //18446744073709551616.0000000000
        //18446744073709551616
        //18413824918158984241.6038124842
//        BigDecimal b=new BigDecimal("60808125.2295532588").multiply(new BigDecimal(10000000000l));
//        BigDecimal dec=new BigDecimal("18446744073709551616");
//        JSONObject json=getBalnce("12xtAYsRUrmbniiWQqJtECiBQrMn8AypQcXhnQAc6RB6XkLW");
        System.out.println(  getNewBlockNum());
//        System.out.println(json.getBigDecimal("miscFrozen"));
//        System.out.println(json.getBigDecimal("feeFrozen"));
//        System.out.println(json.getBigDecimal("free").divide(dec,10,BigDecimal.ROUND_HALF_UP));
//        System.out.println(b.toPlainString());
//        System.out.println(json.getBigDecimal("free").divide(json.getBigDecimal("feeFrozen"),10,BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("1800000000000000")));

//        System.out.println(new BigDecimal(1700000000000000l).divide(new BigDecimal(10000000000l)));
    }

    public static void initUrl(String host){
        url=host;
    }

    public static JSONObject getBlockByNum(long blocknum){
        String res=HttpUtils.httpGet(url+"/blocks/"+blocknum);
        return JSONObject.parseObject(res);
    }
    public static long  getNewBlockNum(){
        String res=HttpUtils.httpGet(url+"/blocks/head");
        return JSONObject.parseObject(res).getLong("number");
    }
    public static JSONObject getBalnce(String address){
//        http://103.84.84.130:8080/accounts/1eLc4sXWSNDXAYtBwt7MCe1SppRUguBQhQUyK7USED7SceY/balance-info
        String res=HttpUtils.httpGet(url+"/accounts/"+address+"/balance-info");
        JSONObject json=JSONObject.parseObject(res);
        return json;
//        return JSONObject.parseObject(res).getLong("number");
    }
    public static JSONObject sendtransaction(String tx){
        JSONObject json=new JSONObject();
        json.put("tx",tx);
        try {
           String res= HttpUtils.sendPostDataByJson(url+"/transaction",json.toJSONString(),"UTF-8");
           return JSONObject.parseObject(res);
        } catch (IOException e) {
            e.printStackTrace();
            json.put("code","500");
            json.put("message",e.getMessage());
            return json;
        }
    }
}
