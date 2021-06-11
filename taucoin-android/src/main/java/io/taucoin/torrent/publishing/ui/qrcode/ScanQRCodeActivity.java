package io.taucoin.torrent.publishing.ui.qrcode;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.frostwire.jlibtorrent.Ed25519;
import com.google.gson.Gson;
//import com.huawei.hms.hmsscankit.RemoteView;
//import com.huawei.hms.hmsscankit.ScanUtil;
//import com.huawei.hms.ml.scan.HmsScan;
//import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;

import java.util.List;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
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
import io.taucoin.torrent.publishing.ui.BaseActivity;
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
public class ScanQRCodeActivity extends BaseActivity implements View.OnClickListener {

    private CompositeDisposable disposables = new CompositeDisposable();
    private Disposable disposable;
    private TextView tvNoQrCode;
    private CommunityViewModel communityViewModel;
    private UserViewModel userViewModel;
    private String friendPk; // 识别的朋友公钥
    private boolean scanKeyOnly; // 只识别的Key(Seed)
//    private RemoteView remoteView;
    private FrameLayout frameLayout;
    //The width and height of scan_view_finder is both 240 dp.
    private static final int SCAN_FRAME_SIZE = 240;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ActivityUtil.fullScreenAll(this);
        ActivityUtil.setRequestedOrientation(this);
        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //换成AppCompatActivity专用方法
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_scan_qr_code);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        userViewModel = provider.get(UserViewModel.class);
        initView(savedInstanceState);
    }

    /**
     * 初始化布局
     */
    private void initView(Bundle savedInstanceState) {
        if (getIntent() != null) {
            scanKeyOnly = getIntent().getBooleanExtra(IntentExtra.SCAN_KEY_ONLY, false);
        }
        tvNoQrCode = findViewById(R.id.tv_no_qr_code);


        //绑定相机预览布局
        frameLayout = findViewById(R.id.rim);
        //设置扫码识别区域，您可以按照需求调整参数
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float density = dm.density;
        int mScreenWidth = getResources().getDisplayMetrics().widthPixels;
        int mScreenHeight = getResources().getDisplayMetrics().heightPixels;
        int scanFrameSize = (int) (SCAN_FRAME_SIZE * density);
        Rect rect = new Rect();
        rect.left = mScreenWidth / 2 - scanFrameSize / 2;
        rect.right = mScreenWidth / 2 + scanFrameSize / 2;
        rect.top = mScreenHeight / 2 - scanFrameSize / 2;
        rect.bottom = mScreenHeight / 2 + scanFrameSize / 2;
        //初始化RemoteView，并通过如下方法设置参数:setContext()（必选）传入context、setBoundingBox()
        // 设置扫描区域、setFormat()设置识别码制式，设置完毕调用build()方法完成创建。
        // 通过setContinuouslyScan（可选）方法设置非连续扫码模式。
//        remoteView = new RemoteView.Builder()
//                .setContext(this)
//                .setBoundingBox(rect)
//                .setContinuouslyScan(true)
//                .setFormat(HmsScan.QRCODE_SCAN_TYPE)
//                .build();
//        //将自定义view加载到activity
//        remoteView.onCreate(savedInstanceState);
//        //识别结果回调事件订阅
//        remoteView.setOnResultCallback(result -> {
//            //获取到扫码结果HmsScan
//            // Check the result.
//            if (result != null && result.length > 0 && result[0] != null
//                    && !TextUtils.isEmpty(result[0].getOriginalValue())) {
//                remoteView.pauseContinuouslyScan();
//                handleScanResult(result[0].getOriginalValue());
//            }
//        });
//
//        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.MATCH_PARENT);
//        frameLayout.addView(remoteView, params);
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
//        remoteView.resumeContinuouslyScan();
        resumeTextTip();
    }

    private void resumeTextTip() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        } else {
            tvNoQrCode.setText(R.string.contacts_error_invalid_qr);
        }
        disposable = Observable.timer(2, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(aLong -> {
                    tvNoQrCode.setText(R.string.qr_code_scan_qr);
                });
    }
//
//    @Override
//    protected void onStart() {
//        super.onStart();
//        //侦听activity的onStart
//        remoteView.onStart();
//        subscribeAddCommunity();
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        //侦听activity的onResume
//        remoteView.onResume();
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        //侦听activity的onPause
//        remoteView.onPause();
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //侦听activity的onDestroy
//        frameLayout.clearAnimation();
//        frameLayout.removeAllViews();
//        remoteView.setOnResultCallback(null);
//        remoteView.onDestroy();
//        remoteView = null;
        disposables.clear();
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        //侦听activity的onStop
//        remoteView.onStop();
//    }

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
        }
    }

    /**
     * 处理选择的照片
     * @param imgPath
     * */
    private void handleSelectedImage(String imgPath) {
//        Disposable disposable = Flowable.create((FlowableOnSubscribe<String>) emitter -> {
//            String scanResult = "";
//            Bitmap bitmap = BitmapFactory.decodeFile(imgPath);
//            HmsScan[] hmsScans = ScanUtil.decodeWithBitmap(getApplicationContext(), bitmap,
//                    new HmsScanAnalyzerOptions.Creator().setPhotoMode(true).create());
//            if (hmsScans != null && hmsScans.length > 0 && hmsScans[0] != null &&
//                    !TextUtils.isEmpty(hmsScans[0].getOriginalValue())) {
//                scanResult = hmsScans[0].getOriginalValue();
//            }
//            emitter.onNext(scanResult);
//            emitter.onComplete();
//        }, BackpressureStrategy.LATEST)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(result -> {
//                    if (StringUtil.isEmpty(result)) {
//                        resumeTextTip();
//                    } else {
//                        handleScanResult(result);
//                    }
//                });
//        disposables.add(disposable);
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