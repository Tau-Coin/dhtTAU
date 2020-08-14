package io.taucoin.torrent.publishing.ui.user;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.frostwire.jlibtorrent.Ed25519;
import com.google.zxing.Result;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.language.LanguageConfig;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
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
import io.taucoin.torrent.publishing.core.utils.DimensionsUtil;
import io.taucoin.torrent.publishing.core.utils.GlideEngine;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.ZxingUtil;
import io.taucoin.torrent.publishing.databinding.ActivityScanQrCodeBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.util.ByteUtil;

/**
 * 用户QR Code页面
 */
public class ScanQRCodeActivity extends BaseActivity implements View.OnClickListener {

    private ActivityScanQrCodeBinding binding;
    private CaptureManager capture;
    private UserViewModel userViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    // 打开相册返回后，是否恢复扫描
    private boolean isResume = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                getWindow().setStatusBarColor(getResources().getColor(R.color.primary_dark));
//                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
//        }
        ViewModelProvider provider = new ViewModelProvider(this);
        userViewModel = provider.get(UserViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_scan_qr_code);
        binding.setListener(this);
        initCapture(savedInstanceState);
        initView();
    }

    /**
     * 初始化捕捉
     */
    private void initCapture(Bundle savedInstanceState){
        if(capture != null){
            capture.onDestroy();
            capture = null;
        }
        capture = new CaptureManager(this, binding.bvBarcode){
            @Override
            protected void returnResult(BarcodeResult rawResult) {
                handleScanResult(rawResult.toString(), savedInstanceState);
            }
        };
        capture.initializeFromIntent(getIntent(), savedInstanceState);
        capture.decode();
    }

    /**
     * 处理扫描结果
     */
    private void handleScanResult(String publicKey, Bundle savedInstanceState) {
        if(StringUtil.isNotEmpty(publicKey) &&
                ByteUtil.toByte(publicKey).length == Ed25519.PUBLIC_KEY_SIZE){
            userViewModel.showUserInfoDialog(ScanQRCodeActivity.this, publicKey);
        }else{
            if(savedInstanceState != null){
                initCapture(savedInstanceState);
            }
            capture.onResume();
            ToastUtils.showShortToast(R.string.contacts_error_invalid_pk);
        }
    }

    /**
     * 初始化布局
     */
    private void initView() {
        TextView statusView = binding.bvBarcode.getStatusView();
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) statusView.getLayoutParams();
        layoutParams.bottomMargin = DimensionsUtil.dip2px(this, 20);
        statusView.setLayoutParams(layoutParams);
        statusView.setText(R.string.qr_code_scan_qr);
    }

    /**
     * import seed和generate seed点击事件
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.iv_back:
                onBackPressed();
                break;
            case R.id.iv_qr_code:
                onBackPressed();
                Intent intent = getIntent();
                if(null != intent){
                    ActivityUtil.startActivity(intent, this, UserQRCodeActivity.class);
                }
                break;
            case R.id.iv_gallery:
                isResume = false;
                openGallery();
                break;
            case R.id.ll_no_qr_code:
                isResume = true;
                capture.onResume();
                showNoQrCodeView(false);
                break;
        }

    }

    /**
     * 打开相册
     */
    private void openGallery(){
        PictureSelector.create(this)
                .openGallery(PictureMimeType.ofImage())
                .setLanguage(LanguageConfig.ENGLISH)
                .selectionMode(PictureConfig.SINGLE)
                .isSingleDirectReturn(true)
                .isCamera(true)
                .compress(false)
                .loadImageEngine(GlideEngine.createGlideEngine())
                .forResult(PictureConfig.CHOOSE_REQUEST);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isResume){
            capture.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        capture.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        capture.onDestroy();
        disposables.clear();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        capture.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        capture.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return binding.bvBarcode.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
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
                            handleSelectedImage(localMedia.getOriginalPath());
                            return;
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        capture.onResume();
    }

    /**
     * 处理选择的照片
     * @param originalPath
     */
    private void handleSelectedImage(String originalPath) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Result>) emitter -> {
            Result result = ZxingUtil.scanningImage(originalPath);
            emitter.onNext(result);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (null == result) {
                        showNoQrCodeView(true);
                    } else {
                        handleScanResult(result.toString(), null);
                    }
                });
        disposables.add(disposable);
    }

    /**
     * 显示或隐藏没有QR Code视图
     * @param noQrCode
     */
    private void showNoQrCodeView(boolean noQrCode) {
        binding.llNoQrCode.setVisibility(noQrCode ? View.VISIBLE : View.GONE);
        binding.ivQrCode.setVisibility(noQrCode ? View.INVISIBLE : View.VISIBLE);
        binding.ivGallery.setVisibility(noQrCode ? View.INVISIBLE : View.VISIBLE);
    }
}