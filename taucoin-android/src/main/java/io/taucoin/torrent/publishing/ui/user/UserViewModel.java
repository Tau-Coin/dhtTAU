package io.taucoin.torrent.publishing.ui.user;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;

import com.frostwire.jlibtorrent.Ed25519;
import com.frostwire.jlibtorrent.Pair;
import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
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
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.FriendStatus;
import io.taucoin.torrent.publishing.core.model.data.Result;
import io.taucoin.torrent.publishing.core.model.data.UserAndFriend;
import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Friend;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.FriendRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.MsgRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.TxRepository;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.AppUtil;
import io.taucoin.torrent.publishing.core.utils.BitmapUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.DimensionsUtil;
import io.taucoin.torrent.publishing.core.utils.FileUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.AddFriendDialogBinding;
import io.taucoin.torrent.publishing.databinding.BanDialogBinding;
import io.taucoin.torrent.publishing.databinding.ContactsDialogBinding;
import io.taucoin.torrent.publishing.databinding.SeedDialogBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.ScanTriggerActivity;
import io.taucoin.torrent.publishing.ui.TauNotifier;
import io.taucoin.torrent.publishing.ui.chat.ChatViewModel;
import io.taucoin.torrent.publishing.ui.constant.Constants;
import io.taucoin.torrent.publishing.ui.constant.KeyQRContent;
import io.taucoin.torrent.publishing.ui.constant.Page;
import io.taucoin.torrent.publishing.ui.constant.QRContent;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.torrent.publishing.ui.main.MainActivity;
import io.taucoin.types.MessageType;
import io.taucoin.util.ByteUtil;

/**
 * 用户相关的ViewModel
 */
public class UserViewModel extends AndroidViewModel {

    private static final Logger logger = LoggerFactory.getLogger("UserViewModel");
    private static final String QR_CODE_NAME = "QRCode%s.jpg";
    private UserRepository userRepo;
    private FriendRepository friendRepo;
    private SettingsRepository settingsRepo;
    private TxRepository txRepo;
    private MsgRepository msgRepo;
    private CompositeDisposable disposables = new CompositeDisposable();
    private MutableLiveData<String> changeResult = new MutableLiveData<>();
    private MutableLiveData<Result> addFriendResult = new MutableLiveData<>();
    private MutableLiveData<Boolean> editNameResult = new MutableLiveData<>();
    private MutableLiveData<List<User>> blackList = new MutableLiveData<>();
    private MutableLiveData<UserAndFriend> userDetail = new MutableLiveData<>();
    private MutableLiveData<QRContent> qrContent = new MutableLiveData<>();
    private MutableLiveData<Bitmap> qrBitmap = new MutableLiveData<>();
    private MutableLiveData<Bitmap> qrBlurBitmap = new MutableLiveData<>();
    private CommonDialog commonDialog;
    private CommonDialog editNameDialog;
    private TauDaemon daemon;
    private UserSourceFactory sourceFactory;
    private ChatViewModel chatViewModel;
    public UserViewModel(@NonNull Application application) {
        super(application);
        userRepo = RepositoryHelper.getUserRepository(getApplication());
        settingsRepo = RepositoryHelper.getSettingsRepository(getApplication());
        txRepo = RepositoryHelper.getTxRepository(getApplication());
        msgRepo = RepositoryHelper.getMsgRepository(getApplication());
        friendRepo = RepositoryHelper.getFriendsRepository(getApplication());
        daemon = TauDaemon.getInstance(application);
        sourceFactory = new UserSourceFactory();
        chatViewModel = new ChatViewModel(application);
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
        if(commonDialog != null){
            commonDialog.closeDialog();
            commonDialog = null;
        }
        if(editNameDialog != null){
            editNameDialog.closeDialog();
            editNameDialog = null;
        }
    }

