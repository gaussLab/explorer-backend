package com.wuyuan.blockbrowse.controller;

import com.alibaba.fastjson.JSONObject;
import com.wuyuan.blockbrowse.utils.ApiResult;
import com.wuyuan.database.sevice.AddressService;
import com.wuyuan.database.sevice.BlockService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Api(tags = "地址和币种发行量相关")
@RestController
@RequestMapping("address")
public class AddressApiController {
    @Resource
    private AddressService addressService;

    @ApiOperation(value = "查询地址详情", nickname = "查询地址")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "address", value = "普通钱包地址", required = true, dataType = "String"),
    })
    @GetMapping("getAddressInfo")
    public ApiResult getAddressInfo(String address) {
        if (StringUtils.isBlank(address)){
            return new ApiResult(-200,"地址不能为null");
        }
        JSONObject addressJson = addressService.getAddressInfo(address.trim());
        if (addressJson == null){
            return new ApiResult(-200,"暂时没有保留此地址");
        }
        return new ApiResult(200,"查询成功",addressJson);
    }

    @ApiOperation(value = "查询地址列表（根据金额）", nickname = "查询地址")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pageIndex", value = "页数", required = true, dataType = "int"),
            @ApiImplicitParam(name = "pageSize", value = "每页几条数据", required = true, dataType = "int"),
    })
    @GetMapping("getAddressByAssets")
    public ApiResult getAddressByAssets(Integer pageIndex,Integer pageSize) {

        return new ApiResult(200,"查询成功",addressService.getAddressByAssets(pageIndex,pageSize));
    }

    @ApiOperation(value = "查找普通地址的委托（delegator）和解委托信息(undelegator)", nickname = "查询地址")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "address", value = "普通钱包地址", required = true, dataType = "String"),
            @ApiImplicitParam(name = "type", value = "类型（delegator，undelegator）", required = true, dataType = "String"),
            @ApiImplicitParam(name = "pageIndex", value = "页数", required = true, dataType = "int"),
            @ApiImplicitParam(name = "pageSize", value = "每页几条数据", required = true, dataType = "int"),
    })
    @GetMapping("getAddressDelegateInfo")
    public ApiResult getAddressDelegateInfo(String address,String type,Integer pageIndex,Integer pageSize) {
        if (StringUtils.isBlank(address)){
            return new ApiResult(-200,"地址不能为null");
        }
        if (pageIndex == null || pageIndex == 0){
            pageIndex = 1;
        }
        if (pageSize == null || pageSize == 0){
            pageSize = 20;
        }
        return new ApiResult(200,"查询成功",addressService.getAddressDelegateInfo(address.trim(),type,pageIndex,pageSize));
    }

    @ApiOperation(value = "获取币种发行量等信息", nickname = "查询coin")
    @GetMapping("getCoinInfo")
    public ApiResult getCoinInfo() {

        return new ApiResult(200,"查询成功",addressService.getCoinInfo());
    }

    @ApiOperation(value = "获取财产占比(扇形图)", nickname = "查询coin")
    @GetMapping("getCoinRange")
    public ApiResult getCoinRange() {

        return new ApiResult(200,"查询成功",addressService.getCoinRange());
    }


    @ApiOperation(value = "更新amount", nickname = "查询coin")
    @GetMapping("updateBalance")
    public void updateBalance() {
        addressService.updateBalance();
    }
}
