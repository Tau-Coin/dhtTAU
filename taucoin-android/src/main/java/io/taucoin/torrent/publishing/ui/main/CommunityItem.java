package io.taucoin.torrent.publishing.ui.main;

import android.os.Parcel;
import android.os.Parcelable;

public class CommunityItem implements Parcelable {
    private String chainId;
    private String communityName;
    private String userName;
    private String message;
    private long time;
    private int messageCount;

    protected CommunityItem() {
    }

    protected CommunityItem(Parcel in) {
        chainId = in.readString();
        communityName = in.readString();
        userName = in.readString();
        message = in.readString();
        time = in.readLong();
        messageCount = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(chainId);
        dest.writeString(communityName);
        dest.writeString(userName);
        dest.writeString(message);
        dest.writeLong(time);
        dest.writeInt(messageCount);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<CommunityItem> CREATOR = new Creator<CommunityItem>() {
        @Override
        public CommunityItem createFromParcel(Parcel in) {
            return new CommunityItem(in);
        }

        @Override
        public CommunityItem[] newArray(int size) {
            return new CommunityItem[size];
        }
    };

    public String getChainId() {
        return chainId;
    }

    public void setChainId(String chainId) {
        this.chainId = chainId;
    }

    public String getCommunityName() {
        return communityName;
    }

    public void setCommunityName(String communityName) {
        this.communityName = communityName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    @Override
    public int hashCode() {
        return communityName.hashCode();
    }

    /*
     * Compare objects by their content
     */

    public boolean equalsContent(CommunityItem item) {
        return super.equals(item);
    }

    /*
     * Compare objects by torrent id
     */

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CommunityItem))
            return false;

        if (o == this)
            return true;

        return communityName.equals(((CommunityItem)o).communityName);
    }
}
