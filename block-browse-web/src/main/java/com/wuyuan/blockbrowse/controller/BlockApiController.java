package com.wuyuan.blockbrowse.controller;

import com.alibaba.fastjson.JSONObject;
import com.wuyuan.blockbrowse.utils.ApiResult;
import com.wuyuan.database.sevice.BlockService;
import com.wuyuan.database.sevice.TransactionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Api(tags = "区块相关")
@RestController
@RequestMapping("block")
public class BlockApiController {
    @Resource
    private BlockService blockService;

    @ApiOperation(value = "获取区块详情", nickname = "查询区块")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "blockNumber", value = "区块高度", required = true, dataType = "String"),
    })
    @GetMapping("getBlockByBlockNumber")
    public ApiResult getBlockByBlockNumber(String blockNumber) {
        JSONObject block = blockService.getBlockByBlockNumber(blockNumber);
        return new ApiResult(200,"查询成功",block);
    }

    @ApiOperation(value = "获取区块列表", nickname = "查询区块")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pageIndex", value = "页数", required = false, dataType = "int"),
            @ApiImplicitParam(name = "pageSize", value = "每页几条数据", required = false, dataType = "int")
    })
    @GetMapping("getBlockList")
    public ApiResult getBlockList(Integer pageIndex,Integer pageSize) {
        return new ApiResult(200,"查询成功",blockService.getBlockList(pageIndex,pageSize));
    }

    @ApiOperation(value = "查询当前最高高度", nickname = "查询区块")
    @GetMapping("getMaxBlockNumber")
    public ApiResult getMaxBlockNumber() {
        return new ApiResult(200,"查询成功",blockService.getMaxBlockNumber());
    }

    @ApiOperation(value = "获取平均区块时间", nickname = "查询区块")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "blockMaxNum", value = "最新区块高度", required = true, dataType = "String"),
    })
    @GetMapping("averageBlockTime")
    public ApiResult averageBlockTime(String blockMaxNum) {
        return new ApiResult(200,"查询成功",blockService.averageBlockTime(blockMaxNum));
    }
}
