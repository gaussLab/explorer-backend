package io.cosmos.msg.utils.type;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.gson.annotations.SerializedName;
import io.cosmos.crypto.Crypto;
import io.cosmos.types.CommissionMsg;
import io.cosmos.types.Description;
import io.cosmos.types.Pubkey;
import io.cosmos.types.Token;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.bouncycastle.util.Strings;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder(alphabetic = true)
public class MsgCreateValidatorValue {

    private CommissionMsg commission;

    @JsonProperty("delegator_address")
    @SerializedName("delegator_address")
    private String delegatorAddress;

    private Description description;

    @JsonProperty("min_self_delegation")
    @SerializedName("min_self_delegation")
    private String minSelfDelegation;

    private String pubkey;

    @JsonProperty("validator_address")
    @SerializedName("validator_address")
    private String validatorAddress;
    private Token value;

    public void setValue(Token value) {
        this.value = value;
    }
    public void setPubKey(String pubKeyString) {
        this.pubkey= pubKeyString;
    }

    public void setDelegatorAddress(String delegatorAddress) {
        this.delegatorAddress = delegatorAddress;
    }

    public void setValidatorAddress(String validatorAddress) {
        this.validatorAddress = validatorAddress;
    }

    public void setMinSelfDelegation(String minSelfDelegation) {
        this.minSelfDelegation = minSelfDelegation;
    }

    public void setDescription(Description description) {
        this.description = description;
    }

    public void setCommission(CommissionMsg commission) {
        this.commission = commission;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("delegator_address", delegatorAddress)
                .append("validator_address", validatorAddress)
                .append("value", this.value)
                .append("min_self_delegation", this.minSelfDelegation)
                .append("description", this.description)
                .append("commission", this.commission)
                .append("pubkey", this.pubkey)
                .toString();
    }
}
