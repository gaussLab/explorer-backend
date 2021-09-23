package io.cosmos.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import io.cosmos.common.HttpUtils;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;
@Slf4j
public class BalanceUtil {


    public static BigDecimal getBalance(String address, String url, String coin, BigDecimal decimal)  {
        String uri = url + "/bank/v1beta1/balances/" + address + "/u" + coin;
        BigDecimal balance = BigDecimal.ZERO;
        try {
            String res = HttpUtils.httpGet(uri);
            balance = JSON.parseObject(res).getJSONObject("balance").getBigDecimal("amount");

        } catch (Exception e) {
            e.printStackTrace();

        }
        return balance.divide(decimal, 6, BigDecimal.ROUND_HALF_UP);
    }

    public static BigDecimal getDelegatorBalance(String address, String url)  {
        BigDecimal balance = BigDecimal.ZERO;
        try {
            List<String> list = getValidators(address, url).toJavaList(String.class);
            balance = getPledgedBalance(address, list, url).divide(new BigDecimal("1000000"), 6, BigDecimal.ROUND_HALF_UP);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return balance;
    }

    public static BigDecimal getRewardBalance(String address, String url) {
        try {
            String uri = url + "/distribution/v1beta1/delegators/"+address+"/rewards";
            String res = HttpUtils.httpGet(uri);
            JSONObject json=JSONObject.parseObject(res);
            JSONArray rewards=json.getJSONArray("total");

            if(rewards!=null&&rewards.size()>0){
                for (int i=0;i<rewards.size();i++) {
                    JSONObject amount=rewards.getJSONObject(i);
                    return amount.getBigDecimal("amount").divide(new BigDecimal(1000000),6,BigDecimal.ROUND_HALF_UP);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return  BigDecimal.ZERO;
    }

    public static BigDecimal getValidatorRewardBalance(String address, String url,String coinName) {
        try {
            String uri = url + "/distribution/v1beta1/validators/"+ address +"/outstanding_rewards";
            String res = HttpUtils.httpGet(uri);
            JSONObject json = JSONObject.parseObject(res);
            JSONArray rewards = json.getJSONObject("rewards").getJSONArray("rewards");
            if(rewards != null && rewards.size() > 0){
                BigDecimal reward = BigDecimal.ZERO;
                for (int i=0;i<rewards.size();i++) {
                    JSONObject amount = rewards.getJSONObject(i);
                    if (amount.getString("denom").equals(coinName)){
                        reward = reward.add(amount.getBigDecimal("amount"));
                    }
                }
                return reward.divide(new BigDecimal(1000000),6,BigDecimal.ROUND_HALF_UP);
            }

        } catch (Exception e) {
           log.error(e.getMessage());
        }
        return  BigDecimal.ZERO;
    }


    ///cosmos/distribution/v1beta1/delegators/gauss1jeplld036c86ppzjaq2z5yet2an88ft0hjp6w2/validators
    public static JSONArray getValidators(String address, String url) {
        String uri = url + "/distribution/v1beta1/delegators/" + address + "/validators";
        String res = HttpUtils.httpGet(uri);
        return JSON.parseObject(res).getJSONArray("validators");
    }

    public static BigDecimal getPledgedBalance(String address, List<String> list, String url) {
        BigDecimal pledged = BigDecimal.ZERO;
        for (String validator : list) {
            BigDecimal p = delegationByValidators(address, validator, url);
            pledged = pledged.add(p);
        }
        return pledged;
    }

    /**
     * 查询验证人委托奖励
     * @param address
     * @param url
     * @return
     */
    public static BigDecimal getValidateatorReward(String address,String url, String coin) {
        String uri = url + "/distribution/v1beta1/validators/"+address+"/commission";
        String res = HttpUtils.httpGet(uri);
        JSONObject json=JSONObject.parseObject(res);
        if (json != null){
            JSONArray rewards=json.getJSONObject("commission").getJSONArray("commission");
            if (!"udga".equals(coin)){
                coin="u"+coin;
            }
            if(rewards!=null&&rewards.size()>0){
                for (int i=0;i<rewards.size();i++) {
                    JSONObject amount=rewards.getJSONObject(i);
                    if(coin.equalsIgnoreCase(amount.getString("denom"))){
                        return amount.getBigDecimal("amount").divide(new BigDecimal("1000000"),6,BigDecimal.ROUND_HALF_UP);
                    }
                }
            }
        }
        return  BigDecimal.ZERO;
    }

    public static BigDecimal delegationByValidators(String delegation, String validator, String url) {
        String uri = url + "/staking/v1beta1/validators/" + validator + "/delegations/" + delegation;
        String res = HttpUtils.httpGet(uri);
        JSONObject balance=JSON.parseObject(res).getJSONObject("delegation_response").getJSONObject("balance");
        return balance.getBigDecimal("amount");
    }

    public static JSONArray getDelegatorTxByAddress(String url,String address){
        String uri = url +"/staking/v1beta1/delegations/"+address;
//        String uri = url + "/staking/delegators/"+address+"/delegations";
        String res = HttpUtils.httpGet(uri);
        if (res == null){
            return null;
        }
        return JSON.parseObject(res).getJSONArray("delegation_responses");
    }

    public static JSONArray getUnDelegators(String url,String address){
//        String uri = url + "/staking/delegators/"+address+"/unbonding_delegations";
        String uri = url + "/staking/v1beta1/delegators/"+address+"/unbonding_delegations";
        String res = HttpUtils.httpGet(uri);
        if (res == null){
            return null;
        }
        return JSON.parseObject(res).getJSONArray("unbonding_responses");
    }
}
