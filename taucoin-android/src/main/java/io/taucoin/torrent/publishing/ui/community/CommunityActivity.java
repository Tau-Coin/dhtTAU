package io.taucoin.torrent.publishing.ui.community;

import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;

import com.github.naturs.logger.Logger;

import java.lang.reflect.Method;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ActivityCommunityBinding;
import io.taucoin.torrent.publishing.core.storage.entity.Community;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;

/**
 * 单个群组页面
 */
public class CommunityActivity extends BaseActivity {
    private ActivityCommunityBinding binding;

    private MessagesViewModel msgViewModel;
    private CommunityViewModel communityViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private boolean isMute = false;
    private Community community;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        msgViewModel = provider.get(MessagesViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_community);
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
        }
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);

        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    @Override
    public void onStart() {
        super.onStart();
        subscribeCommunityViewModel();
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
        menu.findItem(R.id.menu_mute).setVisible(isMute);
        menu.findItem(R.id.menu_unmute).setVisible(!isMute);
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
                } catch (Exception e) {
                    e.printStackTrace();
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
            case R.id.menu_mute:
                isMute = false;
                communityViewModel.setCommunityMute(community.chainId, false);
                invalidateOptionsMenu();
                break;
            case R.id.menu_unmute:
                isMute = true;
                communityViewModel.setCommunityMute(community.chainId, true);
                invalidateOptionsMenu();
                break;
            case R.id.menu_settings:
                break;
            case R.id.menu_blacklist:
                communityViewModel.setCommunityBlacklist(community.chainId, true);
                break;
        }
        return true;
    }
}