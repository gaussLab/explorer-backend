package io.cosmos.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.google.gson.JsonArray;
import io.cosmos.common.HttpUtils;
import io.cosmos.crypto.encode.Bech32;
import io.cosmos.msg.MsgBase;
import io.cosmos.msg.MsgSend;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

public class CosmosUtil {



    public static long getNewBlock(String url){
        String uri=url+"/blocks/latest";
        String res= HttpUtils.httpGetDisable(uri);
        JSONObject json= JSON.parseObject(res);
        JSONObject blockJson=json.getJSONObject("block");
        if(blockJson!=null){
            return blockJson.getJSONObject("header").getLong("height");
        }
        return 0;
    }
    public static JSONObject getBlockByHeight(String url,long blockHeight){
       String uri=url+"/blocks/"+blockHeight;
        String res=HttpUtils.httpGet(uri);
        return JSON.parseObject(res);
    }
    public static JSONObject getByTxHash(String url,String hash){
       String uri=url+"/txs/"+hash;
        String res=HttpUtils.httpGetDisable(uri);
        return JSON.parseObject(res);
    }
    public static JSONArray txSeach(String url,long  minHeight, long maxheight){

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

    public static JSONArray txSeach(String url,List<String> txs){
        JSONArray jsonArray=new JSONArray();
        for (String tx: txs ) {
            String txhash=Sha256.tx2Sha256(tx).toUpperCase();
            jsonArray.add(getTxByhash(url,txhash));
        }
        return jsonArray;
    }

    public  static JSONObject getTxByhash(String url,String txHash){
        String uri=url+"/tx/v1beta1/txs/"+txHash;
        String res=HttpUtils.httpGet(uri);
        JSONObject json=JSONObject.parseObject(res, Feature.DisableSpecialKeyDetect);
        return json;
    }

    public static JSONObject getBalance(String url,String address,String coinName){
        String uri=url+"/bank/v1beta1/balances/"+address+"/"+coinName;
        String res=HttpUtils.httpGet(uri);
        if (res == null){
            return null;
        }
        return JSON.parseObject(res);
    }

    public static JSONObject getAllBalance(String url,String address){
        String uri=url+"/bank/v1beta1/balances/"+address +"?pagination.limit=200&pagination.count_total=true";
        String res=HttpUtils.httpGet(uri);
        if (res == null){
            return null;
        }
        return JSON.parseObject(res);
    }

    public static String  broadcast(String url,String tx) throws IOException {
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

    //获取社区池（Community Pool）
    public static String getCommunity(String url) {
        String uri = url+"/distribution/v1beta1/community_pool";
        String res = HttpUtils.httpGet(uri);
        if (StringUtils.isNotBlank(res)){
            String community = String.valueOf(JSON.parseObject(res).getJSONArray("pool").getJSONObject(0).get("amount"));
            community = new BigDecimal(community).divide(new BigDecimal(Math.pow(10,6))).toPlainString();
            return community;
        }
        return "0";
    }

    public static String validatorsets(String url){
        String validatorsUnBonding = url + "/cosmos/staking/v1beta1/validators?status=BOND_STATUS_UNBONDING&pagination.limit=200&pagination.count_total=true";//不积极的
        String validatorsUnBonded = url + "/cosmos/staking/v1beta1/validators?status=BOND_STATUS_UNBONDED&pagination.limit=200&pagination.count_total=true";//不积极的
        String validatorsBonded = url + "/cosmos/staking/v1beta1/validators?status=BOND_STATUS_BONDED&pagination.limit=200&pagination.count_total=true";//积极的
        String uri = url + "/cosmos/staking/v1beta1/validators/{operatorAddress}/delegations?pagination.limit=10&pagination.count_total=true";//获取指定验证人地址的所有委托信息
        //TODO 无法获取验证人的自我委托地址
        return null;
    }

    //获取通胀率
    public static String getInflation(String url){
        String uri = url + "/mint/v1beta1/inflation";
        String result = HttpUtils.httpGet(uri);
        if (result != null){
            return JSON.parseObject(result).getString("inflation");
        }
        return null;
    }

    public static String getPool(String url){
        String uri = url + "/staking/v1beta1/pool";
        String result = HttpUtils.httpGet(uri);
        if (StringUtils.isNotBlank(result) && result.indexOf("pool") != -1){
            return result;
        }
        return null;
    }

    public static String getTotal(String url,String coinName){
        String uri = url + "/bank/v1beta1/supply/"+coinName;
        String result = HttpUtils.httpGet(uri);
        if (StringUtils.isNotBlank(result)){
            String total = JSON.parseObject(result).getJSONObject("amount").getString("amount");//获取Ato的总发行数
            return new BigDecimal(total).divide(new BigDecimal(Math.pow(10,6))).toPlainString();
        }else {
            return "0";
        }
    }

    public static String getTransferTokenByHash(String url,String tokenHash){
        String uri = url + "/ibc/applications/transfer/v1beta1/denom_traces/"+tokenHash;
        String result = HttpUtils.httpGet(uri);
        return result;
    }

    public static String getAllvalidatorsets(String url){
        String uri = url + "/staking/v1beta1/validators?pagination.limit=350&pagination.count_total=true";
        String result = HttpUtils.httpGet(uri);
        return result;
    }

    public static String getValidatorsetsIcon(String identity){
        String icon = null;
        if (identity.length() == 16){
            String url = "https://keybase.io/_/api/1.0/user/lookup.json?key_suffix="+identity+"&fields=pictures";
            String result = HttpUtils.httpGet(url);
            if (null != result && JSON.parseObject(result).getJSONObject("status").getInteger("code") == 0 && JSON.parseObject(result).getJSONArray("them") != null){
                try {
                    icon = JSON.parseObject(result).getJSONArray("them").getJSONObject(0).getJSONObject("pictures").getJSONObject("primary").getString("url");
                }catch (Exception e){
                    icon = null;
                }
            }
        }else if (identity.indexOf("keybase.io/team/") > 0){
            String result = HttpUtils.httpGet(identity);
            if (null != result && JSON.parseObject(result).getIntValue("statusCode") == 200){
                icon = JSON.parseObject(result).getString("content");
                System.out.println(identity);
                System.out.println(icon+"-----------------identity另一种获取方式");
            }
        }
        return icon;
    }

    public static String getAllDelegations(String url,String validatorsAddress){
        String uri = url + "/staking/v1beta1/validators/"+validatorsAddress+"/delegations?pagination.limit=1&pagination.count_total=true";
        String res = HttpUtils.httpGet(uri);
        return res;
    }

    public static JSONObject getTotalDelegations(String url,String validatorsAddress,Integer pageSize){
        String uri = url + "/staking/v1beta1/validators/"+validatorsAddress+"/delegations?pagination.limit=" + pageSize + "&pagination.count_total=true";
        String res = HttpUtils.httpGet(uri);
        return JSON.parseObject(res);
    }

    public static JSONObject getTotalUnDelegations(String url,String validatorsAddress,Integer pageSize){
        String uri = url + "/staking/v1beta1/validators/"+validatorsAddress+"/unbonding_delegations?pagination.limit=" + pageSize + "&pagination.count_total=true";
        String res = HttpUtils.httpGet(uri);
        return JSON.parseObject(res);
    }


    public static String getDelegatorByOperatorAddr(String prefix,String operatorAddr){
        Bech32.Bech32Data address = Bech32.decode(operatorAddr);
        return Bech32.encode(prefix,address.getData());
    }

    public static String getDelegatorInfo(String url,String operatorAddr,String delegatorAddr ){
        String uri = url + "/staking/v1beta1/validators/" + operatorAddr + "/delegations/"+delegatorAddr;
        return HttpUtils.httpGet(uri);
    }

    public static String getSignedBlocksWindow(String url){
        String uri = url + "/slashing/v1beta1/params";
        return HttpUtils.httpGet(uri);
    }

    public static String getSigningInfos(String url,String cnsAddress){
        String uri = url + "/slashing/v1beta1/signing_infos/"+cnsAddress;
        return HttpUtils.httpGet(uri);
    }

    public static JSONObject getUnBondingBalance(String url,String address){
        String uri = url + "/staking/v1beta1/delegators/" + address +"/unbonding_delegations";
        return JSON.parseObject(HttpUtils.httpGet(uri));
    }

    public static JSONObject getAllProposals(String url,Integer pageSize){
        String uri = url + "/gov/v1beta1/proposals?pagination.limit=" + pageSize + "&pagination.count_total=true";
        return JSON.parseObject(HttpUtils.httpGet(uri),Feature.DisableSpecialKeyDetect);
    }

    public static String getProposalsTally(String url,Integer proposalsId){
        String uri = url + "/gov/v1beta1/proposals/" + proposalsId + "/tally";
        return HttpUtils.httpGet(uri);
    }

    public static String getTokens(String url,Integer pageSize){
        String uri = url + "/token/tokens?pagination.limit=" + pageSize + "&pagination.count_total=true";
        return HttpUtils.httpGet(uri);
    }

    public static JSONObject getIbcClient(String url,String channelId,String portId){
        String uri = url + "/ibc/core/channel/v1beta1/channels/" + channelId + "/ports/" + portId + "/client_state";
        return JSON.parseObject(HttpUtils.httpGet(uri),Feature.DisableSpecialKeyDetect);
    }
}
