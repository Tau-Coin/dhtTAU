package io.taucoin.torrent.publishing.ui.user;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.frostwire.jlibtorrent.Ed25519;
import com.google.zxing.Result;
import com.king.zxing.CaptureActivity;
import com.king.zxing.DecodeFormatManager;
import com.king.zxing.camera.FrontLightMode;
import com.king.zxing.util.CodeUtils;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.language.LanguageConfig;

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
import io.taucoin.torrent.publishing.core.utils.GlideEngine;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ZxingUtil;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
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

    @Override
    public int getLayoutId() {
        return R.layout.activity_scan_qr_code;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ActivityUtil.fullScreenAll(this);
        ActivityUtil.setRequestedOrientation(this);
//        ActivityUtil.lockOrientation(this);
        super.onCreate(savedInstanceState);
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
    private void handleScanResult(String publicKey) {
        try {
            if(StringUtil.isNotEmpty(publicKey) &&
                    ByteUtil.toByte(publicKey).length == Ed25519.PUBLIC_KEY_SIZE){
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.PUBLIC_KEY, publicKey);
                ActivityUtil.startActivity(intent, this, UserDetailActivity.class);
                onBackPressed();
                return;
            }
        }catch (Exception ignore){ }
        showNoQrCodeView(true, false);
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
                onBackPressed();
                Intent intent = getIntent();
                if(null != intent){
                    ActivityUtil.startActivity(intent, this, UserQRCodeActivity.class);
                }
                break;
            case R.id.iv_gallery:
                openGallery();
                break;
            case R.id.surfaceView:
                isParseImage = false;
                super.onResume();
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

    /**
     * 显示或隐藏没有QR Code视图
     * @param isError
     * @param noQrCode
     */
    private void showNoQrCodeView(boolean isError, boolean noQrCode) {
        tvNoQrCode.setText(noQrCode ? R.string.qr_code_not_found : R.string.contacts_error_invalid_pk);
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
            String publicKey = "";
            if(StringUtil.isNotEmpty(result)){
                publicKey = result;
            }
            emitter.onNext(publicKey);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(publicKey -> {
                    if (StringUtil.isEmpty(publicKey)) {
                        showNoQrCodeView(true, true);
                    } else {
                        handleScanResult(publicKey);
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