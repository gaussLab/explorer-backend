package com.wuyuan.blockbrowse.controller;

import com.alibaba.fastjson.JSONObject;
import com.wuyuan.blockbrowse.utils.DeviceUtil;
import com.wuyuan.database.sevice.ConfigService;
import com.wuyuan.database.util.ConfigUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Api(tags = "获取链名")
@RestController
@RequestMapping("chain")
public class ChainApiController {

    @Resource
    private ConfigService configService;

    private String chainName;


    @ApiOperation(value = "获取链名", nickname = "查询链")
    @GetMapping("getChainName")
    public JSONObject getChainName(@RequestHeader("User-Agent") String userAgent){
        JSONObject chainUrl = new JSONObject();
        String wapGauss = null;
        String wapUsdg = null;
        String wapFec = null;
        String wapIgpc =  null;
        String wapGpb = null;
        if (DeviceUtil.checkAgentIsMobile(userAgent)){
            chainUrl.put("code",200);
            chainUrl.put("chainName",getChain());
            wapGauss = configService.getConfig(ConfigUtil.wapGaussUrlKey);
            wapUsdg = configService.getConfig(ConfigUtil.wapUsdgUrlKey);
            wapFec = configService.getConfig(ConfigUtil.wapFecUrlKey);
            wapIgpc = configService.getConfig(ConfigUtil.wapIgpcUrlKey);
            wapGpb = configService.getConfig(ConfigUtil.wapGpbUrlKey);
            if (StringUtils.isBlank(wapGauss)){
                wapGauss = configService.getConfig(ConfigUtil.httpGaussUrlKey);
            }
            if (StringUtils.isBlank(wapUsdg)){
                wapUsdg = configService.getConfig(ConfigUtil.httpUsdgUrlKey);
            }
            if (StringUtils.isBlank(wapFec)){
                wapFec = configService.getConfig(ConfigUtil.httpFecUrlKey);
            }
            if (StringUtils.isBlank(wapIgpc)){
                wapIgpc = configService.getConfig(ConfigUtil.httpIgpcUrlKey);
            }
            if (StringUtils.isBlank(wapGpb)){
                wapGpb = configService.getConfig(ConfigUtil.httpGpbUrlKey);
            }
            chainUrl.put("gaussUrl",wapGauss);
            chainUrl.put("usdgUrl",wapUsdg);
            chainUrl.put("fecUrl",wapFec);
            chainUrl.put("igpcUrl",wapIgpc);
            chainUrl.put("gpbUrl",wapGpb);
        }else {
            chainUrl.put("code",200);
            chainUrl.put("chainName",getChain());
            chainUrl.put("gaussUrl",configService.getConfig(ConfigUtil.httpGaussUrlKey));
            chainUrl.put("usdgUrl",configService.getConfig(ConfigUtil.httpUsdgUrlKey));
            chainUrl.put("fecUrl",configService.getConfig(ConfigUtil.httpFecUrlKey));
            chainUrl.put("igpcUrl",configService.getConfig(ConfigUtil.httpIgpcUrlKey));
            chainUrl.put("gpbUrl",configService.getConfig(ConfigUtil.httpGpbUrlKey));
        }
        return chainUrl;
    }

    public String getChain(){
        if (chainName == null){
            chainName = configService.getConfig(ConfigUtil.chainNameKey);
        }
        return chainName;
    }

}
