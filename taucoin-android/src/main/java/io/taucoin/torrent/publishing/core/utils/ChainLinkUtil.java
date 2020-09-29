package io.taucoin.torrent.publishing.core.utils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import io.taucoin.util.ByteUtil;

public class ChainLinkUtil {
    private static String SCHEMA = "tauchain:?";
    private static String SCHEMA_REGEX = "tauchain:\\?";
    private static String PARAMS_SEPARATOR = "&";
    private static String VALUE_SEPARATOR = "=";
    private static String BS = "bs";
    private static String DN = "dn";
    /**
     * TAUChain 编码
     * @param link
     * @return
     */
    public static ChainLink decode(String link) {
        ChainLink chainLink = new ChainLink();
        if(StringUtil.isNotEmpty(link)){
            link = link.trim();
            String[] data = link.split(SCHEMA_REGEX);
            if(data.length == 2){
                String[] params = data[1].split(PARAMS_SEPARATOR);
                if(params.length > 0){
                    for (String param : params) {
                        String[] keyValues = param.split(VALUE_SEPARATOR);
                        if (keyValues.length == 2) {
                            if (StringUtil.isEquals(keyValues[0], BS)) {
                                chainLink.addBootstrap(keyValues[1]);
                            } else if (StringUtil.isEquals(keyValues[0], DN)) {
                                chainLink.setDn(keyValues[1]);
                            }
                        }
                    }
                }
            }
        }
        return chainLink;
    }

    /**
     * TAUChain 解码
     * @param chainID
     * @param publicKeys
     * @return
     */
    public static String encode(@NonNull String chainID, @NonNull List<String> publicKeys) {
        StringBuilder link = new StringBuilder();
        link.append(SCHEMA);
        link.append(DN);
        link.append(VALUE_SEPARATOR);
        link.append(chainID);
        for (int i = 0; i < publicKeys.size(); i++) {
            link.append(PARAMS_SEPARATOR);
            link.append(BS);
            link.append(VALUE_SEPARATOR);
            link.append(publicKeys.get(i));
        }
        return link.toString();
    }

    public static class ChainLink{
        private List<String> bootstraps = new ArrayList<>();
        private String dn;
        private byte[] bytesDn;
        private List<byte[]> bytesBootstraps;

        public void addBootstrap(String bs){
            this.bootstraps.add(bs);
            try {
                bytesBootstraps.add(ByteUtil.toByte(bs));
            } catch (Exception ignore) {}
        }

        public void setDn(String dn){
            this.dn = dn;
            this.bytesDn = dn.getBytes();
        }

        public List<byte[]> getBytesBootstraps() {
            return bytesBootstraps;
        }

        public List<String> getBootstraps() {
            return bootstraps;
        }

        public String getDn() {
            return dn;
        }

        public byte[] getBytesDn() {
            return bytesDn;
        }
        public boolean isValid() {
            return StringUtil.isNotEmpty(dn);
        }
    }
}
