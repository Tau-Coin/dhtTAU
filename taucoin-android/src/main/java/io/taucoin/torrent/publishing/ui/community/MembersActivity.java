package io.taucoin.torrent.publishing.ui.community;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.MemberAndUser;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.databinding.ActivityMembersBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.contacts.ContactsActivity;

/**
 * 群组成员页面
 */
public class MembersActivity extends BaseActivity implements View.OnClickListener, MemberListAdapter.ClickListener {

    private ActivityMembersBinding binding;
    private CommunityViewModel communityViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private MemberListAdapter adapter;
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

        adapter = new MemberListAdapter(this);
//        /*
//         * A RecyclerView by default creates another copy of the ViewHolder in order to
//         * fade the views into each other. This causes the problem because the old ViewHolder gets
//         * the payload but then the new one doesn't. So needs to explicitly tell it to reuse the old one.
//         */
        DefaultItemAnimator animator = new DefaultItemAnimator() {
            @Override
            public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
                return true;
            }
        };
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.memberList.setLayoutManager(layoutManager);
        binding.memberList.setItemAnimator(animator);
        binding.memberList.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        communityViewModel.getSetBlacklistState().observe(this, isSuccess -> {
            if(isSuccess){
                this.setResult(RESULT_OK);
                this.finish();
            }
        });
        if(null == community){
            return;
        }
        disposables.add(communityViewModel.observeCommunityMembers(community.chainID)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(members -> {
                adapter.setDataList(members);
            }));
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }

    @Override
    public void onClick(View v) {
        if(null == community){
            return;
        }
        switch (v.getId()){
            case R.id.ll_ban:
                communityViewModel.setCommunityBlacklist(community.chainID, true);
                break;
            case R.id.ll_add_member:
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.TYPE, ContactsActivity.TYPE_ADD_MEMBERS);
                intent.putExtra(IntentExtra.CHAIN_ID, community.chainID);
                ActivityUtil.startActivity(intent, this, ContactsActivity.class);
                break;
        }
    }

    @Override
    public void onItemClicked(MemberAndUser item) {

    }
}