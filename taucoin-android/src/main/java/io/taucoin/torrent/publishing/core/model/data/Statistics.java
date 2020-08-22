package io.taucoin.torrent.publishing.core.model.data;

/**
 * 社区成员相关统计
 */
public class Statistics {
    private int members;     // 社区成员数
    private int online;      // 在线成员数

    public int getMembers() {
        return members;
    }

    public void setMembers(int members) {
        this.members = members;
    }

    public int getOnline() {
        return online;
    }

    public void setOnline(int online) {
        this.online = online;
    }
}
