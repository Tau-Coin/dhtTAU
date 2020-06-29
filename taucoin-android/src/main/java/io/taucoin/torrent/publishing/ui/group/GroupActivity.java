package io.taucoin.torrent.publishing.ui.group;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.lang.reflect.Method;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ActivityGroupBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.main.CommunityItem;

/**
 * 单个群组页面
 */
public class GroupActivity extends BaseActivity {
    private ActivityGroupBinding binding;
    private ActionBarDrawerToggle toggle;

    private MessagesViewModel viewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private boolean isMute = false;
    private CommunityItem item;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(MessagesViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_group);
        initParameter();
        initLayout();
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if(getIntent() != null){
            item = getIntent().getParcelableExtra(IntentExtra.BEAN);
        }
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        if(item != null){
            String groupName = item.getCommunityName();
            String firstLetters = StringUtil.getFirstLettersOfName(groupName);
            binding.toolbarInclude.roundButton.setText(firstLetters);
            binding.toolbarInclude.roundButton.setBgColor(Utils.getGroupColor(firstLetters));
            binding.toolbarInclude.tvGroupName.setText(groupName);
            binding.toolbarInclude.tvUsersStats.setText(getString(R.string.group_users_stats, 0, 0));
        }
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);

        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());
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

    /**
     *  创建右上角Menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_group, menu);
        return true;
    }

    /**
     *  invalidateOptionsMenu执行后重新控制menu的显示
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
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
        switch (item.getItemId()) {
            case R.id.menu_mute:
                isMute = false;
                invalidateOptionsMenu();
                break;
            case R.id.menu_unmute:
                isMute = true;
                invalidateOptionsMenu();
                break;
            case R.id.menu_block_group:
                break;
        }
        return true;
    }
}