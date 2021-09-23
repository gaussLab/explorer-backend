package com.wuyuan.database.entity;

public class Transfer {
    public static final String transfer = "/ibc.applications.transfer.v1.MsgTransfer";

    public static final String recvPacket = "/ibc.core.channel.v1.MsgRecvPacket";

    public static final String aknowledgement = "/ibc.core.channel.v1.MsgAcknowledgement";

    public static final String timeOut = "/ibc.core.channel.v1.MsgTimeout";

    public static final String SetOrchestratorAddress = "/gravity.v1.MsgSetOrchestratorAddress";

    public static final String ValsetConfirm = "/gravity.v1.MsgValsetConfirm";

    public static final String DepositClaim = "/gravity.v1.MsgDepositClaim";

    public static final String ValsetUpdatedClaim = "/gravity.v1.MsgValsetUpdatedClaim";

    public static final String ConfirmBatch = "/gravity.v1.MsgConfirmBatch";

    public static final String RequestBatch = "/gravity.v1.MsgRequestBatch";

    public static final String SendToEth = "/gravity.v1.MsgSendToEth";

    public static final String WithdrawClaim = "/gravity.v1.MsgWithdrawClaim";

}
