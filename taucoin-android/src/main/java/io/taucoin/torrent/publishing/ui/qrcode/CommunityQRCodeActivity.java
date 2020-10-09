package io.taucoin.torrent.publishing.ui.qrcode;

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
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.ChainLinkUtil;
import io.taucoin.torrent.publishing.core.utils.SpanUtils;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ActivityQrCodeBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.community.CommunityViewModel;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;

/**
 * 社区QR Code页面
 */
public class CommunityQRCodeActivity extends BaseActivity implements View.OnClickListener {

    private CompositeDisposable disposables = new CompositeDisposable();
    private ActivityQrCodeBinding binding;
    private CommunityViewModel communityViewModel;
    private String chainID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_qr_code);
        binding.setListener(this);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        if(getIntent() != null){
            chainID = getIntent().getStringExtra(IntentExtra.CHAIN_ID);
        }
        if(StringUtil.isEmpty(chainID)){
            return;
        }
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.community_qr_code);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        String showName = UsersUtil.getCommunityName(chainID);
        binding.tvPublicKey.setText(showName);
        binding.roundButton.setText(StringUtil.getFirstLettersOfName(showName));
        binding.roundButton.setBgColor(Utils.getGroupColor(chainID));

        SpannableStringBuilder scanQrCode = new SpanUtils()
                .append(getString(R.string.qr_code_scan_friend_qr))
                .setUnderline()
                .create();
        binding.tvScanQrCode.setText(scanQrCode);

        // 获取10个社区成员的公钥
        disposables.add(communityViewModel.getCommunityMembersLimit(chainID, Constants.CHAIN_LINK_BS_LIMIT)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread()).subscribe(list -> {
                    String communityInviteLink = ChainLinkUtil.encode(chainID, list);
                    Bitmap bitmap = CodeUtils.createQRCode(communityInviteLink, 480);
                    binding.ivQrCode.setImageBitmap(bitmap);
                }));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }

    /**
     * import seed和generate seed点击事件
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_scan_qr_code) {
            onBackPressed();
            ActivityUtil.startActivity( this, ScanQRCodeActivity.class);
        }
    }
}