package io.taucoin.torrent.publishing.ui.user;

import android.app.Application;

import com.frostwire.jlibtorrent.Ed25519;
import com.frostwire.jlibtorrent.Pair;
import com.github.naturs.logger.Logger;

import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.TauUtils;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.UserRepository;
import io.taucoin.torrent.publishing.core.storage.entity.User;

/**
 * 用户的ViewModel
 */
public class UserViewModel extends AndroidViewModel {

    private UserRepository userRepo;
    private CompositeDisposable disposables = new CompositeDisposable();
    public UserViewModel(@NonNull Application application) {
        super(application);
        userRepo = RepositoryHelper.getUserRepository(getApplication());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }

    /**
     * 导入/切换Seed
     */
    void importSeed(String seed) {
        if(StringUtil.isEmpty(seed)){
            ToastUtils.showShortToast(R.string.user_seed_empty);
            return;
        }
        Disposable disposable = Flowable.create((FlowableOnSubscribe<String>) emitter -> {
            String result = "";
            try {
                byte[] seedBytes = TauUtils.hexToBytes(seed);
                Logger.d("import seed::%s, size::%d", seed, seed.length());
                Logger.d("import seedBytes::%s, length::%d", Arrays.toString(seedBytes), seedBytes.length);
                Pair<byte[], byte[]> keypair = Ed25519.createKeypair(seedBytes);
                String publicKey = TauUtils.bytesToHex(keypair.first);
                Logger.d("import publicKey::%s, size::%d", publicKey, publicKey.length());
                User user = new User(publicKey, seed, "", true);
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
        if(StringUtil.isNotEmpty(result)){
            ToastUtils.showShortToast(result);
        }
    }

    /**
     * 生成新的Seed
     */
    void generateSeed() {
        byte[] seedBytes = Ed25519.createSeed();
        String seed = TauUtils.bytesToHex(seedBytes);
        Logger.d("generate seedBytes::%s, length::%d", Arrays.toString(seedBytes), seedBytes.length);
        importSeed(seed);
    }

    /**
     * 观察当前用户是否变化
     */
    public Flowable<User> observeCurrentUser() {
        return userRepo.observeCurrentUser();
    }
}