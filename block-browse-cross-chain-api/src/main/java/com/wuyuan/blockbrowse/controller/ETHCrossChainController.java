package com.wuyuan.blockbrowse.controller;


import com.wuyuan.database.sevice.ETHTransferTxService;
import com.wuyuan.database.util.ApiResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Api(tags = "生态间（ETH）跨链交易相关")
@RestController
@RequestMapping("eth")
public class ETHCrossChainController {

    @Resource
    private ETHTransferTxService ethTransferTxService;

    @ApiOperation(value = "首页+搜索地址+搜索hash", nickname = "查询交易")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pageIndex", value = "第几页", required = true, dataType = "int"),
            @ApiImplicitParam(name = "pageSize", value = "每页几条", required = true, dataType = "int"),
            @ApiImplicitParam(name = "address", value = "地址(搜索address传)", required = false, dataType = "String"),
            @ApiImplicitParam(name = "hash", value = "交易hash(搜索hash传)", required = false, dataType = "String"),
            @ApiImplicitParam(name = "status", value = "交易状态(Success,Failed,Pending)", required = false, dataType = "String"),
    })
    @GetMapping("getCrossList")
    public ApiResult getCrossList(Integer pageIndex,Integer pageSize,String address,String hash,String status) {
        if (pageIndex == null || pageIndex <= 0){
            pageIndex = 1;
        }
        if (pageSize == null || pageSize <= 0){
            pageSize = 20;
        }
        return new ApiResult(200,"查询成功",ethTransferTxService.getCrossList(pageIndex,pageSize,address,hash,status));
    }

    @ApiOperation(value = "交易详情", nickname = "查询交易")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "fromHash", value = "交易的fromHash", required = true, dataType = "String"),
            @ApiImplicitParam(name = "toHash", value = "交易的toHash", required = true, dataType = "String"),
    })
    @GetMapping("getTxByHash")
    public ApiResult getTxByHash(String fromHash,String toHash) {

        return new ApiResult(200,"查询成功",ethTransferTxService.getTxByHash(fromHash,toHash));
    }

//    @ApiOperation(value = "搜索hash）", nickname = "查询交易")
//    @ApiImplicitParams({
//            @ApiImplicitParam(name = "hash", value = "交易hash", required = true, dataType = "String"),
//    })
//    @RequestMapping("getCrossTxByHash")
//    public ApiResult getCrossTxByHash(String hash,Integer pageIndex,Integer pageSize) {
//        if (StringUtils.isBlank(hash)){
//            return new ApiResult(-200,"hash不能为空");
//        }
//        if (pageIndex == null || pageIndex <= 0){
//            pageIndex = 1;
//        }
//        if (pageSize == null || pageSize <= 0){
//            pageSize = 20;
//        }
//        return new ApiResult(200,"查询成功",ethTransferTxService.getCrossTxByHash(hash.trim(),pageIndex,pageSize));
//    }

}
