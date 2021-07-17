package io.taucoin.torrent.publishing.ui.main;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.CommunityAndFriend;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ItemChatListBinding;
import io.taucoin.torrent.publishing.databinding.ItemGroupListBinding;
import io.taucoin.types.MessageType;

/**
 * 主页显示的群组列表的Adapter
 */
public class MainListAdapter extends ListAdapter<CommunityAndFriend, MainListAdapter.ViewHolder> {
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

        void bind(ViewHolder holder, CommunityAndFriend bean) {
            if(null == holder || null == bean){
                return;
            }
            if (holder.binding instanceof ItemGroupListBinding) {
                ItemGroupListBinding binding = (ItemGroupListBinding) holder.binding;
                if(bean.timestamp > 0){
                    String time = DateUtil.getWeekTime(bean.timestamp);
                    binding.tvMsgLastTime.setText(time);
                }else{
                    binding.tvMsgLastTime.setText(null);
                }
                String communityName = UsersUtil.getCommunityName(bean.ID);
                binding.tvGroupName.setText(communityName);
                String firstLetters = StringUtil.getFirstLettersOfName(communityName);
                binding.leftView.setText(firstLetters);
                String balance = FmtMicrometer.fmtBalance(bean.balance);
                String power = FmtMicrometer.fmtLong(bean.power);
                binding.tvBalancePower.setText(context.getString(R.string.main_balance_power, balance, power));

                binding.tvUserMessage.setVisibility(StringUtil.isNotEmpty(bean.memo) ?
                        View.VISIBLE : View.GONE);
                binding.tvUserMessage.setText(bean.memo);

                int bgColor = Utils.getGroupColor(bean.ID);
                binding.leftView.setBgColor(bgColor);
                binding.readOnly.setBgColor(bgColor);
                boolean isReadOnly = bean.balance <= 0 && bean.power <= 0;
                binding.readOnly.setVisibility(isReadOnly ? View.VISIBLE : View.INVISIBLE);
            } else if (holder.binding instanceof ItemChatListBinding) {
                ItemChatListBinding binding = (ItemChatListBinding) holder.binding;
                String friendNickName = UsersUtil.getShowNameWithYourself(bean.friend, bean.ID);
                binding.tvGroupName.setText(friendNickName);
                String firstLetters = StringUtil.getFirstLettersOfName(friendNickName);
                binding.leftView.setText(firstLetters);
                int bgColor = Utils.getGroupColor(bean.ID);
                binding.leftView.setBgColor(bgColor);

                byte[] msg = bean.msg;
                if (bean.msgType == MessageType.PICTURE.ordinal()) {
                    binding.tvUserMessage.setText(context.getString(R.string.main_pic_messages));
                } else if (bean.msgType == MessageType.TEXT.ordinal() && msg != null) {
                    binding.tvUserMessage.setTextContent(bean.msg, bean.senderPk, bean.receiverPk);
                } else if (null == msg) {
                    binding.tvUserMessage.setText(context.getString(R.string.main_no_messages));
                }
                if (bean.timestamp > 0) {
                    String time = DateUtil.getWeekTime(bean.timestamp);
                    binding.tvMsgLastTime.setText(time);
                } else {
                    binding.tvMsgLastTime.setText(null);
                }
                binding.msgUnread.setVisibility(bean.msgUnread > 0 ? View.VISIBLE : View.GONE);
            }
            holder.binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClicked(bean);
                }
            });
        }
    }

    public interface ClickListener {
        void onItemClicked(CommunityAndFriend item);
    }

    private static final DiffUtil.ItemCallback<CommunityAndFriend> diffCallback = new DiffUtil.ItemCallback<CommunityAndFriend>() {
        @Override
        public boolean areContentsTheSame(@NonNull CommunityAndFriend oldItem, @NonNull CommunityAndFriend newItem) {
            boolean isSame = oldItem.equals(newItem);
            if (isSame) {
                if (oldItem.type == 0) {
                    isSame = oldItem.timestamp == newItem.timestamp &&
                            oldItem.balance == newItem.balance &&
                            oldItem.power == newItem.power &&
                            StringUtil.isEquals(oldItem.memo, newItem.memo);
                } else {
                    isSame = oldItem.timestamp == newItem.timestamp &&
                            oldItem.msgUnread == newItem.msgUnread &&
                            Arrays.equals(oldItem.msg, newItem.msg) &&
                            StringUtil.isEquals(oldItem.friend != null ? oldItem.friend.nickname : null,
                                    newItem.friend != null ? newItem.friend.nickname : null);
                }
            }
            return isSame;
        }

        @Override
        public boolean areItemsTheSame(@NonNull CommunityAndFriend oldItem, @NonNull CommunityAndFriend newItem) {
            return oldItem.equals(newItem);
        }
    };
}