    /**
     * 显示保存Seed的对话框，也起到确认效果,确认后执行后续操作
     * @param generate：false导入; true:生成新的seed
     */
    public void showSaveSeedDialog(ScanTriggerActivity activity, boolean generate){
        SeedDialogBinding binding = DataBindingUtil.inflate(LayoutInflater.from(activity),
                R.layout.seed_dialog, null, false);
        if(generate){
            binding.etSeed.setVisibility(View.GONE);
            binding.llScanQrCode.setVisibility(View.GONE);
        } else {
            binding.llScanQrCode.setOnClickListener(v -> {
                if(commonDialog != null){
                    commonDialog.closeDialog();
                }
                activity.openScanQRActivity(this);
            });
        }
        binding.ivClose.setOnClickListener(v -> {
            if(commonDialog != null){
                commonDialog.closeDialog();
            }
        });
        binding.tvSubmit.setOnClickListener(v -> {
            String name = ViewUtils.getText(binding.etName);
            if (StringUtil.isNotEmpty(name)) {
                int nicknameLength = Utils.textStringToBytes(name).length;
                if (nicknameLength > Constants.NICKNAME_LENGTH) {
                    ToastUtils.showShortToast(R.string.user_new_name_too_long);
                    return;
                }
            }
            if(generate){
                if(commonDialog != null){
                    commonDialog.closeDialog();
                }
                generateSeed(name);
            }else{
                String seed = ViewUtils.getText(binding.etSeed);
                if(StringUtil.isEmpty(seed)){
                    ToastUtils.showShortToast(R.string.user_seed_empty);
                }else {
                    importSeed(seed, name);
                }
            }
        });
        commonDialog = new CommonDialog.Builder(activity)
                .setContentView(binding.getRoot())
                .create();
        commonDialog.show();
    }

