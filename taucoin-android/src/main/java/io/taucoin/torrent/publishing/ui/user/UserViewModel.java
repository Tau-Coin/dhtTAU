package io.taucoin.torrent.publishing.ui.user;

import android.app.Application;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import com.frostwire.jlibtorrent.Ed25519;
import com.frostwire.jlibtorrent.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.UserAndMember;
import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.BanDialogBinding;
import io.taucoin.torrent.publishing.databinding.ContactsDialogBinding;
import io.taucoin.torrent.publishing.databinding.SeedDialogBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.util.ByteUtil;

/**
 * 用户相关的ViewModel
 */
public class UserViewModel extends AndroidViewModel {

    private static final Logger logger = LoggerFactory.getLogger("UserViewModel");
    private UserRepository userRepo;
    private SettingsRepository settingsRepo;
    private CompositeDisposable disposables = new CompositeDisposable();
    private MutableLiveData<String> changeResult = new MutableLiveData<>();
    private MutableLiveData<Boolean> addContactResult = new MutableLiveData<>();
    private MutableLiveData<List<User>> blackList = new MutableLiveData<>();
    private MutableLiveData<UserAndMember> userDetail = new MutableLiveData<>();
    private CommonDialog commonDialog;
    public UserViewModel(@NonNull Application application) {
        super(application);
        userRepo = RepositoryHelper.getUserRepository(getApplication());
        settingsRepo = RepositoryHelper.getSettingsRepository(getApplication());
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
     * @param generate：false导入; true:生成新的seed
     */
    void showSaveSeedDialog(Context context, boolean generate){
        SeedDialogBinding binding = DataBindingUtil.inflate(LayoutInflater.from(context),
                R.layout.seed_dialog, null, false);
        if(generate){
            binding.etSeed.setVisibility(View.GONE);
        }
        binding.ivClose.setOnClickListener(v -> {
            if(commonDialog != null){
                commonDialog.closeDialog();
            }
        });
        commonDialog = new CommonDialog.Builder(context)
                .setContentView(binding.getRoot())
                .setButtonWidth(240)
                .setPositiveButton(R.string.common_submit, (dialog, which) -> {
                    String name = ViewUtils.getText(binding.etName);
                    if(generate){
                        dialog.cancel();
                        generateSeed(name);
                    }else{
                        String seed = ViewUtils.getText(binding.etSeed);
                        if(StringUtil.isEmpty(seed)){
                            ToastUtils.showShortToast(R.string.user_seed_empty);
                        }else {
                            dialog.cancel();
                            importSeed(seed, name);
                        }
                    }
                }).create();
        commonDialog.show();
    }

    /**
     * 导入并切换Seed
     */
    void importSeed(String seed, String name) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<String>) emitter -> {
            String result = "";
            try {
                byte[] seedBytes = ByteUtil.toByte(seed);
                Pair<byte[], byte[]> keypair = Ed25519.createKeypair(seedBytes);
                String publicKey = ByteUtil.toHexString(keypair.first);
                logger.debug("import publicKey::{}, size::{}", publicKey, publicKey.length());
                User user = userRepo.getUserByPublicKey(publicKey);
                User currentUser = userRepo.getCurrentUser();
                if(currentUser != null){
                    userRepo.setCurrentUser(currentUser.publicKey, false);
                }
                if(null == user){
                    user = new User(publicKey, seed, name, true);
                    user.lastUpdateTime = DateUtil.getTime();
                    userRepo.addUser(user);
                }else{
                    if(StringUtil.isNotEmpty(name)){
                        user.localName = name;
                    }
                    user.isCurrentUser = true;
                    userRepo.updateUser(user);
                }
            }catch (Exception e){
                result = e.getMessage();
                logger.debug("import seed error::{}", result);
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
    private void generateSeed(String name) {
        byte[] seedBytes = Ed25519.createSeed();
        String seed = ByteUtil.toHexString(seedBytes);
        importSeed(seed, name);
    }

    /**
     * 观察当前用户是否变化
     */
    public MutableLiveData<UserAndMember> getUserDetail() {
        return userDetail;
    }

    /**
     * 观察当前用户是否变化
     */
    public Flowable<User> observeCurrentUser() {
        return userRepo.observeCurrentUser();
    }

    /**
     * 观察Sees历史列表
     */
    Flowable<List<User>> observeSeedHistoryList() {
        return userRepo.observeSeedHistoryList();
    }

    /**
     * 获取切换Seed后结果
     */
    MutableLiveData<String> getChangeResult() {
        return changeResult;
    }

    /**
     * 获取添加联系人的结果
     */
    public MutableLiveData<Boolean> getAddContactResult() {
        return addContactResult;
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

    /**
     * 检查当前用户
     */
    public void checkCurrentUser() {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Boolean>) emitter -> {
            User user = userRepo.getCurrentUser();
            if(null == user){
                logger.info("Create default user");
                generateSeed(null);
            }
            emitter.onNext(true);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
        disposables.add(disposable);
    }

    /**
     * 保存用户名
     */
    public void saveUserName(String name) {
        saveUserName(null, name);
    }

    /**
     * 保存用户名
     */
    private void saveUserName(String publicKey, String name) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Boolean>) emitter -> {
            User user;
            if(StringUtil.isNotEmpty(publicKey)){
                user = userRepo.getUserByPublicKey(publicKey);
            }else{
                user = userRepo.getCurrentUser();
            }
            if(user != null){
                user.localName = name;
                userRepo.updateUser(user);
            }
            emitter.onNext(true);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
        disposables.add(disposable);
    }

    /**
     * 观察不在黑名单的列表中
     */
    public Flowable<List<UserAndMember>> observeUsersNotInBanList() {
        return userRepo.observeUsersNotInBanList();
    }

    /**
     * 添加联系人
     */
    public void addContact(String publicKey) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Boolean>) emitter -> {
            User user = userRepo.getUserByPublicKey(publicKey);
            boolean isExist = true;
            if(null == user){
                user = new User(publicKey);
                userRepo.addUser(user);
                isExist = false;
            }
            emitter.onNext(isExist);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(isExist -> addContactResult.postValue(isExist));
        disposables.add(disposable);
    }

    /**
     * 发送社区邀请链接给朋友
     * @param inviteLink 社区邀请链接
     */
    public void shareInvitedLinkToFriend(String inviteLink, String friendPk) {
        // TODO: 通过msg channel发送
    }

    /**
     * 显示用户信息的对话框
     * @param publicKey 用户公钥
     */
    public void getUserDetail(String publicKey) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<UserAndMember>) emitter -> {
            UserAndMember userAndMember = userRepo.getUserAndMember(publicKey);
            if(null == userAndMember){
                userAndMember = new UserAndMember("");
            }
            emitter.onNext(userAndMember);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(user -> {
                    userDetail.postValue(user);
                });
        disposables.add(disposable);
    }

