package io.taucoin.torrent.publishing.ui.user;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.View;

import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import androidx.databinding.DataBindingUtil;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.SpanUtils;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.core.utils.ZxingUtil;
import io.taucoin.torrent.publishing.databinding.ActivityQrCodeBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;

/**
 * 用户QR Code页面
 */
public class UserQRCodeActivity extends BaseActivity implements View.OnClickListener {

    private ActivityQrCodeBinding binding;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_qr_code);
        binding.setListener(this);
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        if(getIntent() != null){
            user = getIntent().getParcelableExtra(IntentExtra.BEAN);
        }
        if(null == user){
            return;
        }
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.qr_code_title);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());
        binding.tvPublicKey.setText(user.publicKey);
        try {
            BitMatrix encode = ZxingUtil.encode(user.publicKey);
            Bitmap bitmap = ZxingUtil.bitMatrixToBitmap(encode);
            binding.ivQrCode.setImageBitmap(bitmap);
        } catch (WriterException ignore) {
        }

        SpannableStringBuilder scanQrCode = new SpanUtils()
                .append(getString(R.string.qr_code_scan_friend_qr))
                .setUnderline()
                .create();
        binding.tvScanQrCode.setText(scanQrCode);

        String showName = UsersUtil.getShowName(user);
        binding.roundButton.setText(StringUtil.getFirstLettersOfName(showName));
        binding.roundButton.setBgColor(Utils.getGroupColor(user.publicKey));
    }

    /**
     * import seed和generate seed点击事件
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_scan_qr_code) {
            onBackPressed();
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.BEAN, user);
            ActivityUtil.startActivity(intent, this, ScanQRCodeActivity.class);
        }
    }
}