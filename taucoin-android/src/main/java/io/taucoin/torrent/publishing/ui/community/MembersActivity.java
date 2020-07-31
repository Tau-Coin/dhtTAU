package io.taucoin.torrent.publishing.ui.community;

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.utils.CopyManager;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.databinding.ActivityMembersBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;

/**
 * 群组成员页面
 */
public class MembersActivity extends BaseActivity implements View.OnClickListener {

    private ActivityMembersBinding binding;
    private CommunityViewModel communityViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private Community community;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_members);
        binding.setListener(this);
        initParameter();
        initLayout();
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if(getIntent() != null){
            community = getIntent().getParcelableExtra(IntentExtra.BEAN);
        }
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        if(community != null){
            String communityName = community.communityName;
            binding.tvGroupName.setText(Html.fromHtml(communityName));
            binding.tvUsersStats.setText(getString(R.string.community_users_stats, 0, 0));
        }
        binding.toolbarInclude.toolbar.setTitle("");
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            binding.toolbarInclude.toolbar.setElevation(0);
        }

        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        if(community != null){
            String inviteLink = UsersUtil.getCommunityInviteLink(MainApplication.getInstance().getPublicKey());
            binding.tvInviteLink.setText(inviteLink);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tv_invite_link:
                String inviteLink = binding.tvInviteLink.getText().toString();
                CopyManager.copyText(inviteLink);
                ToastUtils.showShortToast(R.string.copy_invite_link);
                break;
            case R.id.ll_add_member:
                break;
        }
    }
}