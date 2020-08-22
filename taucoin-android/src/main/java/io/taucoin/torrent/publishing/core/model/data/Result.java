package io.taucoin.torrent.publishing.core.model.data;

public class Result {
    private boolean success = true;     // 是否成功
    private String msg;                 // 消息

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void setFailMsg(String msg) {
        this.success = false;
        this.msg = msg;
    }
}
