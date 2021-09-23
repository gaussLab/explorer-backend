package io.cosmos.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONAware;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonAnyFormatVisitor;
import io.cosmos.common.HttpUtils;
import io.cosmos.common.Utils;
import io.cosmos.crypto.encode.Bech32;
import io.cosmos.msg.MsgBase;
import io.cosmos.msg.utils.BoardcastTx;
import io.grpc.Grpc;
import org.apache.commons.codec.binary.Hex;
import org.bitcoinj.core.Sha256Hash;
import org.bouncycastle.util.Strings;
import org.bouncycastle.util.encoders.Base64;

import java.io.UnsupportedEncodingException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class Demo {
//    static String url="http://192.168.2.126:1317";
    static String url="https://api.cosmos.network";
//    static String url="https://rpc.cosmos.network";

    public static void main(String[] args) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        String txs="CsYCCqABCjcvY29zbW9zLmRpc3RyaWJ1dGlvbi52MWJldGExLk1zZ1dpdGhkcmF3RGVsZWdhdG9yUmV3YXJkEmUKLWNvc21vczF6dHNhbXFkbWMyanZqNzY5cHB2M2FtcGp4ZzZldmNxcnY0dGV4aBI0Y29zbW9zdmFsb3BlcjF0ZmxrMzBtcTV2Z3FqZGx5OTJra2hocTNyYWV2MmhuejZlZXRlMwqgAQo3L2Nvc21vcy5kaXN0cmlidXRpb24udjFiZXRhMS5Nc2dXaXRoZHJhd0RlbGVnYXRvclJld2FyZBJlCi1jb3Ntb3MxenRzYW1xZG1jMmp2ajc2OXBwdjNhbXBqeGc2ZXZjcXJ2NHRleGgSNGNvc21vc3ZhbG9wZXIxZXk2OXIzN2dmeHZ4ZzYyc2g0cjBrdHB1YzQ2cHpqcm04NzNhZTgSZQpOCkYKHy9jb3Ntb3MuY3J5cHRvLnNlY3AyNTZrMS5QdWJLZXkSIwohAxM4G5BvsOgOMXISi4Bgd24t6zZtV8Wc2WhoH42By3zbEgQKAgh/EhMKDQoFdWF0b20SBDI1MDAQwM8kGkAQ4fMBkNSIF242JJD4eBTUZjR+Glf+bEdm1b/gGSxxbV12g6/0WnCRmeumdpAYjoXavTSbIuR/jBjaUirJR+GB";
        //        Grpc rpc=
//        JSONObject b=getBlockByHeight(
        //B728F0426CEC1B7B259CC6E6CF3DDC57CA20A2B44E58451C4DC03150F67C86CD
//                5222918);
//
////        byte[] bs= Base64.decode("CpMBCpABChwvY29zbW9zLmJhbmsudjFiZXRhMS5Nc2dTZW5kEnAKLWNvc21vczF0NXUwamZnM2xqc2pyaDJtOWU0N2Q0bnkyaGVhN2VlaHhyemRnZBItY29zbW9zMThlODJyZ2s5czZ0dXMyOTB6OHU2NXIwcmRtcnBxeXF2OGgzNm10GhAKBXVhdG9tEgcxMDk3NTAwEmUKTgpGCh8vY29zbW9zLmNyeXB0by5zZWNwMjU2azEuUHViS2V5EiMKIQLqGodLEwWelzPDXpDLKlhlpGtk7ZDSVVR8B8pSOSf8YhIECgIIfxITCg0KBXVhdG9tEgQyNTAwEIiYBRpAf/iRrFZjvOsdgEcILQoekNlemykCYWfpyYB/YUCjmehfS1sA2ghwkKcqcSh/e++Vg+mM8zyXw33tSBwJcK02Sw==");
////        String tx= Strings.fromByteArray(bs);
////        System.out.println(tx);
////        BoardcastTx tx=BoardcastTx.fromJson(new String(bs));
////        System.out.println(tx.toJson());
//
////        System.out.println("lastblcok："+b);
//
////        JSONObject res=getByTxHash("16713BC94E6E0ECD4FF21CCFE5D1F1624DD3130EA26B9ABD18E340A1FF3ECE42");
////        JSONArray  res=txSeach(1,5222950);
//        byte[] txBs= Base64.decode(txs);

        System.out.println(Sha256.tx2Sha256(txs).toUpperCase());
//        System.out.println(Hex.encodeHex(Sha256Hash.hash(txBs)));
    }

    public static JSONObject getBalance(String address){
        url=url+"/cosmos/bank/v1beta1/balances/"+address;
        String res=HttpUtils.httpGet(url);
        return JSON.parseObject(res);
    }
}