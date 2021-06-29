package io.taucoin.torrent.publishing.ui.setting;

import android.os.Bundle;
import android.os.FileObserver;
import android.view.View;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.log.LogConfigurator;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.FileUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.databinding.ActivityJournalBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;

/**
 * 日志页面
 */
public class JournalActivity extends BaseActivity implements View.OnClickListener,
    JournalAdapter.ClickListener{

    private static final Logger logger = LoggerFactory.getLogger("JournalActivity");
    private ActivityJournalBinding binding;
    private JournalAdapter adapter;
    private JournalFileObserver fileObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_journal);
        binding.setListener(this);
        initView();
        loadJournalData();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.setting_journal);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        String path = LogConfigurator.getLogDir() + File.separator;
        fileObserver = new JournalFileObserver(path);
        binding.tvJournalDirectory.setText(path);
        binding.tvShareAll.setVisibility(View.GONE);

        adapter = new JournalAdapter(this);
        DefaultItemAnimator animator = new DefaultItemAnimator() {
            @Override
            public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
                return true;
            }
        };
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setItemAnimator(animator);
        binding.recyclerView.setAdapter(adapter);

    }

    /**
     * 加载日志数据
     */
    private void loadJournalData(){
        String path = StringUtil.getText(binding.tvJournalDirectory);
        List<File> fileList = FileUtil.getFiles(path);
        Collections.sort(fileList, (o1, o2) -> {
            String fileName1 = o1.getName();
            String fileName2 = o2.getName();
            if (fileName1.endsWith(".log")) {
                return -1;
            } else if (fileName2.endsWith(".log")){
                return 1;
            }
            String[] splitsA = fileName1.split("\\.");
            String[] splitsB = fileName2.split("\\.");
            if(splitsA.length >= 2 && splitsB.length >= 2){
                int num1 = StringUtil.getIntString(splitsA[1]);
                int num2 = StringUtil.getIntString(splitsB[1]);
                return Integer.compare(num1, num2);
            }
            return 0;
        });
        logger.info("fileList size::{}", fileList.size());
        adapter.submitList(fileList);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_share_all) {
            ActivityUtil.shareFiles(this, adapter.getCurrentList(), getString(R.string.setting_journal_share));
        }
        switch (v.getId()) {
            case R.id.tv_share_all:
                ActivityUtil.shareFiles(this, adapter.getCurrentList(), getString(R.string.setting_journal_share));
                break;
            case R.id.ll_data_statistics:
                ActivityUtil.startActivity(this, DataStatisticsActivity.class);
                break;
            case R.id.ll_memory_statistics:
                ActivityUtil.startActivity(this, MemoryStatisticsActivity.class);
                break;
            case R.id.ll_cpu_statistics:
                ActivityUtil.startActivity(this, CpuStatisticsActivity.class);
                break;
            default:
                break;
        }
    }

    @Override
    public void onShareClicked(String fileName) {
        String path = StringUtil.getText(binding.tvJournalDirectory);
        path += fileName;
        ActivityUtil.shareFile(this, path, getString(R.string.setting_journal_share));
    }

    @Override
    protected void onStart() {
        super.onStart();
        fileObserver.startWatching();
    }

    @Override
    protected void onStop() {
        super.onStop();
        fileObserver.stopWatching();
    }

    private class JournalFileObserver extends FileObserver {
        // path 为 需要监听的文件或文件夹
        JournalFileObserver(String path) {
            super(path, FileObserver.ALL_EVENTS);
        }

        @Override
        public void onEvent(int event, String path) {
            // CREATE: 监控的文件夹下创建了子文件或文件夹
            // DELETE: 监控的文件夹下删除了子文件或文件夹
            // DELETE_SELF: 被监控的文件或文件夹被删除，监控停止
            // MODIFY: 文件被修改
            // MOVED_FROM: 被监控文件夹有子文件或文件夹移走
            // MOVED_TO: 被监控文件夹有子文件或文件夹被移入
            // MOVE_SELF: 被监控文件或文件夹被移动
            switch (event) {
                case FileObserver.CREATE:
                case FileObserver.DELETE:
                case FileObserver.DELETE_SELF:
//                case FileObserver.MODIFY:
                case FileObserver.MOVED_FROM:
                case FileObserver.MOVED_TO:
                case FileObserver.MOVE_SELF:
                    logger.info("event::{}->loadJournalData", event);
                    loadJournalData();
            }
        }
    }
}