package io.taucoin.torrent.publishing.ui.main;

import android.text.Html;
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
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ItemGroupListBinding;
import io.taucoin.torrent.publishing.core.storage.entity.Community;
import io.taucoin.torrent.publishing.ui.Selectable;

/**
 * 主页显示的群组列表的Adapter
 */
public class MainListAdapter extends ListAdapter<Community, MainListAdapter.ViewHolder>
    implements Selectable<Community> {
    private ClickListener listener;
    private List<Community> dataList = new ArrayList<>();

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
    public Community getItemKey(int position) {
        return dataList.get(position);
    }

    @Override
    public int getItemPosition(Community key) {
        return getCurrentList().indexOf(key);
    }

    /**
     * 设置列表社区展示数据
     * @param communities 社区数据
     */
    void setDataList(List<Community> communities) {
        dataList.clear();
        dataList.addAll(communities);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemGroupListBinding binding;
        private ClickListener listener;

        ViewHolder(ItemGroupListBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(ViewHolder holder, Community community) {
            if(null == holder || null == community){
                return;
            }
            String time = DateUtil.formatTime(DateUtil.getTime(), DateUtil.pattern0);
            holder.binding.tvMsgLastTime.setText(time);
            holder.binding.tvMsgNumber.setText("100");

            holder.binding.tvGroupName.setText(Html.fromHtml(community.communityName));
            String firstLetters = StringUtil.getFirstLettersOfName(community.communityName);
            holder.binding.leftView.setText(firstLetters);
            holder.binding.leftView.setBgColor(Utils.getGroupColor(community.chainID));

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

    private static final DiffUtil.ItemCallback<Community> diffCallback = new DiffUtil.ItemCallback<Community>() {
        @Override
        public boolean areContentsTheSame(@NonNull Community oldItem, @NonNull Community newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull Community oldItem, @NonNull Community newItem) {
            return oldItem.equals(newItem);
        }
    };
}
