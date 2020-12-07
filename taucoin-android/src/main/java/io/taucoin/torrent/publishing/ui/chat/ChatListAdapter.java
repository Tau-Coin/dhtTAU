package io.taucoin.torrent.publishing.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.MsgAndReply;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsgType;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ItemPictureBinding;
import io.taucoin.torrent.publishing.databinding.ItemTextBinding;
import io.taucoin.torrent.publishing.databinding.MsgLeftViewBinding;
import io.taucoin.types.MessageType;

/**
 * 聊天消息的Adapter
 */
public class ChatListAdapter extends PagedListAdapter<ChatMsg, ChatListAdapter.ViewHolder> {
    private ClickListener listener;

    ChatListAdapter(ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        ViewDataBinding binding;
        if (viewType == MessageType.PICTURE.ordinal()) {
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_picture,
                    parent,
                    false);
        } else {
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_text,
                    parent,
                    false);
        }
        return new ViewHolder(binding, listener);
    }

    @Override
    public int getItemViewType(int position) {
        ChatMsg chat = getItem(position);
        if (chat != null) {
            return chat.contextType;
        }
        return -1;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMsg previousChat = null;
        if (position > 0) {
            previousChat = getItem(position - 1);
        }
        if (getItemViewType(position) == MessageType.PICTURE.ordinal()) {
            holder.bindPicture(holder, getItem(position), previousChat);
        } else if (getItemViewType(position) == MessageType.TEXT.ordinal()) {
            holder.bindText(holder, getItem(position), previousChat);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewDataBinding binding;
        private ClickListener listener;

        ViewHolder(ViewDataBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bindText(ViewHolder holder, ChatMsg chat, ChatMsg previousChat) {
            if(null == this.binding || null == holder || null == chat){
                return;
            }
            ItemTextBinding binding = (ItemTextBinding) this.binding;
            binding.roundButton.setBgColor(Utils.getGroupColor(chat.senderPk));

            String showName = UsersUtil.getDefaultName(chat.senderPk);
            binding.roundButton.setText(StringUtil.getFirstLettersOfName(showName));

            boolean isShowTime = isShowTime(chat, previousChat);
            if (isShowTime) {
                String time = DateUtil.getWeekTimeWithHours(chat.timestamp);
                binding.tvTime.setText(time);
            }
            binding.tvTime.setVisibility(isShowTime ? View.VISIBLE : View.GONE);
            binding.tvMsg.setTextHash(chat.contextLink);
            showStatusView(binding.ivStats, binding.tvProgress, chat.status);
        }

        private void showStatusView(ImageView ivStats, ProgressBar tvProgress, int status) {
            if (status == ChatMsgType.QUEUED.ordinal() || status == ChatMsgType.RECEIVED.ordinal()) {
                tvProgress.setVisibility(View.GONE);
                ivStats.setVisibility(View.VISIBLE);
                int icon = status == ChatMsgType.QUEUED.ordinal() ? R.mipmap.icon_sent :
                        R.mipmap.icon_received;
                ivStats.setImageResource(icon);
            } else {
                tvProgress.setVisibility(View.VISIBLE);
                ivStats.setVisibility(View.GONE);
            }
        }

        private boolean isShowTime(ChatMsg chat, ChatMsg previousChat) {
            if (previousChat != null) {
                int interval = DateUtil.getSeconds(previousChat.timestamp, chat.timestamp);
                return interval > 2 * 60;
            }
            return true;
        }

        void bindPicture(ViewHolder holder, ChatMsg chat, ChatMsg previousChat) {
            if(null == this.binding || null == holder || null == chat){
                return;
            }
            ItemPictureBinding binding = (ItemPictureBinding) this.binding;
            binding.roundButton.setBgColor(Utils.getGroupColor(chat.senderPk));

            String showName = UsersUtil.getDefaultName(chat.senderPk);
            binding.roundButton.setText(StringUtil.getFirstLettersOfName(showName));

            boolean isShowTime = isShowTime(chat, previousChat);
            if (isShowTime) {
                String time = DateUtil.getWeekTimeWithHours(chat.timestamp);
                binding.tvTime.setText(time);
            }
            binding.tvTime.setVisibility(isShowTime ? View.VISIBLE : View.GONE);
            binding.tvImage.setImageHash(chat.contextLink);
            showStatusView(binding.ivStats, binding.tvProgress, chat.status);
        }

        private void setLeftViewClickListener(MsgLeftViewBinding leftView, MsgAndReply msg) {
            leftView.roundButton.setOnClickListener(view ->{
                if(listener != null){
                    listener.onUserClicked(msg.senderPk);
                }
            });
            leftView.tvEditName.setOnClickListener(view -> {
                if(listener != null){
                    listener.onEditNameClicked(msg.senderPk);
                }
            });
            leftView.tvBlacklist.setOnClickListener(view ->{
                if(listener != null){
                    listener.onBanClicked(msg);
                }
            });
        }
    }

    public interface ClickListener {
        void onUserClicked(String publicKey);
        void onEditNameClicked(String tx);
        void onBanClicked(MsgAndReply tx);
        void onItemLongClicked(View view, MsgAndReply tx);
    }

    private static final DiffUtil.ItemCallback<ChatMsg> diffCallback = new DiffUtil.ItemCallback<ChatMsg>() {
        @Override
        public boolean areContentsTheSame(@NonNull ChatMsg oldItem, @NonNull ChatMsg newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull ChatMsg oldItem, @NonNull ChatMsg newItem) {
            return oldItem.equals(newItem);
        }
    };
}
