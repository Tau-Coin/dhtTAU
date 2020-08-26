package io.taucoin.torrent.publishing.core.utils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

public class ChainLinkUtil {
    private static String SCHEMA = "TAUchain:?";
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
            String[] data = link.split(SCHEMA);
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
        for (int i = 0; i < publicKeys.size(); i++) {
            if(i > 0){
                link.append(PARAMS_SEPARATOR);
            }
            link.append(BS);
            link.append(VALUE_SEPARATOR);
            link.append(publicKeys.get(i));
        }
        link.append(PARAMS_SEPARATOR);
        link.append(DN);
        link.append(VALUE_SEPARATOR);
        link.append(chainID);
        return link.toString();
    }

    public static class ChainLink{
        private List<String> bootstraps = new ArrayList<>();
        private String dn;

        public void addBootstrap(String bs){
            this.bootstraps.add(bs);
        }

        public void setDn(String dn){
            this.dn = dn;
        }

        public List<String> getBootstraps() {
            return bootstraps;
        }

        public String getDn() {
            return dn;
        }

        public boolean isValid() {
            return StringUtil.isNotEmpty(dn);
        }
    }
}
