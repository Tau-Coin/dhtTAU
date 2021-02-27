package io.taucoin.torrent.publishing.ui.main;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.CommunityAndMember;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ItemChatListBinding;
import io.taucoin.torrent.publishing.databinding.ItemGroupListBinding;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.ui.Selectable;
import io.taucoin.types.MessageType;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.CryptoUtil;

/**
 * 主页显示的群组列表的Adapter
 */
public class MainListAdapter extends ListAdapter<CommunityAndMember, MainListAdapter.ViewHolder> {
    private ClickListener listener;

    MainListAdapter(ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ViewDataBinding binding;
        if (viewType == 0) {
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_group_list,
                    parent,
                    false);
        } else {
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_chat_list,
                    parent,
                    false);
        }
        return new ViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItem(position));
    }

    @Override
    public int getItemViewType(int position) {
        return getCurrentList().get(position).type;
    }

    @Override
    public int getItemCount() {
        return getCurrentList().size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewDataBinding binding;
        private ClickListener listener;
        private Context context;

        ViewHolder(ViewDataBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = binding.getRoot().getContext();
            this.listener = listener;
        }

        void bind(ViewHolder holder, CommunityAndMember community) {
            if(null == holder || null == community){
                return;
            }
            if (holder.binding instanceof ItemGroupListBinding) {
                ItemGroupListBinding binding = (ItemGroupListBinding) holder.binding;
                if(community.txTimestamp > 0){
                    String time = DateUtil.getWeekTime(community.txTimestamp);
                    binding.tvMsgLastTime.setText(time);
                }else{
                    binding.tvMsgLastTime.setText(null);
                }
                binding.tvGroupName.setText(community.communityName);
                String firstLetters = StringUtil.getFirstLettersOfName(community.communityName);
                binding.leftView.setText(firstLetters);
                String balance = FmtMicrometer.fmtBalance(community.balance);
                String power = FmtMicrometer.fmtLong(community.power);
                binding.tvBalancePower.setText(context.getString(R.string.main_balance_power, balance, power));

                binding.tvUserMessage.setVisibility(StringUtil.isNotEmpty(community.txMemo) ?
                        View.VISIBLE : View.GONE);
                binding.tvUserMessage.setText(community.txMemo);

                int bgColor = Utils.getGroupColor(community.chainID);
                binding.leftView.setBgColor(bgColor);
                binding.readOnly.setBgColor(bgColor);
                boolean isReadOnly = community.balance <= 0 && community.power <= 0;
                binding.readOnly.setVisibility(isReadOnly ? View.VISIBLE : View.INVISIBLE);
            } else if (holder.binding instanceof ItemChatListBinding) {
                ItemChatListBinding binding = (ItemChatListBinding) holder.binding;
                binding.tvGroupName.setText(community.communityName);
                String firstLetters = StringUtil.getFirstLettersOfName(community.communityName);
                binding.leftView.setText(firstLetters);
                int bgColor = Utils.getGroupColor(community.chainID);
                binding.leftView.setBgColor(bgColor);

                String msg = community.txMemo;
                String hash = community.txMemo;
                if (community.msgType == MessageType.PICTURE.ordinal()) {
                    binding.tvUserMessage.setText(context.getString(R.string.main_pic_messages));
                } else if (community.msgType == MessageType.TEXT.ordinal() && StringUtil.isNotEmpty(hash)) {
                    byte[] cryptoKey = Utils.keyExchange(community.chainID, MainApplication.getInstance().getSeed());
                    binding.tvUserMessage.setTextHash(false, hash, community.publicKey, cryptoKey);
                } else if (StringUtil.isEmpty(msg)) {
                    binding.tvUserMessage.setText(context.getString(R.string.main_no_messages));
                }
                if(community.txTimestamp > 0){
                    String time = DateUtil.getWeekTime(community.txTimestamp);
                    binding.tvMsgLastTime.setText(time);
                }else{
                    binding.tvMsgLastTime.setText(null);
                }
                binding.readOnly.setVisibility(View.INVISIBLE);
            }
            holder.binding.getRoot().setOnClickListener(v -> {
                if(listener != null){
                    listener.onItemClicked(community);
                }
            });
        }
    }

    public interface ClickListener {
        void onItemClicked(Community item);
    }

    private static final DiffUtil.ItemCallback<CommunityAndMember> diffCallback = new DiffUtil.ItemCallback<CommunityAndMember>() {
        @Override
        public boolean areContentsTheSame(@NonNull CommunityAndMember oldItem, @NonNull CommunityAndMember newItem) {
            boolean isSame = oldItem.equals(newItem);
            if (isSame) {
                if (oldItem.type == 0) {
                    isSame = oldItem.txTimestamp == newItem.txTimestamp &&
                            oldItem.balance == newItem.balance &&
                            oldItem.power == newItem.power &&
                            StringUtil.isEquals(oldItem.txMemo, newItem.txMemo);
                } else {
                    isSame = oldItem.txTimestamp == newItem.txTimestamp &&
                            StringUtil.isEquals(oldItem.communityName, newItem.communityName) &&
                            StringUtil.isEquals(oldItem.txMemo, newItem.txMemo);
                }
            }
            return isSame;
        }

        @Override
        public boolean areItemsTheSame(@NonNull CommunityAndMember oldItem, @NonNull CommunityAndMember newItem) {
            return oldItem.equals(newItem);
        }
    };
}
