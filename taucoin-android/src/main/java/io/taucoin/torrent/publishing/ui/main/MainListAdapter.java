package io.taucoin.torrent.publishing.ui.main;

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
import io.taucoin.torrent.publishing.core.model.data.CommunityAndMember;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ItemGroupListBinding;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.ui.Selectable;

/**
 * 主页显示的群组列表的Adapter
 */
public class MainListAdapter extends ListAdapter<CommunityAndMember, MainListAdapter.ViewHolder>
    implements Selectable<CommunityAndMember> {
    private ClickListener listener;
    private List<CommunityAndMember> dataList = new ArrayList<>();

    MainListAdapter(ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemGroupListBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_group_list,
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
    public CommunityAndMember getItemKey(int position) {
        return dataList.get(position);
    }

    @Override
    public int getItemPosition(CommunityAndMember key) {
        return getCurrentList().indexOf(key);
    }

    /**
     * 设置列表社区展示数据
     * @param communities 社区数据
     */
    void setDataList(List<CommunityAndMember> communities) {
        dataList.clear();
        dataList.addAll(communities);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemGroupListBinding binding;
        private ClickListener listener;
        private Context context;

        ViewHolder(ItemGroupListBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = binding.getRoot().getContext();
            this.listener = listener;
        }

        void bind(ViewHolder holder, CommunityAndMember community) {
            if(null == holder || null == community){
                return;
            }
            if(community.txTimestamp > 0){
                String time = DateUtil.getWeekTime(community.txTimestamp);
                holder.binding.tvMsgLastTime.setText(time);
            }else{
                holder.binding.tvMsgLastTime.setText(null);
            }
            holder.binding.tvGroupName.setText(community.communityName);
            String firstLetters = StringUtil.getFirstLettersOfName(community.communityName);
            holder.binding.leftView.setText(firstLetters);
            String balance = FmtMicrometer.fmtBalance(community.balance);
            String power = FmtMicrometer.fmtLong(community.power);
            holder.binding.tvBalancePower.setText(context.getString(R.string.main_balance_power, balance, power));
            holder.binding.tvUserMessage.setText(community.txMemo);

            int bgColor = Utils.getGroupColor(community.chainID);
            holder.binding.leftView.setBgColor(bgColor);
            holder.binding.readOnly.setBgColor(bgColor);
            boolean isReadOnly = community.balance <= 0 && community.power <= 0;
            holder.binding.readOnly.setVisibility(isReadOnly ? View.VISIBLE : View.INVISIBLE);

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
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull CommunityAndMember oldItem, @NonNull CommunityAndMember newItem) {
            return oldItem.equals(newItem);
        }
    };
}
