package io.taucoin.torrent.publishing.ui.community;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.MemberAndUser;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ItemMemberListBinding;

/**
 * 显示的联系人列表的Adapter
 */
public class MemberListAdapter extends PagedListAdapter<MemberAndUser,
        MemberListAdapter.ViewHolder> {
    private ClickListener listener;

    MemberListAdapter(ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemMemberListBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_member_list,
                parent,
                false);

        return new ViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemMemberListBinding binding;
        private ClickListener listener;
        private Context context;

        ViewHolder(ItemMemberListBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = binding.getRoot().getContext();
            this.listener = listener;
        }

        void bind(ViewHolder holder, MemberAndUser member) {
            if(null == holder || null == member){
                return;
            }
            String showName = UsersUtil.getDefaultName(member.publicKey);
            if(member.user != null){
                showName = UsersUtil.getShowName(member.user);
            }
            holder.binding.tvName.setText(showName);
            String firstLetters = StringUtil.getFirstLettersOfName(showName);
            holder.binding.leftView.setText(firstLetters);

            String time = "";
//            if(member.user != null && member.user.lastUpdateTime > 0){
//                time = DateUtil.formatTime(member.user.lastUpdateTime, DateUtil.pattern5);
//                time = context.getResources().getString(R.string.contacts_last_seen, time);
//            }
            holder.binding.tvTime.setText(time);
            holder.binding.tvCommunities.setVisibility(View.GONE);
            holder.binding.ivShare.setVisibility(View.VISIBLE);

            int bgColor = Utils.getGroupColor(member.publicKey);
            holder.binding.leftView.setBgColor(bgColor);

            holder.binding.ivShare.setOnClickListener(v ->{
                if(listener != null){
                    listener.onShareClicked(member);
                }
            });

            holder.binding.getRoot().setOnClickListener(v -> {
                if(listener != null){
                    listener.onItemClicked(member);
                }
            });
        }
    }

    public interface ClickListener {
        void onItemClicked(MemberAndUser item);
        void onShareClicked(MemberAndUser item);
    }

    private static final DiffUtil.ItemCallback<MemberAndUser> diffCallback = new DiffUtil.ItemCallback<MemberAndUser>() {
        @Override
        public boolean areContentsTheSame(@NonNull MemberAndUser oldItem, @NonNull MemberAndUser newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull MemberAndUser oldItem, @NonNull MemberAndUser newItem) {
            return oldItem.equals(newItem);
        }
    };
}
