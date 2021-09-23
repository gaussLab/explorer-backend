package com.wuyuan.blockbrowse.controller;


import com.alibaba.fastjson.JSONObject;
import com.wuyuan.blockbrowse.utils.ApiResult;
import com.wuyuan.database.sevice.ValidatorDelegatorService;
import com.wuyuan.database.sevice.ValidatorService;
import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Api(tags = "验证人相关")
@RestController
@RequestMapping("validator")
public class ValidatorApiController {

    @Resource
    private ValidatorService validatorService;
    @Resource
    private ValidatorDelegatorService validatorDelegatorService;

    @ApiOperation(value = "通过验证人操作地址获取验证人详情", nickname = "查询验证人")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "operatorAddress", value = "验证人操作地址", required = true, dataType = "String"),
    })
    @GetMapping("getValidatorsByMoniker")
    public ApiResult getValidatorsByMoniker(String operatorAddress) {
        if (StringUtils.isBlank(operatorAddress)){
            return new ApiResult(-200,"操作地址不能为空");
        }
        JSONObject validator = validatorService.getValidatorsByOperator(operatorAddress.trim());
        if (validator == null){
            return new ApiResult(-200,"暂无此人");
        }
        return new ApiResult(200,"查询成功",validator);
    }

    @ApiOperation(value = "根据状态和排序方式获取验证人列表", nickname = "查询验证人")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "status", value = "验证人状态（BOND_STATUS_BONDED -积极的, BOND_STATUS_UNBONDED -候选的, JAILED -入狱的）", required = true, dataType = "String"),
            @ApiImplicitParam(name = "pramsName", value = "参数名-排序用(验证人名-description.moniker,佣金-commission.commission_rates.rate,委托金额 - tokens,投票权 - votingPower,上线时间比重(uptime) ,自我委托-selfDelegateAmount,委托人数-totalDelegations)", required = true, dataType = "String"),
            @ApiImplicitParam(name = "sortMethod", value = "排序方式（1从大到小 0 从小到大）", required = true, dataType = "int"),
            @ApiImplicitParam(name = "pageIndex", value = "页数", required = true, dataType = "int"),
            @ApiImplicitParam(name = "pageSize", value = "每页几条数据", required = true, dataType = "int"),
    })
    @GetMapping("getValidatorsByStatus")
    public ApiResult getValidatorsByStatus(String status, String pramsName, Integer sortMethod, Integer pageIndex, Integer pageSize) {
        if (StringUtils.isBlank(status) || StringUtils.isBlank(pramsName)){
            return new ApiResult(-200,"状态或参数名不能为空");
        }
        if (pageSize == null || pageSize == 0){
            pageSize = 20;
        }
        if (pageIndex == null || pageIndex == 0){
            pageIndex = 1;
        }
        return new ApiResult(200,"查询成功",validatorService.getValidatorsByStatus(status.trim(),pramsName.trim(),sortMethod,pageIndex,pageSize));
    }


    @ApiOperation(value = "通过验证人操作地址获取其他验证人的tokens和佣金比例", nickname = "查询验证人")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "operatorAddress", value = "验证人操作地址", required = true, dataType = "String"),
    })
    @GetMapping("getOtherValidatorCommissionByMoniker")
    public ApiResult getOtherValidatorCommissionByMoniker(String operatorAddress) {
        if (StringUtils.isBlank(operatorAddress)){
            return new ApiResult(-200,"操作地址不能为空");
        }
        return new ApiResult(200,"查询成功",validatorService.getOtherValidatorCommissionByOperator(operatorAddress.trim()));
    }

    @ApiOperation(value = "获取验证人名下的委托信息", nickname = "查询验证人")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "operatorAddress", value = "验证人操作地址", required = true, dataType = "String"),
            @ApiImplicitParam(name = "pageIndex", value = "页数", required = true, dataType = "int"),
            @ApiImplicitParam(name = "pageSize", value = "每页几条数据", required = true, dataType = "int"),
    })
    @GetMapping("getDelegatorByOperatorAddress")
    public ApiResult getDelegatorByOperatorAddress(String operatorAddress,Integer pageIndex, Integer pageSize) {
        if (StringUtils.isBlank(operatorAddress)){
            return new ApiResult(-200,"操作地址不能为空");
        }
        if (pageSize == null || pageSize == 0){
            pageSize = 20;
        }
        if (pageIndex == null || pageIndex == 0){
            pageIndex = 1;
        }
        return new ApiResult(200,"查询成功",validatorDelegatorService.getDelegatorByOperatorAddress(operatorAddress.trim(),pageIndex,pageSize));
    }

    @ApiOperation(value = "获取验证人被解除委托的信息", nickname = "查询验证人")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "operatorAddress", value = "验证人操作地址", required = true, dataType = "String"),
            @ApiImplicitParam(name = "pageIndex", value = "页数", required = true, dataType = "int"),
            @ApiImplicitParam(name = "pageSize", value = "每页几条数据", required = true, dataType = "int"),
    })
    @GetMapping("getUnDelegatorByOperatorAddress")
    public ApiResult getUnDelegatorByOperatorAddress(String operatorAddress,Integer pageIndex, Integer pageSize) {
        if (StringUtils.isBlank(operatorAddress)){
            return new ApiResult(-200,"操作地址不能为空");
        }
        if (pageSize == null || pageSize == 0){
            pageSize = 20;
        }
        if (pageIndex == null || pageIndex == 0){
            pageIndex = 1;
        }
        return new ApiResult(200,"查询成功",validatorDelegatorService.getUnDelegatorByOperatorAddress(operatorAddress.trim(),pageIndex,pageSize));
    }
}
