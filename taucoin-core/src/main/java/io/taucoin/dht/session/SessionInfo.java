package io.taucoin.dht.session;

public class SessionInfo {

    // Session index
    private int index;

    // node id
    private String nids;

    // dht nodes
    private long nodes;

    public SessionInfo(int index, String nids, long nodes) {
        this.index = index;
        this.nids = nids;
        this.nodes = nodes;
    }

    public int index() {
        return index;
    }

    public String nids() {
        return nids;
    }

    public long nodes() {
        return nodes;
    }

    @Override
    public String toString() {
        return "[" + "id:" + index + ", nids:" + nids + ", nodes:" + nodes + "]";
    }
}
