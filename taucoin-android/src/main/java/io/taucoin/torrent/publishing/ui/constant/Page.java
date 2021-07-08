package io.taucoin.torrent.publishing.ui.constant;

import androidx.paging.PagedList;

/**
 * 列表分页展示参数
 */
public class Page {
    // 每页大小
    public static final int PAGE_SIZE = 20;

    // 是否启动占位符
    public static final boolean ENABLE_PLACEHOLDERS = false;

    public static PagedList.Config getPageListConfig() {
        return new PagedList.Config.Builder()
                .setPageSize(Page.PAGE_SIZE)
                .setInitialLoadSizeHint(Page.PAGE_SIZE)
//                .setPrefetchDistance(1)
                .setEnablePlaceholders(Page.ENABLE_PLACEHOLDERS)
                .build();
    }
}