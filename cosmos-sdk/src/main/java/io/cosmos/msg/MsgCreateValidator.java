package io.cosmos.msg;

import io.cosmos.common.EnvInstance;
import io.cosmos.msg.utils.Message;
import io.cosmos.msg.utils.type.MsgCreateValidatorValue;
import io.cosmos.types.CommissionMsg;
import io.cosmos.types.Description;
import io.cosmos.types.Token;

import java.util.ArrayList;
import java.util.List;


public class MsgCreateValidator extends MsgBase{

    public static void main(String[] args) {
        EnvInstance.setEnv("local");
        MsgCreateValidator msg = new MsgCreateValidator();
        msg.setMsgType("cosmos-sdk/MsgCreateValidator");
        msg.initMnemonic(EnvInstance.getEnv().GetNode5Mnmonic());
        Message messages = msg.produceMsg();
        msg.submit(messages, "10"+"000000000", "200000", "");
    }

    public Message produceMsg() {
        MsgCreateValidatorValue value = new MsgCreateValidatorValue();
        value.setDelegatorAddress(this.address);
        value.setValidatorAddress(this.operAddress);
        value.setPubKey(EnvInstance.getEnv().GetTendermintConsensusPubkey());

        Description d = new Description();
        d.setDetails("node5");
        d.setIdentity("2");
        d.setMoniker("3");
        d.setWebsite("4");

        CommissionMsg c = new CommissionMsg();
        c.setMaxChangeRate("0.050000000000000000");
        c.setMaxRate("0.050000000000000000");
        c.setRate("0.050000000000000000");

        Token t = new Token();
        t.setAmount("10" + "000000000");
        t.setDenom(EnvInstance.getEnv().GetDenom());

        value.setValue(t);
        value.setCommission(c);
        value.setDescription(d);
        value.setMinSelfDelegation("1" + "000000000");

        Message<MsgCreateValidatorValue> msg = new Message<>();
        msg.setType(msgType);
        msg.setValue(value);
        return msg;
    }

}
