package io.taucoin.torrent.publishing.ui.community;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.MemberAndUser;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ItemContactListBinding;
import io.taucoin.torrent.publishing.ui.Selectable;

/**
 * 显示的联系人列表的Adapter
 */
public class MemberListAdapter extends ListAdapter<MemberAndUser, MemberListAdapter.ViewHolder>
    implements Selectable<MemberAndUser> {
    private ClickListener listener;
    private List<MemberAndUser> dataList = new ArrayList<>();

    MemberListAdapter(ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemContactListBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_contact_list,
                parent,
                false);

        return new ViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItemKey(position));
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    @Override
    public MemberAndUser getItemKey(int position) {
        return dataList.get(position);
    }

    @Override
    public int getItemPosition(MemberAndUser key) {
        return getCurrentList().indexOf(key);
    }

    /**
     * 设置联系人展示数据
     * @param members 用户数据
     */
    void setDataList(List<MemberAndUser> members) {
        dataList.clear();
        dataList.addAll(members);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemContactListBinding binding;
        private ClickListener listener;
        private Context context;

        ViewHolder(ItemContactListBinding binding, ClickListener listener) {
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
            if(member.user != null && member.user.lastUpdateTime > 0){
                time = DateUtil.formatTime(member.user.lastUpdateTime, DateUtil.pattern5);
                time = context.getResources().getString(R.string.contacts_last_seen, time);
            }
            holder.binding.tvTime.setText(time);
            holder.binding.tvCommunities.setVisibility(View.GONE);
            holder.binding.ivShare.setVisibility(View.GONE);

            int bgColor = Utils.getGroupColor(member.publicKey);
            holder.binding.leftView.setBgColor(bgColor);

            holder.binding.getRoot().setOnClickListener(v -> {
                if(listener != null){
                    listener.onItemClicked(member);
                }
            });
        }
    }

    public interface ClickListener {
        void onItemClicked(MemberAndUser item);
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
