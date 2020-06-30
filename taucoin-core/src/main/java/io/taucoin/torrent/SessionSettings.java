package io.taucoin.torrent;

import com.frostwire.jlibtorrent.SessionParams;
import com.frostwire.jlibtorrent.swig.*;

import java.util.HashSet;
import java.util.Set;

/**
 * SessionSettings includes options for torrent session.
 * You should use SessionSettings.Build to build this object.
 */
public final class SessionSettings {

    public static Set<String> DefaultBootstrapNodes = new HashSet<String>();
    static {
        DefaultBootstrapNodes.add("dht.libtorrent.org:25401");
        DefaultBootstrapNodes.add("router.bittorrent.com:6881");
        DefaultBootstrapNodes.add("dht.transmissionbt.com:6881");
        DefaultBootstrapNodes.add("router.silotis.us:6881");
    }

    public static int TauDHTMaxItems = 10000;

    // DHT bootstrap nodes.
    final Set<String> bootstrapNodes;

    // DHT max items.
    final int maxDhtItems;

    public SessionSettings(Builder builder) {

        if (builder.bootstrapNodes == null) {
            this.bootstrapNodes = new HashSet<String>();
        } else {
            this.bootstrapNodes = builder.bootstrapNodes;
        }

        if (builder.maxDhtItems <= 0) {
            this.maxDhtItems = TauDHTMaxItems;
        } else {
            this.maxDhtItems = builder.maxDhtItems;
        }

        for (String node: DefaultBootstrapNodes) {
            this.bootstrapNodes.add(node);
        }
    }

    public SessionParams getSessionParams() {
        settings_pack sp = new settings_pack();
        sp.set_str(settings_pack.string_types.dht_bootstrap_nodes.swigValue(), dhtBootstrapNodes());

        session_params sparams = new session_params(sp);
        dht_settings ds = new dht_settings();
        ds.setMax_dht_items(this.maxDhtItems);
        sparams.setDht_settings(ds);

        return new SessionParams(sparams);
    }

    private String dhtBootstrapNodes() {
        StringBuilder sb = new StringBuilder();

        for (String n : this.bootstrapNodes) {
            sb.append(n);
        }

        return sb.toString();
    }


    public static final class Builder {

        Set<String> bootstrapNodes;

        int maxDhtItems;

        /**
         * Builder constructor.
         */
        public Builder() {
            this.bootstrapNodes = null;
            this.maxDhtItems = -1;
        }

        /**
         * Add dht bootstrap nodes.
         *
         * @param nodes dht bootstrap nodes.
         * @param Builder
         */
        public Builder addBootstrapNodes(Set<String> nodes) {
            this.bootstrapNodes = nodes;
            return this;
        }

        /**
         * Set dht max items.
         *
         * @param max dht max items
         * @return Builder
         */
        public Builder setDHTMaxItems(int max) {
            this.maxDhtItems = max;
            return this;
        }

        /**
         * Build SessionSettings
         *
         * @return SessionSettings
         */
        public SessionSettings build() {
            return new SessionSettings(this);
        }
    }
}
