package io.taucoin.torrent.publishing.ui.qrcode;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.frostwire.jlibtorrent.Ed25519;
import com.google.gson.Gson;
import com.king.zxing.CaptureActivity;
import com.king.zxing.DecodeFormatManager;
import com.king.zxing.camera.FrontLightMode;
import com.king.zxing.util.CodeUtils;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.ChainLinkUtil;
import io.taucoin.torrent.publishing.core.utils.MediaUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.ui.community.CommunityViewModel;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.constant.QRContent;
import io.taucoin.torrent.publishing.ui.friends.FriendsActivity;
import io.taucoin.torrent.publishing.ui.main.MainActivity;
import io.taucoin.torrent.publishing.ui.user.SeedActivity;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;
import io.taucoin.util.ByteUtil;

/**
 * 用户QR Code扫描页面
 */
public class ScanQRCodeActivity extends CaptureActivity implements View.OnClickListener {

    private CompositeDisposable disposables = new CompositeDisposable();
    // 是否连续扫码
    private boolean isContinuousScan = true;
    private boolean isParseImage = false;
    private ImageView ivQrCode;
    private ImageView ivGallery;
    private TextView tvNoQrCode;
    private TextView tvContinue;
    private CommunityViewModel communityViewModel;
    private UserViewModel userViewModel;
    private String friendPk; // 识别的朋友公钥
    private boolean scanKeyOnly; // 只识别的Key(Seed)

    @Override
    public int getLayoutId() {
        return R.layout.activity_scan_qr_code;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ActivityUtil.fullScreenAll(this);
        ActivityUtil.setRequestedOrientation(this);
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        userViewModel = provider.get(UserViewModel.class);
        initView();
        //获取CaptureHelper，里面有扫码相关的配置设置
        getCaptureHelper().playBeep(false)// 播放音效
                .vibrate(false)//震动
                .supportVerticalCode(true)// 支持扫垂直条码，建议有此需求时才使用。
                .decodeFormats(DecodeFormatManager.QR_CODE_FORMATS)// 设置只识别二维码会提升速度
                .frontLightMode(FrontLightMode.OFF)// 设置闪光灯模式
                .tooDarkLux(45f)// 设置光线太暗时，自动触发开启闪光灯的照度值
                .brightEnoughLux(100f)// 设置光线足够明亮时，自动触发关闭闪光灯的照度值
                .continuousScan(isContinuousScan)// 是否连扫
                .supportLuminanceInvert(true);// 是否支持识别反色码（黑白反色的码），增加识别率
    }

    /**
     * 初始化布局
     */
    private void initView() {
        if (getIntent() != null) {
            scanKeyOnly = getIntent().getBooleanExtra(IntentExtra.SCAN_KEY_ONLY, false);
        }
        ivQrCode = findViewById(R.id.iv_qr_code);
        ivGallery = findViewById(R.id.iv_gallery);
        tvNoQrCode = findViewById(R.id.tv_no_qr_code);
        tvContinue = findViewById(R.id.tv_continue);
    }

    /**
     * 扫码结果回调
     * @param result 扫码结果
     * @return
     */
    @Override
    public boolean onResultCallback(String result) {
        if(isContinuousScan && !isParseImage){
            super.onPause();
            handleScanResult(result);
        }
        return super.onResultCallback(result);
    }

