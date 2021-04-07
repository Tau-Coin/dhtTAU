package io.taucoin.torrent.publishing.core.model.data;

public class DataChanged {
    private boolean refresh = true;     // 是否刷新
    private String msg;                  // 消息

    public boolean isRefresh() {
        return refresh;
    }

    public void setRefresh(boolean refresh) {
        this.refresh = refresh;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
