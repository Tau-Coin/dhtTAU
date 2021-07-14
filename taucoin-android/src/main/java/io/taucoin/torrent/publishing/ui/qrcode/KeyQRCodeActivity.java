package io.taucoin.torrent.publishing.ui.qrcode;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.BitmapUtil;
import io.taucoin.torrent.publishing.core.utils.CopyManager;
import io.taucoin.torrent.publishing.core.utils.DrawablesUtil;
import io.taucoin.torrent.publishing.core.utils.SpanUtils;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.databinding.ActivityKeyQrCodeBinding;
import io.taucoin.torrent.publishing.ui.ScanTriggerActivity;
import io.taucoin.torrent.publishing.ui.constant.KeyQRContent;
import io.taucoin.torrent.publishing.ui.constant.QRContent;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * 用户Key QR Code页面
 */
public class KeyQRCodeActivity extends ScanTriggerActivity implements View.OnClickListener {

    private ActivityKeyQrCodeBinding binding;
    private UserViewModel userViewModel;
    private Bitmap QRBitmap = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityUtil.setRequestedOrientation(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_key_qr_code);
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
        binding.qrCode.tvName.setVisibility(View.GONE);
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.key_qr_code_title);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());
        binding.qrCode.tvQrCode.setVisibility(View.GONE);
        binding.qrCode.ivCopy.setVisibility(View.GONE);
        binding.tvKeptSecret.setVisibility(View.GONE);
        DrawablesUtil.setEndDrawable(binding.tvKeyCopy, R.mipmap.icon_copy_text,
                getResources().getDimension(R.dimen.widget_size_16));

        // 查询数据
        userViewModel.queryCurrentUser();
        userViewModel.getQRContent().observe(this, this::showQRCOdeImage);
        userViewModel.getQRBitmap().observe(this, bitmap -> {
            QRBitmap = bitmap;
            userViewModel.generateBlurQRCode(bitmap);
        });
        userViewModel.getQRBlurBitmap().observe(this, bitmap -> {
            if (bitmap != null) {
                binding.qrCode.ivQrCode.setImageBitmap(bitmap);
                binding.tvKeptSecret.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * 显示QRCode图片
     */
    private void showQRCOdeImage(QRContent content) {
        KeyQRContent keyContent = (KeyQRContent) content;
        String midHideName = UsersUtil.getMidHideName(keyContent.getSeed());
        SpannableStringBuilder stringBuilder = new SpanUtils()
                .append(getString(R.string.qr_code_tau_key))
                .setForegroundColor(getResources().getColor(R.color.gray_dark))
                .append(" ")
                .append(midHideName)
                .create();
        binding.tvKeyCopy.setText(stringBuilder);
        binding.qrCode.tvQrCode.setTag(keyContent.getSeed());
        userViewModel.generateQRCode(this, keyContent);
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
            userViewModel.shareQRCode(this, binding.qrCode.ivQrCode.getDrawable(), 240);
        }
        return true;
    }

    /**
     * import seed和generate seed点击事件
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.ll_key_copy) {
            String key = StringUtil.getTag(binding.qrCode.tvQrCode);
            CopyManager.copyText(key);
            ToastUtils.showShortToast(R.string.copy_seed);
        } else if (v.getId() == R.id.tv_kept_secret) {
            if (QRBitmap != null) {
                BitmapUtil.recycleImageView(binding.qrCode.ivQrCode);
                Bitmap bitmap = userViewModel.getQRBlurBitmap().getValue();
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                    userViewModel.getQRBlurBitmap().postValue(null);
                }
                System.gc();
                binding.tvKeptSecret.setVisibility(View.GONE);
                binding.qrCode.ivQrCode.setImageBitmap(QRBitmap);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BitmapUtil.recycleImageView(binding.qrCode.ivQrCode);
        if (QRBitmap != null && !QRBitmap.isRecycled()) {
            QRBitmap.recycle();
            QRBitmap = null;
        }
        System.gc();
    }
}