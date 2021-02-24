package io.taucoin.torrent.publishing.ui.qrcode;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.king.zxing.util.CodeUtils;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.CopyManager;
import io.taucoin.torrent.publishing.core.utils.DimensionsUtil;
import io.taucoin.torrent.publishing.core.utils.DrawablesUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ActivityQrCodeBinding;
import io.taucoin.torrent.publishing.ui.ScanTriggerActivity;
import io.taucoin.torrent.publishing.ui.constant.KeyQRContent;
import io.taucoin.torrent.publishing.ui.constant.QRContent;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * 用户Key QR Code页面
 */
public class KeyQRCodeActivity extends ScanTriggerActivity implements View.OnClickListener {

    private ActivityQrCodeBinding binding;
    private UserViewModel userViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_qr_code);
        binding.setListener(this);
        binding.qrCode.setListener(this);
        ViewModelProvider provider = new ViewModelProvider(this);
        userViewModel = provider.get(UserViewModel.class);
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.tvTips.setVisibility(View.GONE);
        binding.llScanQrCode.setVisibility(View.GONE);

        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.key_qr_code_title);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());
        binding.qrCode.tvQrCode.setVisibility(View.GONE);
        binding.qrCode.ivCopy.setVisibility(View.GONE);
        DrawablesUtil.setEndDrawable(binding.qrCode.tvName, R.mipmap.icon_copy_text,
                14);

        // 查询数据
        userViewModel.queryCurrentUser();
        userViewModel.getQRContent().observe(this, this::showQRCOdeImage);
    }

    /**
     * 显示QRCode图片
     */
    private void showQRCOdeImage(QRContent content) {
        KeyQRContent keyContent = (KeyQRContent) content;
        String midHideName = UsersUtil.getMidHideName(keyContent.getSeed());
        String name = getString(R.string.user_show_name);
        name = String.format(name, content.getNickName(), midHideName);
        binding.qrCode.tvName.setText(name);
        binding.qrCode.tvQrCode.setTag(keyContent.getSeed());
        binding.qrCode.roundButton.setText(keyContent.getNickName());
        binding.qrCode.roundButton.setBgColor(Utils.getGroupColor(keyContent.getPublicKey()));
        int heightPix = DimensionsUtil.dip2px(this, 480);
        Bitmap bitmap = CodeUtils.createQRCode(keyContent.getSeed(), heightPix);
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
        if (v.getId() == R.id.tv_name) {
            String key = StringUtil.getTag(binding.qrCode.tvQrCode);
            CopyManager.copyText(key);
            ToastUtils.showShortToast(R.string.copy_seed);
        }
    }
}