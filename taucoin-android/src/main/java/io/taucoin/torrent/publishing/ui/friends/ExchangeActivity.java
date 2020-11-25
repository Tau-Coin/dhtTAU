package io.taucoin.torrent.publishing.ui.friends;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.View;

import com.king.zxing.util.CodeUtils;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.SpanUtils;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ActivityExchangeBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.qrcode.ScanQRCodeActivity;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * 和朋友交换二维码页面
 */
public class ExchangeActivity extends BaseActivity implements View.OnClickListener {

    private CompositeDisposable disposables = new CompositeDisposable();
    private ActivityExchangeBinding binding;
    private UserViewModel userViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_exchange);
        binding.setListener(this);
        ViewModelProvider provider = new ViewModelProvider(this);
        userViewModel = provider.get(UserViewModel.class);
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        String publicKey = MainApplication.getInstance().getPublicKey();
        if(StringUtil.isEmpty(publicKey)){
            return;
        }
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.contacts_exchange_qr);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());
        binding.roundButton.setBgColor(Utils.getGroupColor(publicKey));

        Bitmap bitmap = CodeUtils.createQRCode(publicKey, 480);
        binding.ivQrCode.setImageBitmap(bitmap);

        SpannableStringBuilder stepOne = new SpanUtils()
                .append(getString(R.string.qr_code_step_one_point))
                .append(getString(R.string.qr_code_step_one))
                .setUnderline()
                .create();
        binding.tvStepOne.setText(stepOne);

        SpannableStringBuilder stepTwo = new SpanUtils()
                .append(getString(R.string.qr_code_step_one_point))
                .append(getString(R.string.qr_code_step_two))
                .setUnderline()
                .create();
        binding.tvStepTwo.setText(stepTwo);

        disposables.add(userViewModel.observeCurrentUser()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(user -> {
                    String showName = UsersUtil.getShowName(user);
                    binding.roundButton.setText(StringUtil.getFirstLettersOfName(showName));
                }));
    }

    /**
     * 分享二维码
     */
    private void shareQRCode() {
        userViewModel.shareQRCode(this, binding.rlQrCode, 240);
    }

    /**
     * import seed和generate seed点击事件
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_step_one:
                shareQRCode();
                break;
            case R.id.tv_step_two:
                ActivityUtil.startActivity(this, ScanQRCodeActivity.class);
                break;
            default:
                break;
        }
    }
}