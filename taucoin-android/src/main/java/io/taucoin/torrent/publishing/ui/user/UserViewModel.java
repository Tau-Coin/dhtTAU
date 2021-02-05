package io.taucoin.torrent.publishing.ui.user;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.BuildConfig;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.UserAndFriend;
import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Friend;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.FriendRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.MsgRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.TxRepository;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.CopyManager;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.FileUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.BanDialogBinding;
import io.taucoin.torrent.publishing.databinding.ContactsDialogBinding;
import io.taucoin.torrent.publishing.databinding.SeedDialogBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.Page;
import io.taucoin.torrent.publishing.ui.constant.QRContent;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.util.ByteUtil;

/**
 * 用户相关的ViewModel
 */
public class UserViewModel extends AndroidViewModel {

    private static final Logger logger = LoggerFactory.getLogger("UserViewModel");
    private static final String QR_CODE_NAME = "QRCode.jpg";
    private UserRepository userRepo;
    private FriendRepository friendRepo;
    private SettingsRepository settingsRepo;
    private TxRepository txRepo;
    private MsgRepository msgRepo;
    private CompositeDisposable disposables = new CompositeDisposable();
    private MutableLiveData<String> changeResult = new MutableLiveData<>();
    private MutableLiveData<Boolean> addFriendResult = new MutableLiveData<>();
    private MutableLiveData<Boolean> editNameResult = new MutableLiveData<>();
    private MutableLiveData<List<User>> blackList = new MutableLiveData<>();
    private MutableLiveData<UserAndFriend> userDetail = new MutableLiveData<>();
    private MutableLiveData<QRContent> qrContent = new MutableLiveData<>();
    private CommonDialog commonDialog;
    private TauDaemon daemon;
    private Disposable observeDaemonRunning;
    private UserSourceFactory sourceFactory;
    public UserViewModel(@NonNull Application application) {
        super(application);
        userRepo = RepositoryHelper.getUserRepository(getApplication());
        settingsRepo = RepositoryHelper.getSettingsRepository(getApplication());
        txRepo = RepositoryHelper.getTxRepository(getApplication());
        msgRepo = RepositoryHelper.getMsgRepository(getApplication());
        friendRepo = RepositoryHelper.getFriendsRepository(getApplication());
        daemon = TauDaemon.getInstance(application);
        sourceFactory = new UserSourceFactory();
    }

    public void observeNeedStartDaemon () {
        disposables.add(daemon.observeNeedStartDaemon()
                .subscribeOn(Schedulers.io())
                .filter((needStart) -> needStart)
                .subscribe((needStart) -> daemon.start()));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
        if(commonDialog != null && commonDialog.isShowing()){
            commonDialog.dismiss();
            commonDialog = null;
        }
        if (observeDaemonRunning != null && !observeDaemonRunning.isDisposed()) {
            observeDaemonRunning.dispose();
        }
    }

