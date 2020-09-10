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

    public static int TauDHTMaxItems = 1000;

    private static final boolean EnablePrivateNetwork = false;

    public static final String PN_Listen_Interface = "0.0.0.0:6883";

    public static Set<String> PrivateBootstrapNodes = new HashSet<String>();
    static {
        PrivateBootstrapNodes.add("52.74.25.27:6883");
        //PrivateBootstrapNodes.add("tau.geekgalaxy.com:6881");
    }

    // DHT bootstrap nodes.
    final Set<String> bootstrapNodes;

    // DHT max items.
    final int maxDhtItems;

    // Use private network or not.
    final boolean privateNetwork;

    // DHT bootstrap nodes for private network.
    final Set<String> privateBootstrapNodes;

    public SessionSettings(Builder builder) {

        if (builder.bootstrapNodes == null) {
            this.bootstrapNodes = new HashSet<String>();
        } else {
            this.bootstrapNodes = builder.bootstrapNodes;
        }

        if (builder.privateBootstrapNodes == null) {
            this.privateBootstrapNodes = new HashSet<String>();
        } else {
            this.privateBootstrapNodes = builder.privateBootstrapNodes;
        }

        if (builder.maxDhtItems <= 0) {
            this.maxDhtItems = TauDHTMaxItems;
        } else {
            this.maxDhtItems = builder.maxDhtItems;
        }

        this.privateNetwork = builder.privateNetwork;

        for (String node: DefaultBootstrapNodes) {
            this.bootstrapNodes.add(node);
        }

        for (String node: PrivateBootstrapNodes) {
            this.privateBootstrapNodes.add(node);
        }
    }

    public SessionParams getSessionParams() {
        settings_pack sp = new settings_pack();

        if (!this.privateNetwork) {
            sp.set_str(settings_pack.string_types.dht_bootstrap_nodes.swigValue(),
                    dhtBootstrapNodes());
        } else {
            sp.set_str(settings_pack.string_types.dht_bootstrap_nodes.swigValue(),
                    privateDhtBootstrapNodes());
            sp.set_str(settings_pack.string_types.listen_interfaces.swigValue(),
                    PN_Listen_Interface);
        }

        session_params sparams = new session_params(sp);

        dht_settings ds = new dht_settings();
        ds.setMax_dht_items(this.maxDhtItems);
        if (this.privateNetwork) {
            // For private network, unrestrict dht entries to one per IP.
            ds.setRestrict_routing_ips(false);
        }

        sparams.setDht_settings(ds);

        return new SessionParams(sparams);
    }

    private String dhtBootstrapNodes() {
        StringBuilder sb = new StringBuilder();

        for (String n : this.bootstrapNodes) {
            sb.append(n).append(",");
        }

        String s = sb.toString();
        String result = s.substring(0, s.length() - 1);

        return result;
    }

    private String privateDhtBootstrapNodes() {
        StringBuilder sb = new StringBuilder();

        for (String n : this.privateBootstrapNodes) {
            sb.append(n).append(",");
        }

        String s = sb.toString();
        String result = s.substring(0, s.length() - 1);

        return result;
    }

    public static final class Builder {

        Set<String> bootstrapNodes;

        int maxDhtItems;

        boolean privateNetwork;

        Set<String> privateBootstrapNodes;

        /**
         * Builder constructor.
         */
        public Builder() {
            this.bootstrapNodes = null;
            this.maxDhtItems = -1;
            this.privateNetwork = EnablePrivateNetwork;
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
         * Enable private network or not.
         *
         * @param enable true or false
         * @return Builder
         */
        public Builder enablePrivateNetwork(boolean enable) {
            this.privateNetwork = enable;
            return this;
        }

        /**
         * Add dht bootstrap nodes for private network.
         *
         * @param nodes dht bootstrap nodes.
         * @param Builder
         */
        public Builder addPrivateBootstrapNodes(Set<String> nodes) {
            this.privateBootstrapNodes = nodes;
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
