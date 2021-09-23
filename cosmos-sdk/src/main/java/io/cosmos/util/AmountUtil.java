package io.cosmos.util;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

public class AmountUtil {
    private String coin;
    private BigDecimal amount;

    public static void main(String[] args) {
        System.out.println("usdg1tq2nytr42q7kwr8zgk9v26hucjzfjhmq9yv4aw".length());
    }
    public static AmountUtil initAmount(String amountStr,BigDecimal decimal,int scale) {
        AmountUtil amountUtil = new AmountUtil();
        String coin=getCoin(amountStr);
        amountUtil.setCoin(coin);
        amountUtil.setAmount(getAmount(amountStr,coin,decimal,scale));
        return amountUtil;
    }
    public static BigDecimal getAmount(String amountStr,String coin,BigDecimal decimal,int scale){
        if(StringUtils.isBlank(amountStr)){
            return BigDecimal.ZERO;
        }
        BigDecimal amount=BigDecimal.ZERO;
        if(StringUtils.isNoneBlank()){
            int index=amountStr.indexOf(coin);
            amount=new BigDecimal(amountStr.substring(0,index)).divide(decimal,scale,BigDecimal.ROUND_HALF_UP);
            return amount;
        }
        return new BigDecimal(amountStr).divide(decimal,6,BigDecimal.ROUND_HALF_UP);
    }
    public static String getCoin(String amountStr) {
        String coin = "";
        if (StringUtils.isBlank(amountStr)) {
            return null;
        }
        char[] chars=amountStr.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            boolean isNum=Character.isDigit(chars[i]);
            if(!isNum){
                return amountStr.substring(i);
            }
        }

        return coin;
    }

    public String getCoin() {
        return coin;
    }

    public void setCoin(String coin) {
        this.coin = coin;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
