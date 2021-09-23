package com.wuyuan.blockbrowse.entity;

import lombok.Data;

import java.util.List;

@Data
public class EventPrams {

    private String page;

    private Integer isEvent;

    private String methodName;

    private List<String> parms;

    private String blockNum;
}
