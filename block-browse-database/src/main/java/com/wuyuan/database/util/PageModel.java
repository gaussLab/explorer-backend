package com.wuyuan.database.util;

import java.io.Serializable;
import java.util.List;

public class PageModel<T> implements Serializable {
    /**
     * @Fields: serialVersionUID
     * @Todo: TODO
     */
    private static final long serialVersionUID = 1L;
    // 当前页
    private Integer page = 1;
    // 当前页面条数
    private Integer size = 10;

    private int pages;
    private long total;
    private String chainName;
    private List<T> records;

    public PageModel(List<T> records, int page, int size, long total ){
        this.records=records;
        this.size=size;
        this.page=page;
        this.total=total;
    }

    public PageModel(List<T> records, int page, int size, long total ,String chainName){
        this.records=records;
        this.size=size;
        this.page=page;
        this.total=total;
        this.chainName=chainName;
    }
    public PageModel(){

    }

    public String getChainName() {
        return chainName;
    }

    public void setChainName(String chainName) {
        this.chainName = chainName;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public long getPages() {
        if (total%size > 0){
            return total/size + 1;
        }
        return total/size;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }
}
