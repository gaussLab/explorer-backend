package com.wuyuan.database.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.wuyuan.database.sevice.ConfigService;
import io.cosmos.types.Pair;

import org.apache.http.NameValuePair;

import org.apache.http.client.ClientProtocolException;

import org.apache.http.client.config.RequestConfig;

import org.apache.http.client.entity.UrlEncodedFormEntity;

import org.apache.http.client.methods.CloseableHttpResponse;

import org.apache.http.client.methods.HttpGet;

import org.apache.http.client.methods.HttpPost;

import org.apache.http.entity.ContentType;

import org.apache.http.entity.StringEntity;

import org.apache.http.impl.client.CloseableHttpClient;

import org.apache.http.impl.client.HttpClients;

import org.apache.http.message.BasicNameValuePair;

import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;


import javax.annotation.Resource;
import java.io.*;

import java.net.HttpURLConnection;

import java.net.SocketException;

import java.net.URL;

import java.util.ArrayList;

import java.util.List;

import java.util.Map;



public class HttpUtils {

    static HttpPost m_httpPost = null;

    static HttpGet  m_httpGet = null;

    static CloseableHttpClient m_httpClient = HttpClients.createDefault();



    public static String httpGet(String url, ArrayList<Pair> pairs) {

        String params = "";

        if (pairs != null) {

            for (int i = 0; i < pairs.size(); i++) {

                String v = pairs.get(i).getValue();

                if (v == null || v.equals("")) {

                    pairs.remove(i);

                    i--;

                }

            }

            if (!pairs.isEmpty()) {

                params = pairs.get(0).getKey() + "=" + pairs.get(0).getValue();

                for (int i = 1; i < pairs.size(); i++) {

                    params = params + "&" + pairs.get(i).getKey() + "=" + pairs.get(i).getValue();

                }

            }



        }

        if (params.equals("")) return httpGet(url);

        return httpGet(url + "?" + params);

    }



    public static String httpGetDisable(String url) {

        //System.out.println("get:" + url);

        try {

            String res = sendGetData(url, "");

            return res;

        } catch (ClientProtocolException e) {

            return e.getMessage();

        } catch (IOException e) {

            return e.getMessage();

        }



    }



    public static String httpGet(String httpUrl){

        BufferedReader reader = null;

        String result = null;

        StringBuffer sbf = new StringBuffer();





        try {

            URL url = new URL(httpUrl);

            HttpURLConnection connection = null;

            connection = (HttpURLConnection) url.openConnection();// 正常访问



            connection.setConnectTimeout(300*1000);

            connection.setReadTimeout(300*1000);

            connection.setRequestMethod("GET");



            connection.connect();

            InputStream is = connection.getInputStream();

            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

            String strRead = null;

            while ((strRead = reader.readLine()) != null) {

                sbf.append(strRead);

                sbf.append("\r\n");

            }

            reader.close();

            result = sbf.toString();

        } catch (SocketException e) {

            System.out.println("Connection timed out: connect");

        } catch (Exception e) {

            e.printStackTrace();

        }

        return result;

    }



    public static String httpPost(String url, String data) {

        //System.out.println("post: " + url);

        //System.out.println("data: " + data);

        try {

            String res = sendPostDataByJson(url, data, "");

            return res;

        } catch (ClientProtocolException e) {

            e.printStackTrace();

            return e.getMessage();

        } catch (IOException e) {

            e.printStackTrace();

            return e.getMessage();

        }



    }



