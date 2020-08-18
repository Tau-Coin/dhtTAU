package io.taucoin.torrent.publishing.ui.user;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ItemUserCommunityBinding;
import io.taucoin.torrent.publishing.ui.Selectable;

/**
 * 用户加入社区的列表的Adapter
 */
public class UserCommunityListAdapter extends ListAdapter<Member, UserCommunityListAdapter.ViewHolder>
    implements Selectable<Member> {
    private List<Member> dataList = new ArrayList<>();
    private ClickListener clickListener;

    UserCommunityListAdapter(ClickListener clickListener) {
        super(diffCallback);
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemUserCommunityBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_user_community,
                parent,
                false);

        return new ViewHolder(binding, clickListener);
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
    public Member getItemKey(int position) {
        return dataList.get(position);
    }

    @Override
    public int getItemPosition(Member key) {
        return getCurrentList().indexOf(key);
    }

    /**
     * 设置示数据
     * @param user 用户数据
     */
    void setDataList(List<Member> user) {
        dataList.clear();
        dataList.addAll(user);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemUserCommunityBinding binding;
        private Context context;
        private ClickListener listener;

        ViewHolder(ItemUserCommunityBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = binding.getRoot().getContext();
            this.listener = listener;
        }

        void bind(ViewHolder holder, Member member) {
            if(null == holder || null == member){
                return;
            }
            String communityName = Utils.getCommunityName(member.chainID);
            communityName = context.getString(R.string.user_community_name, communityName);
            holder.binding.tvName.setText(communityName);

            String balance = FmtMicrometer.fmtBalance(member.balance);
            String power = FmtMicrometer.fmtLong(member.power);
            String balancePower = context.getString(R.string.main_balance_power, balance, power);
            holder.binding.tvBalancePower.setText(balancePower);

            holder.binding.getRoot().setOnClickListener(v -> {
                if(listener != null){
                    listener.onItemClicked(member);
                }
            });
        }
    }

    public interface ClickListener {
        void onItemClicked(Member member);
    }

    private static final DiffUtil.ItemCallback<Member> diffCallback = new DiffUtil.ItemCallback<Member>() {
        @Override
        public boolean areContentsTheSame(@NonNull Member oldItem, @NonNull Member newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull Member oldItem, @NonNull Member newItem) {
            return oldItem.equals(newItem);
        }
    };
}
