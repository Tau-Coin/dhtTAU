package io.taucoin.torrent.publishing.ui.notify;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.NotificationAndUser;
import io.taucoin.torrent.publishing.databinding.ActivityNotificationsBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.community.CommunityViewModel;

/**
 * 通知页面
 */
public class NotificationActivity extends BaseActivity implements NotificationAdapter.ClickListener {
    private ActivityNotificationsBinding binding;
    private CommunityViewModel communityViewModel;
    private NotificationViewModel notifyViewModel;
    private NotificationAdapter adapter;
    private CompositeDisposable disposables = new CompositeDisposable();
    private boolean isEdit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_notifications);
        ViewModelProvider provider = new ViewModelProvider(this);
        notifyViewModel = provider.get(NotificationViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.notifications_title);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new NotificationAdapter(this, isEdit);
        /*
         * A RecyclerView by default creates another copy of the ViewHolder in order to
         * fade the views into each other. This causes the problem because the old ViewHolder gets
         * the payload but then the new one doesn't. So needs to explicitly tell it to reuse the old one.
         */
        DefaultItemAnimator animator = new DefaultItemAnimator() {
            @Override
            public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
                return true;
            }
        };
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerList.setLayoutManager(layoutManager);
        binding.recyclerList.setItemAnimator(animator);
        binding.recyclerList.setEmptyView(binding.emptyViewList);
        binding.recyclerList.setAdapter(adapter);

        notifyViewModel.observerNotifications().observe(this, list -> {
            adapter.submitList(list);
            notifyViewModel.readAllNotifications();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        notifyViewModel.getDeleteState().observe(this, result -> {
            if(result.isSuccess()){
                adapter.getSelectedList().clear();
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        disposables.clear();
    }

    /**
     * 创建右上角Menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_notification, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuDone = menu.findItem(R.id.menu_done);
        MenuItem menuEdit = menu.findItem(R.id.menu_edit);
        MenuItem menuDelete = menu.findItem(R.id.menu_delete);
        menuDone.setVisible(isEdit);
        menuDelete.setVisible(isEdit);
        menuEdit.setVisible(!isEdit);
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * 右上角Menu选项选择事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_done) {
            isEdit = false;
            invalidateOptionsMenu();
            adapter.setEdit(isEdit);
        } else if (item.getItemId() == R.id.menu_edit) {
            isEdit = true;
            invalidateOptionsMenu();
            adapter.setEdit(isEdit);
        } else if (item.getItemId() == R.id.menu_delete) {
            if(adapter.getSelectedList().size() > 0){
                notifyViewModel.deleteNotifications(adapter.getSelectedList());
            }
        }
        return true;
    }

    @Override
    public void onJoinClicked(NotificationAndUser notify) {
        communityViewModel.addCommunity(notify.chainID, notify.chainLink);
    }
}