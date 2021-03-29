package io.taucoin.torrent.publishing.ui.chat;

import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.MsgAndReply;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ItemMsgBinding;
import io.taucoin.torrent.publishing.databinding.MsgLeftViewBinding;

/**
 * 消息/交易列表显示的Adapter
 */
public class MsgListAdapter extends PagedListAdapter<MsgAndReply, MsgListAdapter.ViewHolder> {
    private ClickListener listener;

    MsgListAdapter(ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemMsgBinding binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_msg,
                    parent,
                    false);
        return new ViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemMsgBinding binding;
        private ClickListener listener;

        ViewHolder(ItemMsgBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(ViewHolder holder, MsgAndReply msg) {
            if(null == binding || null == holder || null == msg){
                return;
            }
            binding.leftView.roundButton.setBgColor(Utils.getGroupColor(msg.senderPk));
            String showName = UsersUtil.getShowName(msg.sender, msg.senderPk);
            binding.leftView.tvName.setText(StringUtil.getFirstLettersOfName(showName));
            binding.leftView.tvBalance.setText(UsersUtil.getShowBalance(msg.senderBalance));

            binding.tvName.setText(showName);

            String userName = UsersUtil.getUserName(msg.sender, msg.senderPk);
            binding.leftView.tvEditName.setText(userName);

            String time = DateUtil.getWeekTime(msg.timestamp);
            binding.tvTime.setText(time);

            SpannableStringBuilder context = Utils.getSpannableStringUrl(msg.content);
            binding.tvMsg.setText(context);

            if(msg.replyMsg != null){
                binding.llReply.setVisibility(View.VISIBLE);
                String replyName = UsersUtil.getShowName(msg.replyMsg.senderPk, msg.replyName);
                binding.tvReplyName.setText(replyName);
                binding.tvReplyMsg.setText(msg.replyMsg.content);
            }else{
                binding.llReply.setVisibility(View.GONE);
            }

            setLeftViewClickListener(binding.leftView, msg);

            if(StringUtil.isEquals(msg.senderPk,
                    MainApplication.getInstance().getPublicKey())){
                binding.leftView.tvBlacklist.setVisibility(View.GONE);
            } else {
                binding.leftView.tvBlacklist.setVisibility(View.VISIBLE);
            }
            binding.middleView.setOnLongClickListener(view -> {
                if(listener != null){
                    listener.onItemLongClicked(view, msg);
                }
                return false;
            });
        }

        private void setLeftViewClickListener(MsgLeftViewBinding leftView, MsgAndReply msg) {
            leftView.roundButton.setOnClickListener(view ->{
                if(listener != null){
                    listener.onUserClicked(msg.senderPk);
                }
            });
            leftView.tvEditName.setOnClickListener(view ->{
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

    private static final DiffUtil.ItemCallback<MsgAndReply> diffCallback = new DiffUtil.ItemCallback<MsgAndReply>() {
        @Override
        public boolean areContentsTheSame(@NonNull MsgAndReply oldItem, @NonNull MsgAndReply newItem) {
            boolean isSame = false;
            if (null == oldItem.sender && null == newItem.sender) {
                isSame = true;
            } else if(null != oldItem.sender && null != newItem.sender){
                isSame =  StringUtil.isEquals(oldItem.sender.nickname, newItem.sender.nickname);
            }
            if(isSame && oldItem.senderBalance != newItem.senderBalance){
                isSame = false;
            }
            return isSame;
        }

        @Override
        public boolean areItemsTheSame(@NonNull MsgAndReply oldItem, @NonNull MsgAndReply newItem) {
            return oldItem.equals(newItem);
        }
    };
}
