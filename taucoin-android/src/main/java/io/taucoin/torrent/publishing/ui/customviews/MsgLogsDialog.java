/**
 * Copyright 2018 Taucoin Core Developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.taucoin.torrent.publishing.ui.customviews;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.ChatMsgStatus;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsgLog;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.DimensionsUtil;
import io.taucoin.torrent.publishing.databinding.ItemMsgLogBinding;
import io.taucoin.torrent.publishing.databinding.MsgLogsBinding;

/**
 * 消息发送历史展示
 */
public class MsgLogsDialog extends Dialog {

    private LogsAdapter adapter;
    public MsgLogsDialog(Context context) {
        super(context);
    }

    public MsgLogsDialog(Context context, int theme) {
        super(context, theme);
    }

    public MsgLogsDialog(Context context, int theme, LogsAdapter adapter) {
        this(context, theme);
        this.adapter = adapter;
    }

    public static class Builder {
        private Context context;
        private boolean isCanCancel = true;
        private MsgLogsListener msgLogsListener;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder setCanceledOnTouchOutside(boolean cancel) {
            this.isCanCancel = cancel;
            return this;
        }

        public Builder setMsgLogsListener(MsgLogsListener msgLogsListener) {
            this.msgLogsListener = msgLogsListener;
            return this;
        }

        public MsgLogsDialog create() {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            MsgLogsBinding binding = DataBindingUtil.inflate(inflater, R.layout.msg_logs,
                    null, false);
            LogsAdapter adapter = new LogsAdapter(msgLogsListener);
            final MsgLogsDialog msgLogsDialog = new MsgLogsDialog(context, R.style.CommonDialog, adapter);
            binding.ivClose.setOnClickListener(v -> {
                if (msgLogsDialog.isShowing()) {
                    msgLogsDialog.closeDialog();
                }
                if (msgLogsListener != null) {
                    msgLogsListener.onCancel();
                }
            });
            binding.recyclerView.setAdapter(adapter);
            LinearLayoutManager layoutManager = new LinearLayoutManager(context);
            binding.recyclerView.setLayoutManager(layoutManager);
            View layout = binding.getRoot();
            msgLogsDialog.addContentView(layout, new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT));
            msgLogsDialog.setCanceledOnTouchOutside(isCanCancel);
            msgLogsDialog.setOnCancelListener(dialog -> {
                if (msgLogsListener != null) {
                    msgLogsListener.onCancel();
                }
            });
            return msgLogsDialog;
        }
    }

    private static class LogsAdapter extends ListAdapter<ChatMsgLog, LogsAdapter.ViewHolder> {
        private MsgLogsListener listener;
        LogsAdapter(MsgLogsListener listener) {
            super(diffCallback);
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            ItemMsgLogBinding binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_msg_log,
                    parent,
                    false);
            return new ViewHolder(binding, listener);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(holder, getItem(position), position, getCurrentList().size());
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private ItemMsgLogBinding binding;
            private MsgLogsListener listener;
            ViewHolder(ItemMsgLogBinding binding, MsgLogsListener listener) {
                super(binding.getRoot());
                this.binding = binding;
                this.listener = listener;
            }

            public void bind(ViewHolder holder, ChatMsgLog log, int pos, int size) {
                if (null == binding || null == holder || null == log) {
                    return;
                }
                Context context = binding.getRoot().getContext();
                // 消息是否送达送达
                boolean isReceived = log.status == ChatMsgStatus.RECEIVED_CONFIRMATION.getStatus();
                int color = isReceived ? R.color.color_black : R.color.gray_dark;
                binding.tvTime.setText(DateUtil.formatTime(log.timestamp, DateUtil.pattern6));
                binding.tvTime.setTextColor(context.getResources().getColor(color));
                binding.tvStatus.setText(ChatMsgStatus.getStatusInfo(log.status));
                binding.tvStatus.setTextColor(context.getResources().getColor(color));

                binding.timeLineBottom.setVisibility(pos == size - 1 ? View.GONE : View.VISIBLE);
                binding.timeLineTop.setVisibility(pos == 0 ? View.INVISIBLE : View.VISIBLE);

                int timePointRes = isReceived ? R.mipmap.icon_time_end : R.mipmap.icon_time_point;
                binding.timePoint.setImageResource(timePointRes);
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                        binding.timePoint.getLayoutParams();
                int pointSize = isReceived || pos == size - 1 ? 20 : 15;
                layoutParams.width = DimensionsUtil.dip2px(context, pointSize);
                layoutParams.height = DimensionsUtil.dip2px(context, pointSize);
                binding.timePoint.setLayoutParams(layoutParams);
            }
        }

        private static final DiffUtil.ItemCallback<ChatMsgLog> diffCallback = new DiffUtil.ItemCallback<ChatMsgLog>() {
            @Override
            public boolean areContentsTheSame(@NonNull ChatMsgLog oldItem, @NonNull ChatMsgLog newItem) {
                return oldItem.equals(newItem) && oldItem.status == newItem.status;
            }

            @Override
            public boolean areItemsTheSame(@NonNull ChatMsgLog oldItem, @NonNull ChatMsgLog newItem) {
                return oldItem.equals(newItem);
            }
        };
    }

    public interface MsgLogsListener {
        void onRetry();
        void onCancel();
    }

    public void closeDialog(){
        if(isShowing()){
            dismiss();
        }
    }

    public void submitList(ChatMsg msg, List<ChatMsgLog> logs) {
        if (adapter != null) {
            logs.add(new ChatMsgLog(msg.hash, ChatMsgStatus.UNSENT.getStatus(), msg.timestamp));
            adapter.submitList(logs);
        }
    }
}