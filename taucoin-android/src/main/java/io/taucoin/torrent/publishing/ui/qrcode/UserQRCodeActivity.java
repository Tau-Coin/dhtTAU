package io.taucoin.torrent.publishing.ui.qrcode;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.google.gson.Gson;
import com.king.zxing.util.CodeUtils;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.DimensionsUtil;
import io.taucoin.torrent.publishing.core.utils.SpanUtils;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ActivityQrCodeBinding;
import io.taucoin.torrent.publishing.ui.ScanTriggerActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.constant.QRContent;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * 用户QR Code页面
 */
public class UserQRCodeActivity extends ScanTriggerActivity implements View.OnClickListener {

    public static final int TYPE_QR_DISPLAY = 0x01;
    public static final int TYPE_QR_SHARE = 0x02;
    public static final int TYPE_QR_SHARE_ADDED = 0x03;
    private ActivityQrCodeBinding binding;
    private UserViewModel userViewModel;
    private int type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_qr_code);
        binding.setListener(this);
        ViewModelProvider provider = new ViewModelProvider(this);
        userViewModel = provider.get(UserViewModel.class);
        initParameter();
        initView();
    }

    private void initParameter() {
        type = getIntent().getIntExtra(IntentExtra.TYPE, TYPE_QR_DISPLAY);
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.qrCode.tvQrCode.setVisibility(type == TYPE_QR_SHARE_ADDED ? View.VISIBLE : View.GONE);
        binding.qrCode.tvQrCode.setText(R.string.user_share_qr_dialog_tips);
        if (type == TYPE_QR_SHARE_ADDED) {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams)
                    binding.qrCode.rlQrCode.getLayoutParams();
            layoutParams.topMargin = 0;
            binding.qrCode.rlQrCode.setLayoutParams(layoutParams);
        }
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.qr_code_title);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        SpannableStringBuilder scanQrCode = new SpanUtils()
                .append(getString(R.string.qr_code_scan_friend_qr))
                .setUnderline()
                .create();
        binding.tvScanQrCode.setText(scanQrCode);
        binding.tvScanQrCode.setVisibility(type == TYPE_QR_DISPLAY ? View.VISIBLE : View.GONE);

        // 查询数据
        userViewModel.queryCurrentUserAndFriends();
        userViewModel.getQRContent().observe(this, this::showQRCOdeImage);
    }

    /**
     * 显示QRCode图片
     */
    private void showQRCOdeImage(QRContent content) {
        binding.qrCode.roundButton.setText(content.getNickName());
        binding.qrCode.roundButton.setBgColor(Utils.getGroupColor(content.getPublicKey()));
        String contentJson = new Gson().toJson(content);
        Bitmap bitmap = CodeUtils.createQRCode(contentJson, 480);
        binding.qrCode.ivQrCode.setImageBitmap(bitmap);
    }

    /**
     *  创建右上角Menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_qr_code, menu);
        return true;
    }

    /**
     * 右上角Menu选项选择事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_share) {
            userViewModel.shareQRCode(this, binding.qrCode.rlQrCode, 240);
        }
        return true;
    }

    /**
     * import seed和generate seed点击事件
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_scan_qr_code) {
            openScanQRActivityAndExit();
        }
    }
}