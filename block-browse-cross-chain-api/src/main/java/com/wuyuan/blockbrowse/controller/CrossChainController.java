package com.wuyuan.blockbrowse.controller;

import com.alibaba.fastjson.JSONObject;
import com.wuyuan.database.sevice.ConfigService;
import com.wuyuan.database.sevice.CrossChainService;
import com.wuyuan.database.sevice.TransactionService;
import com.wuyuan.database.sevice.TxsQueueService;
import com.wuyuan.database.util.ApiResult;
import com.wuyuan.database.util.ConfigUtil;
import com.wuyuan.database.util.DeviceUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Api(tags = "跨链交易相关")
@RestController
@RequestMapping("transfer")
public class CrossChainController {


    @Resource
    private ConfigService configService;

    @Resource
    private CrossChainService crossChainService;

    @ApiOperation(value = "首页和搜索地址", nickname = "查询交易")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pageIndex", value = "第几页", required = true, dataType = "int"),
            @ApiImplicitParam(name = "pageSize", value = "每页几条", required = true, dataType = "int"),
            @ApiImplicitParam(name = "chainName", value = "链名", required = false, dataType = "String"),
            @ApiImplicitParam(name = "address", value = "地址", required = false, dataType = "String"),
            @ApiImplicitParam(name = "id", value = "id", required = false, dataType = "long"),
            @ApiImplicitParam(name = "status", value = "交易状态(Success,Failed)", required = false, dataType = "String"),
    })
    @GetMapping("getCrossList")
    public ApiResult getCrossList(Integer pageIndex,Integer pageSize,String chainName,String address,Long id,String status) {
        if (pageIndex == null || pageIndex <= 0){
            pageIndex = 1;
        }
        if (pageSize == null || pageSize <= 0){
            pageSize = 20;
        }
        return new ApiResult(200,"查询成功",crossChainService.getCrossList(pageIndex,pageSize,chainName,address,id,status));
    }

    @ApiOperation(value = "搜索hash）", nickname = "查询交易")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "hash", value = "交易hash", required = true, dataType = "String"),
    })
    @GetMapping("getCrossTxByHash")
    public ApiResult getCrossTxByHash(String hash) {
        if (StringUtils.isBlank(hash)){
            return new ApiResult(-200,"hash不能为空");
        }
        return new ApiResult(200,"查询成功",crossChainService.getCrossTxByHash(hash.trim()));
    }

    @ApiOperation(value = "点击序列号", nickname = "查询交易")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "packetSequence", value = "序列号", required = true, dataType = "String"),
            @ApiImplicitParam(name = "fromhash", value = "fromHash", required = true, dataType = "String"),
            @ApiImplicitParam(name = "chainName", value = "链名(gauss,igpc,usdg,fec,gpb)", required = true, dataType = "String")
    })
    @GetMapping("getCrossTxBySequence")
    public ApiResult getCrossTxBySequence(String packetSequence,String fromhash,String chainName) {
        if (StringUtils.isBlank(packetSequence)){
            return new ApiResult(-200,"必要字段不能为空");
        }
        return new ApiResult(200,"查询成功",crossChainService.getCrossTxBySequence(packetSequence.trim(),fromhash,chainName.trim()));
    }

    @ApiOperation(value = "搜索序列号", nickname = "查询交易")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "packetSequence", value = "序列号", required = true, dataType = "String"),
            @ApiImplicitParam(name = "chainName", value = "链名(gauss,igpc,usdg,fec)", required = true, dataType = "String"),
            @ApiImplicitParam(name = "pageIndex", value = "第几页", required = true, dataType = "int"),
            @ApiImplicitParam(name = "pageSize", value = "每页几条", required = true, dataType = "int"),
    })
    @GetMapping("getCrossListBySequence")
    public ApiResult getCrossListBySequence(String packetSequence,String chainName,Integer pageIndex,Integer pageSize) {
        if (StringUtils.isBlank(packetSequence) || StringUtils.isBlank(chainName)){
            return new ApiResult(-200,"必要字段不能为空");
        }
        if (pageSize == null || pageSize <= 0){
            pageSize = 20;
        }

        if (pageIndex == null || pageIndex <= 0){
            pageIndex = 1;
        }
        return new ApiResult(200,"查询成功",crossChainService.getCrossListBySequence(packetSequence.trim(),chainName.trim(),pageIndex,pageSize));
    }

    @ApiOperation(value = "获取各个链浏览器", nickname = "查询链")
    @GetMapping("getChainName")
    public JSONObject getChainName(@RequestHeader("User-Agent") String userAgent){
        JSONObject chainUrl = new JSONObject();
        String gaussUrl = null;
        String gpbUrl = null;
        String usdgUrl = null;
        String fecUrl = null;
        String igpcUrl = null;
        if (DeviceUtil.checkAgentIsMobile(userAgent)){
            gaussUrl = configService.getConfig(ConfigUtil.gaussWapUrl);
            gpbUrl = configService.getConfig(ConfigUtil.gpbWapUrl);
            usdgUrl = configService.getConfig(ConfigUtil.usdgWapUrl);
            fecUrl = configService.getConfig(ConfigUtil.fecWapUrl);
            igpcUrl = configService.getConfig(ConfigUtil.igpcWapUrl);
        }else {
            gaussUrl = configService.getConfig(ConfigUtil.gaussBrowseUrl);
            gpbUrl = configService.getConfig(ConfigUtil.gpbBrowseUrl);
            usdgUrl = configService.getConfig(ConfigUtil.usdgBrowseUrl);
            fecUrl =  configService.getConfig(ConfigUtil.fecBrowseUrl);
            igpcUrl = configService.getConfig(ConfigUtil.igpcBrowseUrl);
        }
        chainUrl.put("code",200);
        chainUrl.put("gaussUrl",gaussUrl);
        chainUrl.put("usdgUrl",usdgUrl);
        chainUrl.put("fecUrl",fecUrl);
        chainUrl.put("igpcUrl",igpcUrl);
        chainUrl.put("gpbUrl",gpbUrl);
        return chainUrl;
    }
}
