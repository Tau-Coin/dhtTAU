package io.taucoin.dht;

public enum DHTReqResult {

    Success("success"),
    Dropped("drop item due to max capability"),
    Duplicated("duplicated item");

    private String result;

    private DHTReqResult(String result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return result;
    }
}