    /**
     * 显示编辑名字的对话框
     */
    public void showEditNameDialog(AppCompatActivity activity, String publicKey) {
        ContactsDialogBinding binding = DataBindingUtil.inflate(LayoutInflater.from(activity),
                R.layout.contacts_dialog, null, false);
        binding.etPublicKey.setHint(R.string.user_new_name_hint);
        binding.ivClose.setOnClickListener(v -> {
            if (commonDialog != null) {
                commonDialog.closeDialog();
            }
        });
        binding.tvSubmit.setOnClickListener(v -> {
            String newName = StringUtil.getText(binding.etPublicKey);
            if(StringUtil.isNotEmpty(newName)){
                saveUserName(publicKey, newName);
                if (commonDialog != null) {
                    commonDialog.closeDialog();
                }
            }else{
                ToastUtils.showShortToast(R.string.user_invalid_new_name);
            }
        });
        commonDialog = new CommonDialog.Builder(activity)
                .setContentView(binding.getRoot())
                .setButtonWidth(240)
                .create();
        commonDialog.show();
    }

    /**
     * 显示Ban User的对话框
     */
    public void showBanDialog(BaseActivity activity, String publicKey, String showName) {
        if(settingsRepo.doNotShowBanDialog()){
            setUserBlacklist(publicKey, true);
            return;
        }
        BanDialogBinding binding = DataBindingUtil.inflate(LayoutInflater.from(activity),
                R.layout.ban_dialog, null, false);
        binding.tvName.setText(showName);
        binding.ivClose.setOnClickListener(v -> {
            if (commonDialog != null) {
                commonDialog.closeDialog();
            }
        });
        binding.tvDoNotShow.setOnClickListener(v -> {
            if (commonDialog != null) {
                commonDialog.closeDialog();
            }
            settingsRepo.doNotShowBanDialog(true);
        });
        binding.tvSubmit.setOnClickListener(v -> {
            if (commonDialog != null) {
                commonDialog.closeDialog();
            }
            setUserBlacklist(publicKey, true);
        });
        commonDialog = new CommonDialog.Builder(activity)
                .setContentView(binding.getRoot())
                .create();
        commonDialog.show();
    }
}