package io.cosmos.util;

import io.cosmos.crypto.encode.Bech32;
import org.bitcoinj.core.Sha256Hash;
import org.spongycastle.util.encoders.Hex;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

public class PubkeyUtil {
    public static String hexToBech32(String address,String prefix) throws UnsupportedEncodingException {
        byte[] bytes= Hex.decode(address.getBytes());
        StringBuffer sb=new StringBuffer();
        for (byte b: bytes) {
            sb.append((int)b+",");
        }
        bytes=Bech32.toWords(bytes);
        return Bech32.encode(prefix,bytes );
    }

    public static String getAddressFromPubkey(String pubkey){
        byte[] bytes= Base64.getDecoder().decode(pubkey.getBytes());

        byte[] hash= Sha256Hash.hash(bytes);
        byte[] newByte=new byte[20];
        System.arraycopy(hash,0,newByte,0,20);
        return Hex.toHexString(newByte).toUpperCase();
    }
}
