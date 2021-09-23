package cosmos;

import lombok.Data;

import java.util.List;
@Data
public class Block {

    //chain_id	string
    private String chain_id;
    //example: cosmoshub-2
    private long height;
    //height	number
    //example: 1
    //time	string
    private String time;
    //example: 2017-12-30T05:53:09.287+01:00
    //num_txs	number
    private long num_txs;
    //example: 0
    private long total_txs;
    //total_txs	number
    //example: 35
    private String last_commit_hash;
    //last_commit_hash	string
    //example: EE5F3404034C524501629B56E0DDC38FAD651F04
    //data_hash	string
    private String data_hash;
    //example: EE5F3404034C524501629B56E0DDC38FAD651F04
    private String validators_hash;
    //validators_hash	string
    //example: EE5F3404034C524501629B56E0DDC38FAD651F04
    private String next_validators_hash;
    //next_validators_hash	string
    //example: EE5F3404034C524501629B56E0DDC38FAD651F04
    //consensus_hash	string
    private String consensus_hash;
    //example: EE5F3404034C524501629B56E0DDC38FAD651F04
    //app_hash	string
    private String app_hash;
    //example: EE5F3404034C524501629B56E0DDC38FAD651F04
    //last_results_hash	string
    private String last_results_hash;
    //example: EE5F3404034C524501629B56E0DDC38FAD651F04
    //evidence_hash	string
    private String evidence_hash;
    //example: EE5F3404034C524501629B56E0DDC38FAD651F04
    //proposer_address	string
    private String proposer_address;
    //example: cosmos1depk54cuajgkzea6zpgkq36tnjwdzv4afc3d27
    private String encoded;
    private Version version;
    private LastCommit last_commit;

}
class  Version{
    private String block;
//        private String app;
}
class LastCommit{
    private String parentHash;
    private long parentHeight;
    private List<Signatures> signatures;
}
class Signatures{
    //validator_address	string
    private String validator_address;
    //validator_index	string
    //example: 0
    //height	string
    private String height;
    //example: 0
    //example: 0
    //timestamp	string
    private String timestamp;
    //example: 2017-12-30T05:53:09.287+01:00
    //type	number
    private int block_id_flag;
    //example: 2
    //block_id	{...}
    //signature	string
    private String signature;

}
