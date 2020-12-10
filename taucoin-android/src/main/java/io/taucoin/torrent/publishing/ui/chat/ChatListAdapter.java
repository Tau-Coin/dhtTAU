package io.taucoin.torrent.publishing.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.MsgAndReply;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsgType;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ItemPictureBinding;
import io.taucoin.torrent.publishing.databinding.ItemPictureRightBinding;
import io.taucoin.torrent.publishing.databinding.ItemTextBinding;
import io.taucoin.torrent.publishing.databinding.ItemTextRightBinding;
import io.taucoin.torrent.publishing.ui.customviews.HashImageView;
import io.taucoin.torrent.publishing.ui.customviews.HashTextView;
import io.taucoin.torrent.publishing.ui.customviews.RoundButton;
import io.taucoin.types.MessageType;

/**
 * 聊天消息的Adapter
 */
public class ChatListAdapter extends PagedListAdapter<ChatMsg, ChatListAdapter.ViewHolder> {

    enum ViewType {
        LEFT_TEXT,
        LEFT_PICTURE,
        RIGHT_TEXT,
        RIGHT_PICTURE
    }
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
        if (viewType == ViewType.RIGHT_PICTURE.ordinal()) {
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_picture_right,
                    parent,
                    false);
        } else if (viewType == ViewType.RIGHT_TEXT.ordinal()) {
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_text_right,
                    parent,
                    false);
        }  else if (viewType == ViewType.LEFT_PICTURE.ordinal()) {
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
        String userPk = MainApplication.getInstance().getPublicKey();
        if (chat != null) {
            if (StringUtil.isEquals(userPk, chat.senderPk) ) {
                if (chat.contextType == MessageType.PICTURE.ordinal()) {
                    return ViewType.RIGHT_PICTURE.ordinal();
                } else {
                    return ViewType.RIGHT_TEXT.ordinal();
                }
            } else {
                if (chat.contextType == MessageType.PICTURE.ordinal()) {
                    return ViewType.LEFT_PICTURE.ordinal();
                } else {
                    return ViewType.LEFT_TEXT.ordinal();
                }
            }
        }
        return ViewType.LEFT_TEXT.ordinal();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMsg previousChat = null;
        if (position > 0) {
            previousChat = getItem(position - 1);
        }
        if (getItemViewType(position) == ViewType.RIGHT_PICTURE.ordinal()) {
            ItemPictureRightBinding binding = (ItemPictureRightBinding) holder.binding;
            holder.bindPictureRight(binding, getItem(position), previousChat);
        } else if (getItemViewType(position) == ViewType.RIGHT_TEXT.ordinal()) {
            ItemTextRightBinding binding = (ItemTextRightBinding) holder.binding;
            holder.bindTextRight(binding, getItem(position), previousChat);
        } else if (getItemViewType(position) == ViewType.LEFT_PICTURE.ordinal()) {
            ItemPictureBinding binding = (ItemPictureBinding) holder.binding;
            holder.bindPicture(binding, getItem(position), previousChat);
        } else {
            ItemTextBinding binding = (ItemTextBinding) holder.binding;
            holder.bindText(binding, getItem(position), previousChat);
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

        void bindTextRight(ItemTextRightBinding binding, ChatMsg chat, ChatMsg previousChat) {
            if (null == binding || null == chat) {
                return;
            }
            showStatusView(binding.ivStats, binding.tvProgress, chat.status, true);
            bindText(binding.roundButton, binding.tvTime, binding.tvMsg,
                    binding.tvProgress, chat, previousChat, true);
        }

        void bindText(ItemTextBinding binding, ChatMsg chat, ChatMsg previousChat) {
            if (null == binding || null == chat) {
                return;
            }
            showStatusView(binding.ivStats, binding.tvProgress, chat.status, false);
            bindText(binding.roundButton, binding.tvTime, binding.tvMsg,
                    binding.tvProgress, chat, previousChat, false);
        }

        private void bindText(RoundButton roundButton, TextView tvTime, HashTextView tvMsg,
                      ProgressBar tvProgress, ChatMsg chat, ChatMsg previousChat, boolean isMine) {
            if (null == chat) {
                return;
            }
            roundButton.setBgColor(Utils.getGroupColor(chat.senderPk));

            String showName = UsersUtil.getDefaultName(chat.senderPk);
            roundButton.setText(StringUtil.getFirstLettersOfName(showName));

            boolean isShowTime = isShowTime(chat, previousChat);
            if (isShowTime) {
                String time = DateUtil.getWeekTimeWithHours(chat.timestamp);
                tvTime.setText(time);
            }
            tvTime.setVisibility(isShowTime ? View.VISIBLE : View.GONE);
            tvMsg.setTextHash(chat.contextLink, () -> {
                if (!isMine) {
                    tvProgress.setVisibility(View.GONE);
                }
            });
        }

        private void showStatusView(ImageView ivStats, ProgressBar tvProgress,
                                    int status, boolean isMine) {
            if (isMine && status == ChatMsgType.QUEUED.ordinal() ||
                    status == ChatMsgType.RECEIVED.ordinal()) {
                tvProgress.setVisibility(View.GONE);
                ivStats.setVisibility(View.VISIBLE);
                int icon = status == ChatMsgType.QUEUED.ordinal() ? R.mipmap.icon_sent :
                        R.mipmap.icon_received;
                ivStats.setImageResource(icon);
            } else {
                ivStats.setVisibility(View.GONE);
                tvProgress.setVisibility(View.VISIBLE);
            }
        }

        private boolean isShowTime(ChatMsg chat, ChatMsg previousChat) {
            if (previousChat != null) {
                int interval = DateUtil.getSeconds(previousChat.timestamp, chat.timestamp);
                return interval > 2 * 60;
            }
            return true;
        }

        void bindPictureRight(ItemPictureRightBinding binding, ChatMsg chat, ChatMsg previousChat) {
            if (null == binding || null == chat) {
                return;
            }
            showStatusView(binding.ivStats, binding.tvProgress, chat.status, true);
            bindPicture(binding.roundButton, binding.tvTime, binding.tvImage,
                    binding.tvProgress, chat, previousChat, true);
        }

        void bindPicture(ItemPictureBinding binding, ChatMsg chat, ChatMsg previousChat) {
            if(null == binding || null == chat){
                return;
            }
            showStatusView(binding.ivStats, binding.tvProgress, chat.status, false);
            bindPicture(binding.roundButton, binding.tvTime, binding.tvImage,
                    binding.tvProgress, chat, previousChat, false);
        }

        private void bindPicture(RoundButton roundButton, TextView tvTime, HashImageView tvImage,
                 ProgressBar tvProgress, ChatMsg chat, ChatMsg previousChat, boolean isMine) {
            if (null == chat) {
                return;
            }
            roundButton.setBgColor(Utils.getGroupColor(chat.senderPk));

            String showName = UsersUtil.getDefaultName(chat.senderPk);
            roundButton.setText(StringUtil.getFirstLettersOfName(showName));

            boolean isShowTime = isShowTime(chat, previousChat);
            if (isShowTime) {
                String time = DateUtil.getWeekTimeWithHours(chat.timestamp);
                tvTime.setText(time);
            }
            tvTime.setVisibility(isShowTime ? View.VISIBLE : View.GONE);
            tvImage.setImageHash(chat.contextLink, () -> {
                if (!isMine) {
                    tvProgress.setVisibility(View.GONE);
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
            return oldItem.equals(newItem) && oldItem.status == newItem.status;
        }

        @Override
        public boolean areItemsTheSame(@NonNull ChatMsg oldItem, @NonNull ChatMsg newItem) {
            return oldItem.equals(newItem);
        }
    };
}
