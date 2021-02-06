package io.taucoin.dht2.session;

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

    // the starting listen port
    private static final int Listen_Port = 6881;
    private static final int Listen_Interfaces_Count = 32;

    // test network listen interface
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

    final String networkInterfaces;

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

        this.networkInterfaces = builder.networkInterfaces;
    }

    public SessionParams getSessionParams() {
        settings_pack sp = new settings_pack();

        if (!this.privateNetwork) {
            sp.set_str(settings_pack.string_types.dht_bootstrap_nodes.swigValue(),
                    dhtBootstrapNodes());
            sp.set_str(settings_pack.string_types.listen_interfaces.swigValue(),
                    networkInterfaces);
            sp.set_bool(settings_pack.bool_types.enable_ip_notifier.swigValue(), false);
            //sp.set_int(settings_pack.int_types.upload_rate_limit.swigValue(), 512);
            //sp.set_int(settings_pack.int_types.download_rate_limit.swigValue(), 512);
            //sp.set_int(settings_pack.int_types.dht_upload_rate_limit.swigValue(), 512);
            //sp.set_int(settings_pack.int_types.dht_download_rate_limit.swigValue(), 512);
        } else {
            sp.set_str(settings_pack.string_types.dht_bootstrap_nodes.swigValue(),
                    privateDhtBootstrapNodes());
            sp.set_str(settings_pack.string_types.listen_interfaces.swigValue(),
                    PN_Listen_Interface);
            sp.set_bool(settings_pack.bool_types.enable_ip_notifier.swigValue(), false);
        }

        session_params sparams = new session_params(sp);

        dht_settings ds = new dht_settings();
        ds.setMax_dht_items(this.maxDhtItems);
        ds.setMax_peers_reply(0);
        ds.setMax_fail_count(10);
        ds.setMax_torrents(0);
        ds.setMax_peers(0);
        ds.setMax_torrent_search_reply(0);

        if (this.privateNetwork) {
            // For private network, unrestrict dht entries to one per IP.
            ds.setRestrict_routing_ips(false);
            ds.setRestrict_search_ips(false);
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

    private static String constructListenInterfaces(int port, int count) {
        String interfaces = "";

        for (int i = 1; i < count; i++) {
            interfaces = interfaces + "0.0.0.0:" + port + ",";
            port++;
        }
        interfaces = interfaces + "0.0.0.0:" + port;

        return interfaces;
    }

    public static final class Builder {

        Set<String> bootstrapNodes;

        int maxDhtItems;

        boolean privateNetwork;

        Set<String> privateBootstrapNodes;

        String networkInterfaces;

        /**
         * Builder constructor.
         */
        public Builder() {
            this.bootstrapNodes = null;
            this.maxDhtItems = -1;
            this.privateNetwork = EnablePrivateNetwork;
            this.networkInterfaces = "0.0.0.0:0";
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
         * @return Builder
         */
        public Builder addPrivateBootstrapNodes(Set<String> nodes) {
            this.privateBootstrapNodes = nodes;
            return this;
        }

        /**
         * Set network interfaces.
         *
         * @param interfaces network interfaces
         * @return Builder
         */
        public Builder setNetworkInterfaces(String interfaces) {
            this.networkInterfaces = interfaces;
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
