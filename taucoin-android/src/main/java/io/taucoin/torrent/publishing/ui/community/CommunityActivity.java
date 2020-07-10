package io.taucoin.torrent.publishing.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.github.naturs.logger.Logger;

import java.lang.reflect.Method;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.entity.Tx;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ActivityCommunityBinding;
import io.taucoin.torrent.publishing.core.storage.entity.Community;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.PopUpDialog;
import io.taucoin.torrent.publishing.ui.transaction.NicknameActivity;
import io.taucoin.torrent.publishing.ui.transaction.TransactionCreateActivity;
import io.taucoin.torrent.publishing.ui.transaction.TxViewModel;

/**
 * 单个群组页面
 */
public class CommunityActivity extends BaseActivity implements View.OnClickListener, TxListAdapter.ClickListener {
    private ActivityCommunityBinding binding;

    private TxViewModel txViewModel;
    private CommunityViewModel communityViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private Community community;
    private PopUpDialog popUpDialog;
    private TxListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        txViewModel = provider.get(TxViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_community);
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
            String firstLetters = StringUtil.getFirstLettersOfName(communityName);
            binding.toolbarInclude.roundButton.setText(firstLetters);
            binding.toolbarInclude.roundButton.setBgColor(Utils.getGroupColor(firstLetters));
            binding.toolbarInclude.tvGroupName.setText(Html.fromHtml(communityName));
            binding.toolbarInclude.tvUsersStats.setText(getString(R.string.community_users_stats, 0, 0));

            txViewModel.getTxsBychainID(community.chainID, 0);
        }
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);

        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new TxListAdapter(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.txList.setLayoutManager(layoutManager);
        binding.txList.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        subscribeCommunityViewModel();
        subscribeTxViewModel();
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }

    /**
     * 订阅社区相关的被观察者
     */
    private void subscribeCommunityViewModel() {
        communityViewModel.getSetBlacklistState().observe(this, state -> {
            if(state){
                onBackPressed();
            }
        });
    }

    private void subscribeTxViewModel() {
        txViewModel.getChainTxs().observe(this, list->{
            adapter.setDataList(list);
        });
    }

    /**
     *  创建右上角Menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_community, menu);
        return true;
    }

    /**
     *  invalidateOptionsMenu执行后重新控制menu的显示
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        String publicKey = MainApplication.getInstance().getPublicKey();
        boolean isCreator = community != null && StringUtil.equals(community.publicKey, publicKey);
        Logger.d("publicKey=%s", community != null ? community.publicKey:"");
        Logger.d("publicKey=%s", publicKey);
        menu.findItem(R.id.menu_blacklist).setVisible(!isCreator);
        menu.findItem(R.id.menu_settings).setVisible(isCreator);
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     *  重构onMenuOpened方法，通过反射显示icon
     */
    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (menu != null) {
            if (menu.getClass().getSimpleName().equalsIgnoreCase("MenuBuilder")) {
                try {
                    Method method = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                    method.setAccessible(true);
                    method.invoke(menu, true);
                } catch (Exception ignore) {
                }
            }
        }
        return super.onMenuOpened(featureId, menu);
    }
    /**
     * 右上角Menu选项选择事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if( null == community){
            return false;
        }
        switch (item.getItemId()) {
            case R.id.menu_settings:
                ActivityUtil.startActivity(this, CommunitySettingActivity.class);
                break;
            case R.id.menu_blacklist:
                communityViewModel.setCommunityBlacklist(community.chainID, true);
                break;
            case R.id.menu_invite_friends:
                break;
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.iv_link:
                showPopUpDialog();
                break;
        }
    }

    /**
     * 显示底部弹出对话框
     */
    private void showPopUpDialog() {
        String transaction = getString(R.string.community_transactions);
        String nickName = getString(R.string.community_nickname);
        popUpDialog = new PopUpDialog.Builder(this)
                .addItems(transaction, 0)
                .addItems(nickName, 1)
                .setOnItemClickListener((dialog, name, code) -> {
                    dialog.dismiss();
                    if(null == community){
                        return;
                    }
                    Intent intent = new Intent();
                    intent.putExtra(IntentExtra.BEAN, community);
                    switch (code){
                        case 0:
                            ActivityUtil.startActivity(intent, this, TransactionCreateActivity.class);
                            break;
                        case 1:
                            ActivityUtil.startActivity(intent, this, NicknameActivity.class);
                            break;
                    }
                }).create();
        popUpDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(popUpDialog != null){
            popUpDialog.closeDialog();
        }
    }

    @Override
    public void onItemClicked(Tx item) {

    }
}