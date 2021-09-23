package com.wuyuan.blockbrowse.config;

import com.alibaba.fastjson.serializer.ValueFilter;

import java.math.BigDecimal;

/**
 * Description: BigDecimalValueFilter
 *
 * @date 4/23/21 17:48
 */
public class BigDecimalValueFilter implements ValueFilter {
    @Override
    public Object process(Object object, String name, Object value) {
        if(value instanceof BigDecimal) {
            return ((BigDecimal) value).toPlainString();
        }if (value instanceof Double){
            return new BigDecimal(((Double) value).doubleValue()).toPlainString();
        }
        return value;
    }
}