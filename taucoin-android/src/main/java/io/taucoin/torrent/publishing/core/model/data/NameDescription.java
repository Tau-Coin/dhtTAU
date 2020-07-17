package io.taucoin.torrent.publishing.core.model.data;

/**
 * 社区名字的描述信息类
 */
public class NameDescription {
    private String personalProfile;  // 个人简介
    private int contactPlatform;     // 联系平台
    private String contactID;        // 联系平台ID

    public String getPersonalProfile() {
        return personalProfile;
    }

    public void setPersonalProfile(String personalProfile) {
        this.personalProfile = personalProfile;
    }

    public int getContactPlatform() {
        return contactPlatform;
    }

    public void setContactPlatform(int contactPlatform) {
        this.contactPlatform = contactPlatform;
    }

    public String getContactID() {
        return contactID;
    }

    public void setContactID(String contactID) {
        this.contactID = contactID;
    }
}
