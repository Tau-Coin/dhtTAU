package io.taucoin.torrent.publishing;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;

public class MainApplication extends MultiDexApplication {
    static {
        /* Vector Drawable support in ImageView for API < 21 */
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private static MainApplication instance;
    private String publicKey;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

//        // SQLite数据库测试
//        Stetho.initializeWithDefaults(this);
    }

    public static MainApplication getInstance(){
        return instance;
    }

    /**
     * 获取全局参数 当前用户的publicKey
     * @return  publicKey 公钥
     */
    public String getPublicKey() {
        return publicKey;
    }

    /**
     * 设置全局参数 当前用户的publicKey
     * @param publicKey 公钥
     */
    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
}