    public static String sendPostDataByMap(String url, Map<String, String> map, String encoding) throws ClientProtocolException, IOException {

        String result = "";



        CloseableHttpClient httpClient = HttpClients.createDefault();

        HttpPost httpPost = new HttpPost(url);



        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

        if (map != null) {

            for (Map.Entry<String, String> entry : map.entrySet()) {

                nameValuePairs.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));

            }

        }



        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, encoding));



        httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");

        httpPost.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");



        CloseableHttpResponse response = httpClient.execute(httpPost);



    //        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {

    //            result = EntityUtils.toString(response.getEntity(), "utf-8");

    //        }

        result = EntityUtils.toString(response.getEntity(), "utf-8");

        response.close();



        return result;

    }


    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        list.add("GPB");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("tokenList",list);
        JSONObject data = null;
        //https://walletapi.gaussdex.io/gauss-dex-wallet-api/api/token/queryAmount
        //https://walletapi.gaussdex.io/gauss-dex-wallet-transaction/outside/txCouple/queryLastDealAmount
        System.out.println(JSON.toJSONString(jsonObject));
        try {
            String res = sendPostDataByJson("https://walletapi.gaussdex.io/gauss-dex-wallet-api/api/token/queryAmount", JSON.toJSONString(jsonObject),"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiLlkI7lj7DnrqHnkIYiLCJjbGllbnQiOiJiYWNrZW5kLW1hbmFnZXIiLCJleHAiOjE2NTIzMzU1MDEsImlhdCI6MTYyMDc5OTUwMX0.g7DDEoOVYFYIqIWXXsJwu3hggQ75zS6cEXcaL0QZZCk");
            System.out.println(res);
            data = JSON.parseObject(res);
        }catch (Exception e){

        }
        System.out.println(data);
    }

    public static String sendPostDataByJson(String url, String json, String head) throws ClientProtocolException, IOException {

        String result = "";

        CloseableHttpClient httpClient = HttpClients.createDefault();

        m_httpPost = new HttpPost(url);

        HttpPost httpPost = m_httpPost;

        StringEntity stringEntity = new StringEntity(json, ContentType.APPLICATION_JSON);

        stringEntity.setContentEncoding("utf-8");

        httpPost.setEntity(stringEntity);
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("accessToken",head);

        CloseableHttpResponse response = httpClient.execute(httpPost);

        result = EntityUtils.toString(response.getEntity(), "utf-8");

        response.close();
        return result;

    }





    public static String sendGetData(String url, String encoding) throws ClientProtocolException, IOException {

        String result = "";



        CloseableHttpClient httpClient = m_httpClient;

        RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(300*1000)

                .setSocketTimeout(300*1000).setConnectTimeout(300*1000).build();

    //        if(m_httpGet == null){

        m_httpGet = new HttpGet(url);

    //        }

        HttpGet httpGet = m_httpGet;

        httpGet.setConfig(requestConfig);

        httpGet.addHeader("Content-type", "application/json");
        httpGet.addHeader("projectId", "ab771e9ce0f94b06925f47e3d3ffa51d");

        CloseableHttpResponse response = httpClient.execute(httpGet);



    //        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {

    //            result = EntityUtils.toString(response.getEntity(), "utf-8");

    //        }

        result = EntityUtils.toString(response.getEntity(), "utf-8");

        //    response.close();



        return result;

    }

    /**

     * 发送HttpPost请求

     *

     * @param strURL

     *            服务地址

     * @param params

     *            json字符串,例如: "{ \"id\":\"12345\" }" ;其中属性名必须带双引号<br/>

     * @return 成功:返回json字符串<br/>

     */

    public static String post(String strURL, String params) {

        BufferedReader reader = null;

        try {

            URL url = new URL(strURL);// 创建连接

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setDoOutput(true);

            connection.setDoInput(true);

            connection.setUseCaches(false);

            connection.setInstanceFollowRedirects(true);

            connection.setRequestMethod("POST"); // 设置请求方式

            connection.setRequestProperty("Accept", "application/json"); // 设置接收数据的格式<br>、　　　　　　//因为要登陆才可以执行请求，所以这里要带cookie的header

            connection.setRequestProperty("Content-Type", "application/json"); // 设置发送数据的格式

            connection.connect();

            //一定要用BufferedReader 来接收响应， 使用字节来接收响应的方法是接收不到内容的

            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream(), "UTF-8"); // utf-8编码

            out.append(params);

            out.flush();

            out.close();

            // 读取响应

            reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));

            String line;

            String res = "";

            while ((line = reader.readLine()) != null) {

                res += line;

            }

            reader.close();

            return res;

        } catch (IOException e) {

            // TODO Auto-generated catch block

            e.printStackTrace();

        }

        return "error"; // 自定义错误信息

    }

}