    /**
     * 处理扫描结果
     */
    private void handleScanResult(String scanResult) {
        try {
            if (StringUtil.isNotEmpty(scanResult)) {
                ChainLinkUtil.ChainLink decode = ChainLinkUtil.decode(scanResult);
                if (decode.isValid()) {
                    String chainID = decode.getDn();
                    openChainLink(chainID, scanResult);
                    return;
                }
                if (Utils.isKeyValid(scanResult)) {
                    if (scanKeyOnly) {
                        // 返回上一个页面扫描结果
                        Intent intent = new Intent();
                        intent.putExtra(IntentExtra.DATA, scanResult);
                        setResult(RESULT_OK, intent);
                        onBackPressed();
                    } else {
                        // 直接导入扫描结果
                        userViewModel.importSeed(scanResult, null);
                    }
                    return;
                }
                QRContent content = new Gson().fromJson(scanResult, QRContent.class);
                if (content != null &&
                        ByteUtil.toByte(content.getPublicKey()).length == Ed25519.PUBLIC_KEY_SIZE) {
                    friendPk = content.getPublicKey();
                    userViewModel.addFriend(content.getPublicKey(), content.getNickName());
                    return;
                }
            }
        } catch (Exception ignore){ }
        showNoQrCodeView(true, false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        subscribeAddCommunity();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }

    private void subscribeAddCommunity(){
        communityViewModel.getAddCommunityState().observe(this, result -> {
            if(result.isSuccess()){
                openCommunityActivity(result.getMsg());
            }
        });

        userViewModel.getAddFriendResult().observe(this, result -> {
            openFriendActivity();
        });

        userViewModel.getChangeResult().observe(this, result -> {
            if(StringUtil.isNotEmpty(result)){
                ToastUtils.showShortToast(result);
            }
            openSeedActivity();
        });
    }

    /**
     * 打开Seed页面
     */
    private void openSeedActivity() {
        ActivityUtil.startActivity(this, SeedActivity.class);
        onBackPressed();
    }

    /**
     * 打开朋友列表页面
     */
    private void openFriendActivity() {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(IntentExtra.PUBLIC_KEY, friendPk);
        ActivityUtil.startActivity(intent, this, FriendsActivity.class);
        onBackPressed();
    }

    /**
     * 打开社区页面
     */
    private void openCommunityActivity(String chainID) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.CHAIN_ID, chainID);
        ActivityUtil.startActivity(intent, this, MainActivity.class);
        onBackPressed();
    }

    /**
     * 打开chain link
     * @param chainID
     */
    private void openChainLink(String chainID, String chainLink) {
        disposables.add(communityViewModel.getCommunityByChainIDSingle(chainID)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(community -> {
                    openCommunityActivity(chainID);
                }, it -> {
                    communityViewModel.addCommunity(chainID, chainLink);
                }));
    }

    /**
     * 视图点击事件
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.iv_back:
                onBackPressed();
                break;
            case R.id.iv_qr_code:
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.TYPE, UserQRCodeActivity.TYPE_QR_SHARE);
                ActivityUtil.startActivity(intent, this, UserQRCodeActivity.class);
                break;
            case R.id.iv_gallery:
                MediaUtil.startOpenGallery(this);
                break;
            case R.id.surfaceView:
                refreshScanView();
                break;
        }
    }

    /**
     * 刷新扫描试图
     */
    private void refreshScanView() {
        isParseImage = false;
        super.onResume();
        showNoQrCodeView(false);
    }

    /**
     * 显示或隐藏没有QR Code视图
     * @param isError
     * @param noQrCode
     */
    private void showNoQrCodeView(boolean isError, boolean noQrCode) {
        tvNoQrCode.setText(noQrCode ? R.string.qr_code_not_found : R.string.contacts_error_invalid_qr);
        tvContinue.setVisibility(View.VISIBLE);
        showNoQrCodeView(isError);
    }

    /**
     * 显示或隐藏没有QR Code视图
     * @param isError
     */
    private void showNoQrCodeView(boolean isError) {
        if(!isError){
            tvNoQrCode.setText(R.string.qr_code_scan_qr);
            tvContinue.setVisibility(View.GONE);
        }
        ivQrCode.setVisibility(isError ? View.INVISIBLE : View.VISIBLE);
        ivGallery.setVisibility(isError ? View.INVISIBLE : View.VISIBLE);
    }

    /**
     * 处理选择的照片
     * @param imgPath
     * */
    private void handleSelectedImage(String imgPath) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<String>) emitter -> {
            String result = CodeUtils.parseQRCode(imgPath);
            String scanResult = "";
            if(StringUtil.isNotEmpty(result)){
                scanResult = result;
            }
            emitter.onNext(scanResult);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (StringUtil.isEmpty(result)) {
                        showNoQrCodeView(true, true);
                    } else {
                        handleScanResult(result);
                    }
                });
        disposables.add(disposable);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode){
                case PictureConfig.CHOOSE_REQUEST:
                case PictureConfig.REQUEST_CAMERA:
                    List<LocalMedia> selectList = PictureSelector.obtainMultipleResult(data);
                    if(selectList != null && selectList.size() == 1){
                        LocalMedia localMedia = selectList.get(0);
                        if(PictureMimeType.eqImage(localMedia.getMimeType())){
                            isParseImage = true;
                            handleSelectedImage(localMedia.getPath());
                            return;
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }
}