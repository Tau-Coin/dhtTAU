package io.taucoin.torrent.publishing.ui.user;

import android.app.Application;
import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import com.frostwire.jlibtorrent.Ed25519;
import com.frostwire.jlibtorrent.Pair;
import com.github.naturs.logger.Logger;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.UserRepository;
import io.taucoin.torrent.publishing.core.storage.entity.User;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.util.ByteUtil;

/**
 * 用户相关的ViewModel
 */
public class UserViewModel extends AndroidViewModel {

    private UserRepository userRepo;
    private CompositeDisposable disposables = new CompositeDisposable();
    private MutableLiveData<String> changeResult = new MutableLiveData<>();
    private MutableLiveData<List<User>> blackList = new MutableLiveData<>();
    private CommonDialog commonDialog;
    public UserViewModel(@NonNull Application application) {
        super(application);
        userRepo = RepositoryHelper.getUserRepository(getApplication());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
        if(commonDialog != null && commonDialog.isShowing()){
            commonDialog.dismiss();
            commonDialog = null;
        }
    }

    /**
     * 显示保存Seed的对话框，也起到确认效果,确认后执行后续操作
     * @param seed 不null：导入; null:生成新的seed
     */
    public void showSaveSeedDialog(Context context, String seed){
        View view = LinearLayout.inflate(context, R.layout.view_dialog, null);
        commonDialog = new CommonDialog.Builder(context)
                .setContentView(view)
                .setButtonWidth(240)
                .setExchange(seed == null)
                .setPositiveButton(R.string.common_proceed, (dialog, which) -> {
                    dialog.cancel();
                    if(StringUtil.isNotEmpty(seed)){
                        importSeed(seed);
                    }else{
                        generateSeed();
                    }
                }).setNegativeButton(R.string.common_back, (dialog, which) -> dialog.cancel())
                .create();
        commonDialog.show();
    }

    /**
     * 导入/切换Seed
     */
    private void importSeed(String seed) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<String>) emitter -> {
            String result = "";
            try {
                byte[] seedBytes = ByteUtil.toByte(seed);
                Pair<byte[], byte[]> keypair = Ed25519.createKeypair(seedBytes);
                String publicKey = ByteUtil.toHexString(keypair.first);
                Logger.d("import publicKey::%s, size::%d", publicKey, publicKey.length());
                userRepo.setCurrentUser(MainApplication.getInstance().getPublicKey(), false);
                User user = new User(publicKey, seed, null, true);
                userRepo.addUser(user);
            }catch (Exception e){
                result = e.getMessage();
                Logger.d("import seed error::%s", seed, result);
            }
            emitter.onNext(result);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::importSeedResult);
        disposables.add(disposable);
    }

    /**
     * 导入Seed结果
     */
    private void importSeedResult(String result){
        changeResult.postValue(result);
    }

    /**
     * 生成新的Seed
     */
    private void generateSeed() {
        byte[] seedBytes = Ed25519.createSeed();
        String seed = ByteUtil.toHexString(seedBytes);
        importSeed(seed);
    }

    /**
     * 观察当前用户是否变化
     */
    public Flowable<User> observeCurrentUser() {
        return userRepo.observeCurrentUser();
    }

    /**
     * 获取切换Seed后结果
     */
    MutableLiveData<String> getChangeResult() {
        return changeResult;
    }

    /**
     * 获取用户黑名单的被观察者
     * @return 被观察者
     */
    public MutableLiveData<List<User>> getBlackList() {
        return blackList;
    }
    /**
     * 获取在黑名单的用户列表
     */
    public void getUsersInBlacklist() {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<List<User>>) emitter -> {
            List<User> list = userRepo.getUsersInBlacklist();
            emitter.onNext(list);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> blackList.postValue(list));
        disposables.add(disposable);
    }

    /**
     * 设置用户是否加入黑名单
     * @param publicKey 用户publicKey
     * @param blacklist 是否加入黑名单
     */
    public void setUserBlacklist(String publicKey, boolean blacklist) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Boolean>) emitter -> {
            userRepo.setUserBlacklist(publicKey, blacklist);
            emitter.onNext(true);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
        disposables.add(disposable);
    }
}