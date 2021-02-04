package io.taucoin.torrent.publishing.ui.setting;

import android.os.Bundle;

import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.DeviceRepository;
import io.taucoin.torrent.publishing.databinding.ActivityDevicesBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;

/**
 * 用户登录设备页面
 */
public class DevicesActivity extends BaseActivity {

    private ActivityDevicesBinding binding;
    private DevicesAdapter adapter;
    private DeviceRepository deviceRepo;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_devices);
        deviceRepo = RepositoryHelper.getDeviceRepository(this.getApplicationContext());
        initView();
    }
    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.setting_login_devices);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new DevicesAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerView.setLayoutManager(layoutManager);
        // 设置adapter
        binding.recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        String userPk = MainApplication.getInstance().getPublicKey();
        Disposable disposable = deviceRepo.observerDevices(userPk)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(devices -> adapter.submitList(devices));
        disposables.add(disposable);
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }
}