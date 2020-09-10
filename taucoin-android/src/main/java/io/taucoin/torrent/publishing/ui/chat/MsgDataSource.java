package io.taucoin.torrent.publishing.ui.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.paging.PositionalDataSource;
import io.reactivex.disposables.Disposable;
import io.taucoin.torrent.publishing.core.model.data.MsgAndReply;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.MsgRepository;
import io.taucoin.torrent.publishing.core.utils.StringUtil;

class MsgDataSource extends PositionalDataSource<MsgAndReply> {
    private static final Logger logger = LoggerFactory.getLogger("MsgDataSource");
    private MsgRepository msgRepo;
    private String chainID;
    private Disposable disposable;

    MsgDataSource(@NonNull MsgRepository msgRepo, @NonNull String chainID) {
        this.msgRepo = msgRepo;
        this.chainID = chainID;
        disposable = msgRepo.observeDataSetChanged()
                .subscribe(s -> invalidate());
    }

    @Override
    public void invalidate() {
        if(disposable != null && !disposable.isDisposed()){
            disposable.dispose();
        }
        super.invalidate();
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams params,
                            @NonNull LoadInitialCallback<MsgAndReply> callback) {
        if(StringUtil.isEmpty(chainID)) {
            return;
        }
        int numMessages = msgRepo.getNumMessages(chainID);
        int pos;
        int loadSize = params.requestedLoadSize;
        // 初始加载大小大于等于数据总数，开始位置为0，否则为二者之差
        if (loadSize >= numMessages) {
            pos = 0;
        } else {
            pos = numMessages - loadSize;
        }
        logger.debug("loadInitial pos::{}, LoadSize::{}, numMessages::{}", pos, loadSize, numMessages);
        List<MsgAndReply> messages = msgRepo.getMessages(chainID, pos, loadSize);
        logger.debug("loadInitial messages.size::{}", messages.size());
        if (messages.isEmpty()) {
            callback.onResult(messages, 0);
        } else {
            callback.onResult(messages, pos);
        }
    }

    @Override
    public void loadRange(@NonNull LoadRangeParams params,
                          @NonNull LoadRangeCallback<MsgAndReply> callback) {
        if(StringUtil.isEmpty(chainID)) {
            return;
        }

        List<MsgAndReply> messages;
        int numMessages = msgRepo.getNumMessages(chainID);
        int pos = params.startPosition;
        int loadSize = params.loadSize;
        logger.debug("loadRange pos::{}, loadSize::{}, numEntries::{}", pos, loadSize, numMessages);
        if (pos < numMessages) {
            // 开始位置小于数据总数
            messages = msgRepo.getMessages(chainID, pos, loadSize);
        } else {
            // 否则数据为空
            messages = new ArrayList<>(0);
        }
        logger.debug("loadRange messages.size::{}", messages.size());
        callback.onResult(messages);
    }
}
