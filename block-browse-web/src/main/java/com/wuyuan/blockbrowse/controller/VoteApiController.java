package com.wuyuan.blockbrowse.controller;

import com.wuyuan.blockbrowse.utils.ApiResult;
import com.wuyuan.database.sevice.VoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Api(tags = "投票和提案相关")
@RestController
@RequestMapping("proposal")
public class VoteApiController {
    @Resource
    private VoteService voteService;

    @ApiOperation(value = "通过提案id查询提案", nickname = "查询提案")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "proposalId", value = "提案id", required = true, dataType = "String"),
    })
    @GetMapping("getProposalByProposalId")
    public ApiResult getProposalByProposalId(String proposalId) {
        if (StringUtils.isBlank(proposalId)){
            return new ApiResult(-200,"proposalId不能为null");
        }
        return new ApiResult(200,"查询成功",voteService.getProposalByProposalId(proposalId.trim()));
    }

    @ApiOperation(value = "获取提案列表", nickname = "查询提案")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pageIndex", value = "页数", required = true, dataType = "int"),
            @ApiImplicitParam(name = "pageSize", value = "每页几条数据", required = true, dataType = "int"),
    })
    @GetMapping("getProposalIdList")
    public ApiResult getProposalIdList(Integer pageIndex,Integer pageSize) {
        if (pageIndex == null || pageIndex == 0){
            pageIndex = 1;
        }
        if (pageSize == null || pageIndex == 0){
            pageSize = 40;
        }
        return new ApiResult(200,"查询成功",voteService.getProposalList(pageIndex,pageSize));
    }
}