    /**
     * 显示保存Seed的对话框，也起到确认效果,确认后执行后续操作
     * @param generate：false导入; true:生成新的seed
     */
    public void showSaveSeedDialog(Context context, boolean generate){
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
                // 1、更新本地数据库数据
                if(null == user){
                    user = new User(publicKey, seed, name, true);
                    userRepo.addUser(user);
                }else{
                    if(StringUtil.isNotEmpty(name)){
                        user.localName = name;
                    }
                    user.seed = seed;
                    user.isCurrentUser = true;
                    userRepo.updateUser(user);
                }
                // 2、更新本地的用户公钥
                MainApplication.getInstance().setPublicKey(publicKey);
                /* 保证数据不会错乱，必须顺序执行以下逻辑 */
                // 3、更新链端seed
                daemon.updateSeed(seed);
            } catch (Exception e){
                result = getApplication().getString(R.string.user_seed_invalid);
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
        if (StringUtil.isEmpty(result) && commonDialog != null && commonDialog.isShowing()) {
            commonDialog.closeDialog();
        }
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
    public MutableLiveData<UserAndFriend> getUserDetail() {
        return userDetail;
    }

    /**
     * 观察当前用户需要生成的QR内容
     */
    public MutableLiveData<QRContent> getQRContent() {
        return qrContent;
    }

    /**
     * 观察当前用户是否变化
     */
    public Flowable<User> observeCurrentUser() {
        return userRepo.observeCurrentUser();
    }

    /**
     * 观察当前用户和其朋友列表
     */
    public void queryCurrentUserAndFriends() {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<QRContent>) emitter -> {
            try {
                User user = userRepo.getCurrentUser();
                List<String> friendPks = friendRepo.queryConnectedFriends(user.publicKey, 10);
                String showName = UsersUtil.getShowName(user);
                QRContent content = new QRContent();
                content.setPublicKey(user.publicKey);
                content.setNickName(showName);
                content.setFriendPks(friendPks);
                emitter.onNext(content);
            }catch (Exception e){
                logger.error("queryCurrentUserAndFriends error ", e);
            }
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> qrContent.postValue(result));
        disposables.add(disposable);
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
    public MutableLiveData<String> getChangeResult() {
        return changeResult;
    }

    /**
     * 获取添加联系人的结果
     */
    public MutableLiveData<Boolean> getAddFriendResult() {
        return addFriendResult;
    }

    /**
     * 获取用户黑名单的被观察者
     * @return 被观察者
     */
    public MutableLiveData<List<User>> getBlackList() {
        return blackList;
    }

    public MutableLiveData<Boolean> getEditNameResult() {
        return editNameResult;
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
                .subscribe(isSuccess -> {
                    submitDataSetChanged();
                });
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
            } else {
                MainApplication.getInstance().setPublicKey(user.publicKey);
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
            if (StringUtil.isNotEmpty(publicKey)) {
                user = userRepo.getUserByPublicKey(publicKey);
                if (StringUtil.isNotEmpty(name)) {
                    user.localName = name;
                }
            } else {
                user = userRepo.getCurrentUser();
                user.localName = name;
            }
            if (user != null) {
                userRepo.updateUser(user);
            }
            emitter.onNext(true);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    submitDataSetChanged();
                    editNameResult.postValue(result);
                });
        disposables.add(disposable);
    }

    /**
     * 在社区页面，刷新数据
     */
    private void submitDataSetChanged(){
        msgRepo.submitDataSetChanged();
        txRepo.submitDataSetChanged();
    }

    /**
     * 查询用户列表
     * @return DataSource
     * @param order 排序字段
     * @param isAll 是否查询所有用户
     * @param friendPk 扫描朋友公钥
     */
    public LiveData<PagedList<UserAndFriend>> observerUsers(int order, boolean isAll, String friendPk) {
        sourceFactory.setParameter(order, isAll, friendPk);
        return new LivePagedListBuilder<>(sourceFactory, Page.getPageListConfig())
                .setInitialLoadKey(Page.PAGE_SIZE)
                .build();
    }

    /**
     * 添加朋友
     */
    public void addFriend(String publicKey) {
        addFriend(publicKey, null);
    }

    /**
     * 添加朋友
     * 根据BuildConfig.DEBUG 添加测试代码，多次调用添加测试朋友
     * @param publicKey
     * @param nickname
     */
    public void addFriend(String publicKey, String nickname) {
        if (!BuildConfig.DEBUG && observeDaemonRunning != null
                && !observeDaemonRunning.isDisposed()) {
            return;
        }
        Disposable disposable = daemon.observeDaemonRunning()
                .subscribeOn(Schedulers.io())
                .subscribe((isRunning) -> {
                    if (isRunning) {
                        addFriendTask(publicKey, nickname);
                        if (observeDaemonRunning != null) {
                            observeDaemonRunning.dispose();
                        }
                    }
                });
        if (!BuildConfig.DEBUG) {
            observeDaemonRunning = disposable;
        }
    }

    private void addFriendTask(String publicKey, String nickname) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Boolean>) emitter -> {
            User user = userRepo.getUserByPublicKey(publicKey);
            if(null == user){
                user = new User(publicKey, nickname);
                userRepo.addUser(user);
            } else {
                if (StringUtil.isNotEmpty(nickname)) {
                    user.localName = nickname;
                    userRepo.updateUser(user);
                }
            }
            String userPK = MainApplication.getInstance().getPublicKey();
            Friend friend = friendRepo.queryFriend(userPK, publicKey);
            boolean isExist = true;
            if (null == friend) {
                friend = new Friend(userPK, publicKey, 1);
                friendRepo.addFriend(friend);
                isExist = false;
                daemon.addNewFriend(ByteUtil.toByte(publicKey));
            }
            emitter.onNext(isExist);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(isExist -> addFriendResult.postValue(isExist));
        disposables.add(disposable);
    }

    /**
     * 显示用户信息的对话框
     * @param publicKey 用户公钥
     */
    public void getUserDetail(String publicKey) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<UserAndFriend>) emitter -> {
            String userPK = MainApplication.getInstance().getPublicKey();
            UserAndFriend userAndFriend = userRepo.getUserAndFriend(userPK, publicKey);
            if(null == userAndFriend){
                userAndFriend = new UserAndFriend(publicKey);
            }
            emitter.onNext(userAndFriend);
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

    /**
     * 分享QRCOde
     */
    public void shareQRCode(AppCompatActivity activity, View view, int size) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Boolean>) emitter -> {
            try {
                view.setDrawingCacheEnabled(true);
                view.buildDrawingCache();
                Bitmap bitmap = view.getDrawingCache();
                logger.debug("shareQRCode bitmap::{}", bitmap);
                if (bitmap != null) {
                    Bitmap.createBitmap(bitmap);
                    int w = bitmap.getWidth();
                    int h = bitmap.getHeight();
                    float sx = (float) size / w;
                    float sy = (float) size / h;
                    Matrix matrix = new Matrix();
                    matrix.postScale(sx, sy);
                    Bitmap resizeBmp = Bitmap.createBitmap(bitmap, 0, 0,
                            w, h, matrix, true);
                    view.destroyDrawingCache();
                    String fileName = FileUtil.getQRCodeFilePath() + QR_CODE_NAME;

                    logger.debug("shareQRCode fileName::{}", fileName);

                        FileUtil.saveFilesDirBitmap(fileName, resizeBmp);
                        ActivityUtil.shareFile(activity, fileName, view.getContext()
                                .getString(R.string.contacts_share_qr_code));
                }
            } catch (Exception e) {
                logger.error("shareQRCode error", e);
            }
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
        disposables.add(disposable);
    }

    /**
     * 到处Seed
     */
    void exportSeed() {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<String>) emitter -> {
            try {
                User currentUser = userRepo.getCurrentUser();
                emitter.onNext(currentUser.seed);
            } catch (Exception e) {
                logger.error("exportSeed error", e);
                emitter.onNext("");
            }
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(seed -> {
                    if (StringUtil.isNotEmpty(seed)) {
                        CopyManager.copyText(seed);
                        ToastUtils.showShortToast(R.string.copy_seed);
                    }
                });
        disposables.add(disposable);
    }
}