    /**
     * 导入并切换Seed
     */
    public void importSeed(String seed, String name) {
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
                        user.nickname = name;
                        user.updateTime = DateUtil.getTime();
                    }
                    user.seed = seed;
                    user.isCurrentUser = true;
                    userRepo.updateUser(user);
                }
                // 2、把自己当作自己的朋友
                Friend friend = friendRepo.queryFriend(publicKey, publicKey);
                if (null == friend) {
                    friend = new Friend(publicKey, publicKey, FriendStatus.CONNECTED.getStatus());
                    friendRepo.addFriend(friend);
                }
                // 3、更新本地的用户公钥
                MainApplication.getInstance().setCurrentUser(user);
                /* 保证数据不会错乱，必须顺序执行以下逻辑 */
                // 4、更新链端seed
                daemon.updateSeed(seed);
                // 5、关闭所有消息通知
                TauNotifier.getInstance().cancelAllNotify();
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
     * 观察生成的二维码
     */
    public MutableLiveData<Bitmap> getQRBitmap() {
        return qrBitmap;
    }

    /**
     * 观察生成的模糊二维码
     */
    public MutableLiveData<Bitmap> getQRBlurBitmap() {
        return qrBlurBitmap;
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
                String showName = UsersUtil.getShowName(user);
                QRContent content = new QRContent();
                content.setPublicKey(user.publicKey);
                content.setNickName(showName);
                emitter.onNext(content);
            } catch (Exception e) {
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
     * 观察当前用户
     */
    public void queryCurrentUser() {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<QRContent>) emitter -> {
            try {
                User user = userRepo.getCurrentUser();
                String showName = UsersUtil.getShowName(user);
                KeyQRContent content = new KeyQRContent();
                content.setSeed(user.seed);
                content.setPublicKey(user.publicKey);
                content.setNickName(showName);
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
    public MutableLiveData<Result> getAddFriendResult() {
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
                MainApplication.getInstance().setCurrentUser(user);
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
            } else {
                user = userRepo.getCurrentUser();
            }
            if (user != null) {
                if (StringUtil.isNotEmpty(name)) {
                    user.nickname = name;
                    user.updateTime = DateUtil.getTime();
                }
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
     * 根据BuildConfig.DEBUG 添加测试代码，多次调用添加测试朋友
     * @param publicKey
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
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Result>) emitter -> {
            Result result = new Result();
            result.setMsg(publicKey);
            User user = userRepo.getUserByPublicKey(publicKey);
            if(null == user){
                user = new User(publicKey);
                if (StringUtil.isNotEmpty(nickname)) {
                    user.nickname = nickname;
                    user.updateTime = DateUtil.getTime();
                }
                userRepo.addUser(user);
            } else {
                if (StringUtil.isEmpty(user.nickname) && StringUtil.isNotEmpty(nickname)) {
                    user.nickname = nickname;
                    user.updateTime = DateUtil.getTime();
                    userRepo.updateUser(user);
                }
            }
            String userPK = MainApplication.getInstance().getPublicKey();
            Friend friend = friendRepo.queryFriend(userPK, publicKey);
            boolean isExist = true;
            if (null == friend) {
                // 1、添加朋友
                friend = new Friend(userPK, publicKey, FriendStatus.ADDED.getStatus());
                friendRepo.addFriend(friend);

                isExist = false;
                // 2、发送默认消息
                String msg = getApplication().getString(R.string.contacts_have_added);
                chatViewModel.syncSendMessageTask(publicKey, msg, MessageType.TEXT.ordinal());
            }
            result.setSuccess(isExist);
            emitter.onNext(result);
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
            UserAndFriend userAndFriend = userRepo.getFriend(publicKey);
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
            if (editNameDialog != null) {
                editNameDialog.closeDialog();
            }
        });
        binding.tvSubmit.setOnClickListener(v -> {
            String newName = StringUtil.getText(binding.etPublicKey);
            if (StringUtil.isNotEmpty(newName)) {
                int nicknameLength = Utils.textStringToBytes(newName).length;
                if (nicknameLength > Constants.NICKNAME_LENGTH) {
                    ToastUtils.showShortToast(R.string.user_new_name_too_long);
                } else {
                    saveUserName(publicKey, newName);
                    if (editNameDialog != null) {
                        editNameDialog.closeDialog();
                    }
                }
            } else {
                ToastUtils.showShortToast(R.string.user_invalid_new_name);
            }
        });
        editNameDialog = new CommonDialog.Builder(activity)
                .setContentView(binding.getRoot())
                .setCanceledOnTouchOutside(false)
                .setButtonWidth(240)
                .create();
        editNameDialog.show();
    }

    /**
     * 显示添加朋友的对话框
     */
    public void showAddFriendDialog(AppCompatActivity activity) {
        AddFriendDialogBinding binding = DataBindingUtil.inflate(LayoutInflater.from(activity),
                R.layout.add_friend_dialog, null, false);
        binding.ivClose.setOnClickListener(v -> {
            if (commonDialog != null) {
                commonDialog.closeDialog();
            }
        });
        binding.tvSubmit.setOnClickListener(v -> {
            String publicKey = StringUtil.getText(binding.etPublicKey);
            try {
                if (StringUtil.isNotEmpty(publicKey) &&
                        ByteUtil.toByte(publicKey).length == Ed25519.PUBLIC_KEY_SIZE) {
                    String nickName = StringUtil.getText(binding.etNickname);
                    if (StringUtil.isNotEmpty(nickName)) {
                        int nicknameLength = Utils.textStringToBytes(nickName).length;
                        if (nicknameLength > Constants.NICKNAME_LENGTH) {
                            ToastUtils.showShortToast(R.string.user_new_name_too_long);
                            return;
                        }
                    }
                    addFriend(publicKey, nickName);
                    return;
                }
            } catch (Exception ignore) { }
            ToastUtils.showShortToast(R.string.user_invalid_friend_pk);
        });
        commonDialog = new CommonDialog.Builder(activity)
                .setContentView(binding.getRoot())
                .setCanceledOnTouchOutside(false)
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
    public void shareQRCode(AppCompatActivity activity, Drawable drawable, int size) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Boolean>) emitter -> {
            try {
                if (drawable != null) {
                    Bitmap bitmap = BitmapUtil.drawableToBitmap(drawable);
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

                        // 清除旧二维码
                        String filePath = FileUtil.getQRCodeFilePath();
                        File dir = new File(filePath);
                        File[] files = dir.listFiles();
                        for (File file : files) {
                            file.delete();
                        }
                        // 添加时间戳，防止数据分享的还是旧数据
                        String fileName = String.format(QR_CODE_NAME, DateUtil.getDateTime());
                        filePath += fileName;

                        logger.debug("shareQRCode filePath::{}", filePath);

                        FileUtil.saveFilesDirBitmap(filePath, resizeBmp);
                        Context context = MainApplication.getInstance();
                        ActivityUtil.sharePic(activity, filePath, context
                                .getString(R.string.contacts_share_qr_code));
                    }
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
     * 第一次APP启动，提示用户操作：设置昵称和每日流量限制
     * @param activity
     * @param user
     */
    public void promptUserFirstStartApp(AppCompatActivity activity, User user) {
        String firstStartKey = activity.getString(R.string.pref_key_first_start);
        boolean isFirstStart = settingsRepo.getBooleanValue(firstStartKey, true);
        boolean isForeground = AppUtil.isForeground(activity, MainActivity.class.getName());
        // 如果APP是第一次启动, 并且MainActivity也在前台
        if (isFirstStart && isForeground) {
            settingsRepo.setBooleanValue(firstStartKey, false);
            // 如果用户没有nickname
            String showName = UsersUtil.getCurrentUserName(user);
            String defaultName = UsersUtil.getDefaultName(user.publicKey);
            logger.trace("promptUserFirstStart showName::{}, defaultName::{}", showName, defaultName);
            if (StringUtil.isEquals(showName, defaultName)) {
                showEditNameDialog(activity, user.publicKey);
            }
        }
    }

    /**
     * 生成二维码
     * @param context
     * @param qrContent 二维码内容
     */
    public void generateQRCode(Context context, QRContent qrContent) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Bitmap>) emitter -> {
            try {
                String content;
                if (qrContent instanceof KeyQRContent) {
                    content = ((KeyQRContent) qrContent).getSeed();
                } else {
                    content = new Gson().toJson(qrContent);
                }
                int bgColor = Utils.getGroupColor(qrContent.getPublicKey());
                String firstLettersName = UsersUtil.getQRCodeName(qrContent.getNickName());
                Bitmap logoBitmap = BitmapUtil.createLogoBitmap(bgColor, firstLettersName);

                int heightPix = DimensionsUtil.dip2px(context, 480);
                int widthPix = heightPix;
                //Generate the QR Code.
                HashMap<EncodeHintType, Object> hints = new HashMap<>();
                hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
                hints.put(EncodeHintType.MARGIN, 0);
                QRCodeWriter writer = new QRCodeWriter();
                Bitmap bitmap = writer.encode(content, BarcodeFormat.QR_CODE, widthPix, heightPix,
                        hints, logoBitmap, context);
                logger.debug("shareQRCode bitmap::{}", bitmap);
                if (bitmap != null) {
                    emitter.onNext(bitmap);
                }
            } catch (Exception e) {
                logger.error("generateTAUIDQRCode error ", e);
            }
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> qrBitmap.postValue(result));
        disposables.add(disposable);
    }

    /**
     * 把二维码转化为模糊二维码
     * @param bitmap
     */
    public void generateBlurQRCode(Bitmap bitmap) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Bitmap>) emitter -> {
            try {
                Bitmap blurBitmap = BitmapUtil.blurBitmap(bitmap, 80, false);
                emitter.onNext(blurBitmap);
            } catch (Exception e) {
                logger.error("queryCurrentUserAndFriends error ", e);
            }
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> qrBlurBitmap.postValue(result));
        disposables.add(disposable);
    }

    /**
     * 观察朋友信息变化
     */
    public Flowable<Friend> observeFriend(String friendPk) {
        return userRepo.observeFriend(friendPk);
    }

    /**
     * 清除朋友的消息未读状态
     */
    public void clearMsgUnread(String friendPK) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Boolean>) emitter -> {
            try {
                String userPk = MainApplication.getInstance().getPublicKey();
                Friend friend = friendRepo.queryFriend(userPk, friendPK);
                if (friend != null && friend.msgUnread > 0) {
                    friend.msgUnread = 0;
                    friendRepo.updateFriend(friend);
                }
            } catch (Exception e) {
                logger.error("clearMsgUnread error ", e);
            }
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
        disposables.add(disposable);
    }

    public void closeDialog() {
        if (commonDialog != null) {
            commonDialog.closeDialog();
        }
    }
}