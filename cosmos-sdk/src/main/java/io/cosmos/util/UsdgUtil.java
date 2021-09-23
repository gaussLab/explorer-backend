package io.cosmos.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.cosmos.common.HttpUtils;
import io.cosmos.msg.MsgBase;

import java.io.IOException;
import java.math.BigDecimal;

public class UsdgUtil {
    //    static StringString uri="http://192.168.2.126:1317";
    public   static String url="http://192.168.2.126:5317";
    //    static StringString uri="https://rpc.cosmos.network";

//    public static void setUrl(String httpurl){

//    }

    public static long getNewBlock(){
        String uri=url+"/blocks/latest";
        String res= HttpUtils.httpGetDisable(uri);
        JSONObject json= JSON.parseObject(res);
        JSONObject blockJson=json.getJSONObject("block");
        if(blockJson!=null){
            return blockJson.getJSONObject("header").getLong("height");
        }
        return 0;
    }
    public static JSONObject getBlockByHeight(long blockHeight){
        String uri=url+"/blocks/"+blockHeight;
        String res=HttpUtils.httpGetDisable(uri);
        return JSON.parseObject(res);
    }
    public static JSONObject getByTxHash(String hash){
        String uri=url+"/txs/"+hash;
        String res=HttpUtils.httpGetDisable(uri);
        return JSON.parseObject(res);
    }
    public static JSONArray txSeach(long  minHeight, long maxheight){
//        query=1&prove=true&page=1&per_page=30&order_by=desc
        //https://api.cosmos.network/txs?message.action=send&page=1&limit=1&tx.minheight=5222918&tx.maxheight=5222920

        int page=0;
        int pageTotal=1;
        JSONArray jsonArray=new JSONArray();
        while (pageTotal>page){
            page=pageTotal;
            String uri=url+"/txs?page=1&page="+page+"&limit=30&tx.minheight="+minHeight+"&tx.maxheight="+maxheight;
            String res=HttpUtils.httpGetDisable(uri);
            JSONObject json= JSON.parseObject(res);
            pageTotal=json.getIntValue("page_total");
            if(pageTotal>0){
                jsonArray.addAll(json.getJSONArray("txs"));
            }
        }
        return jsonArray;
    }

    public static JSONObject getBalance(String address){
        String uri=url+"/bank/balances/"+address;
        String res=HttpUtils.httpGet(uri);
        return JSON.parseObject(res);
    }
    public static BigDecimal getBalanceByCoin(String address,String coin){
        if(!"udga".equalsIgnoreCase(coin.trim())){
            coin=""+coin.trim();
        }
        String uri=url+"/usdg/bank/v1beta1/balances/"+address+"/"+coin.toLowerCase();
        String res=HttpUtils.httpGet(uri);
        JSONObject balance=JSON.parseObject(res);
        return balance.getJSONObject("balance").getBigDecimal("amount").divide(new BigDecimal(1000000)).setScale(6);
    }
    public static String  broadcast(String tx) throws IOException {
//        /txs
        String uri=url+"/txs";
        String res=HttpUtils.sendPostDataByJson(url,tx,"UTF-8");
        return JSON.parseObject(res).getString("hash");
    }
    ///cosmos/bank/v1beta1/balances/cosmos1vrf87mrnep5zh3lm9wfk388r7xxqhz2jwauk4h/uatom
//    ///{address}
//    public static JSONObject accountInfo(String address){
//        String uri=url+"/cosmos/auth/v1beta1/accounts/"+address;
//        String res=HttpUtils.httpGet(uri);
//        return JSON.parseObject(res);
//    }
}
