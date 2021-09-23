package com.wuyuan.blockbrowse.controller;

import com.alibaba.fastjson.JSONObject;
import com.wuyuan.blockbrowse.utils.ApiResult;
import com.wuyuan.database.sevice.TransactionService;
import com.wuyuan.database.util.PageModel;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Api(tags = "交易相关")
@RestController
@RequestMapping("transaction")
public class TransactionApiController {
    @Resource
    private TransactionService transactionService;

    @ApiOperation(value = "查询交易详情（通过交易hash）", nickname = "查询交易")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "txHash", value = "交易hash", required = true, dataType = "String"),
    })
    @GetMapping("getTransactionByHash")
    public ApiResult getTransactionByHash(String txHash) {
        if (StringUtils.isBlank(txHash)){
            return new ApiResult(-200,"txhash不能为null");
        }
        JSONObject tx = transactionService.getTransactionByHash(txHash.trim());
        if (tx != null){
            return new ApiResult(200,"查询成功",tx);
        }
        return new ApiResult(-200,"查询失败");
    }

    @ApiOperation(value = "查询交易列表（通过不同条件）", nickname = "查询交易")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "blockHeight", value = "区块高度", required = false, dataType = "String"),
            @ApiImplicitParam(name = "bigType", value = "大交易类型(transfer(正常交易),delegator(委托相关),reward(收益相关),vote(治理相关),jail(出入狱相关))", required = false, dataType = "String"),
            @ApiImplicitParam(name = "smallType", value = "小交易类型(send,transfer,multi,beginredelegate," +
                    "delegator,undelegate,withdrawdelegatorreward,setwithdrawaddress,validatorcommission," +
                    "vote,deposit,editvalidator,createvalidator,jail,issuetoken,unlocktoken,submitproposal," +
                    "transfertokenowner,createpool,addpledge,minttoken,edittoken,placeorder" +
                    ",agreeorderpair,createclient,updateclient,connectionopenconfirm,connectionopeninit,channelopeninit," +
                    "timeout,channelopenack,connectionopenack,connectionopentry,recvpacket,channelopentry)", required = false, dataType = "String"),
            @ApiImplicitParam(name = "address", value = "钱包地址或者验证人操作地址", required = false, dataType = "String"),
            @ApiImplicitParam(name = "proposalId", value = "提案id(只有在小类型是vote时传入这个才有效)", required = false, dataType = "String"),
            @ApiImplicitParam(name = "voteType", value = "投票类型（yes,no,nowithveto,abstain）", required = false, dataType = "String"),
            @ApiImplicitParam(name = "status", value = "交易状态(0成功，1失败)", required = false, dataType = "int"),
            @ApiImplicitParam(name = "pageIndex", value = "页数", required = false, dataType = "int"),
            @ApiImplicitParam(name = "pageSize", value = "每页几条数据", required = false, dataType = "int"),
            @ApiImplicitParam(name = "startTime", value = "开始时间(格式必须为xxxx-xx-xx)", required = false, dataType = "String"),
            @ApiImplicitParam(name = "endTime", value = "结束时间(格式必须为xxxx-xx-xx)", required = false, dataType = "String"),
            @ApiImplicitParam(name = "isAccording", value = "isAccording 是否根据指定高度查出他以下的所有交易的分页(0或者null 否， 1 是)", required = false, dataType = "int"),
    })
    @GetMapping("getTransactionByBlockNumber")
    public ApiResult getTransactionByBlockNumber(String blockHeight,String bigType,String smallType,String address,String proposalId,String voteType,Integer status,Integer pageIndex, Integer pageSize,String startTime,String endTime,Integer isAccording) {
        if (pageIndex == null || pageIndex == 0){
            pageIndex = 1;
        }
        if (pageSize == null || pageSize == 0){
            pageSize = 30;
        }
        return new ApiResult(200,"查询成功",transactionService.getTransaction(blockHeight,bigType,smallType,address,proposalId,voteType,status,pageIndex,pageSize,startTime,endTime,isAccording));
    }

    @ApiOperation(value = "获取该地址所有交易类型的数量", nickname = "查询交易")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "address", value = "钱包地址", required = false, dataType = "String"),
            @ApiImplicitParam(name = "blockHeight", value = "区块高度", required = false, dataType = "String"),
    })
    @GetMapping("getAllTypeCount")
    public ApiResult getAllTypeCount(String address,String blockHeight) {
        return new ApiResult(200,"查询成功",transactionService.getAllTypeCount(address,blockHeight));
    }

    @ApiOperation(value = "查询委托人和验证人相关的交易", nickname = "查询交易")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "bigType", value = "大交易类型(delegation,validation,deposit)", required = true, dataType = "String"),
            @ApiImplicitParam(name = "smallType", value = "小交易类型(若大类型为delegation,则小类型为（delegator，undelegate，withdrawdelegatorreward，setwithdrawaddress，beginredelegate）,若大类型为validation,则（createvalidator，editvalidator，unjail，withdrawvalidatorcommission）)", required = false, dataType = "String"),
            @ApiImplicitParam(name = "status", value = "交易状态(0成功，1失败)", required = false, dataType = "int"),
            @ApiImplicitParam(name = "pageIndex", value = "页数", required = false, dataType = "int"),
            @ApiImplicitParam(name = "pageSize", value = "每页几条数据", required = false, dataType = "int"),
            @ApiImplicitParam(name = "startTime", value = "开始时间(格式必须为xxxx-xx-xx)", required = false, dataType = "String"),
            @ApiImplicitParam(name = "endTime", value = "结束时间(格式必须为xxxx-xx-xx)", required = false, dataType = "String"),
    })
    @GetMapping("getValidatorRelatedTx")
    public ApiResult getValidatorRelatedTx(String bigType,String smallType,String startTime,String endTime,Integer status,Integer pageIndex,Integer pageSize) {
        if (StringUtils.isBlank(bigType)){
            return new ApiResult(-200,"大类型不能为null");
        }
        if (pageIndex == null || pageIndex == 0){
            pageIndex = 1;
        }
        if (pageSize == null || pageSize == 0){
            pageSize = 30;
        }
        return new ApiResult(200,"查询成功",transactionService.getValidatorRelatedTx(bigType.trim(),smallType,startTime,endTime,status,pageIndex,pageSize));
    }

//    @ApiOperation(value = "保存tx", nickname = "查询交易")
//    @ApiImplicitParams({
//            @ApiImplicitParam(name = "hash", value = "hash", required = false, dataType = "String"),
//    })
//    @GetMapping("saveTx")
//    public ApiResult saveTx(String hash) {
//        return new ApiResult(200,"查询成功",transactionService.saveTx(hash));
//    }
}
