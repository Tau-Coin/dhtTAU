package io.taucoin.torrent.publishing.ui.community;

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
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ItemCommunityChooseBinding;
import io.taucoin.torrent.publishing.ui.Selectable;

/**
 * 社区选择列表的Adapter
 */
public class ChooseListAdapter extends ListAdapter<Community, ChooseListAdapter.ViewHolder>
        implements Selectable<Community> {
    private List<Community> dataList = new ArrayList<>();

    ChooseListAdapter() {
        super(diffCallback);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemCommunityChooseBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_community_choose,
                parent,
                false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ChooseListAdapter.ViewHolder holder, int position) {
        Community community = getItemKey(position);
        holder.bindCommunity(community);
    }

    @Override
    public Community getItemKey(int position) {
        return dataList.get(position);
    }

    @Override
    public int getItemPosition(Community key) {
        return getCurrentList().indexOf(key);
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    /**
     * 设置社区列表列表数据
     * @param dataList 社区数据
     */
    void setCommunityList(List<Community> dataList) {
        this.dataList.clear();
        if(dataList != null){
            this.dataList.addAll(dataList);
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemCommunityChooseBinding binding;

        ViewHolder(ItemCommunityChooseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * 绑定社区数据
         */
        void bindCommunity(Community community) {
            if(null == community){
                return;
            }
            binding.tvName.setText(community.communityName);
            String firstLetters = StringUtil.getFirstLettersOfName(community.communityName);
            binding.leftView.setText(firstLetters);
            binding.leftView.setBgColor(Utils.getGroupColor(firstLetters));
        }
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
