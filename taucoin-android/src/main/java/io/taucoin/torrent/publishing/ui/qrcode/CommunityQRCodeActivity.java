package io.taucoin.torrent.publishing.ui.qrcode;

import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.utils.ChainLinkUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ActivityCommunityQrCodeBinding;
import io.taucoin.torrent.publishing.ui.ScanTriggerActivity;
import io.taucoin.torrent.publishing.ui.community.CommunityViewModel;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;

/**
 * 社区QR Code页面
 */
public class CommunityQRCodeActivity extends ScanTriggerActivity implements View.OnClickListener {

    private CompositeDisposable disposables = new CompositeDisposable();
    private ActivityCommunityQrCodeBinding binding;
    private CommunityViewModel communityViewModel;
    private ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener;
    private String chainID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_community_qr_code);
        binding.setListener(this);
        binding.qrCode.setListener(this);
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
        binding.qrCode.tvName.setText(showName);
        binding.qrCode.roundButton.setText(UsersUtil.getQRCodeName(showName));
        binding.qrCode.roundButton.setBgColor(Utils.getGroupColor(chainID));
        binding.qrCode.tvQrCode.setVisibility(View.GONE);
        binding.qrCode.ivCopy.setVisibility(View.GONE);

        // 获取10个社区成员的公钥
        disposables.add(communityViewModel.getCommunityMembersLimit(chainID, Constants.CHAIN_LINK_BS_LIMIT)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread()).subscribe(list -> {
                    String communityInviteLink = ChainLinkUtil.encode(chainID, list);
                    generateCommunityQRCode(communityInviteLink);
                }));

        communityViewModel.getQRBitmap().observe(this, bitmap -> {
            binding.qrCode.ivQrCode.setImageBitmap(bitmap);
        });
    }

    /**
     *
     * @param communityInviteLink
     */
    private void generateCommunityQRCode(String communityInviteLink) {
        onGlobalLayoutListener = () -> {
            communityViewModel.generateQRCode(this, communityInviteLink,
                    binding.qrCode.flLogo);
        };
        // view重绘时回调
        binding.qrCode.flLogo.getViewTreeObserver().addOnDrawListener(() -> {
            if (onGlobalLayoutListener != null) {
                binding.qrCode.flLogo.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);
            }
        });
        // view加载完成时回调
        binding.qrCode.flLogo.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
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
        if (v.getId() == R.id.ll_scan_qr_code) {
            openScanQRActivityAndExit();
        }
    }
}