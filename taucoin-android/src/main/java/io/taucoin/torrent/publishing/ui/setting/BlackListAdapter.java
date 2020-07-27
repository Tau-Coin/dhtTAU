package io.taucoin.torrent.publishing.ui.setting;

import android.os.Parcelable;
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
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ItemBlacklistBinding;
import io.taucoin.torrent.publishing.ui.Selectable;

/**
 * 主页显示的群组列表的Adapter
 */
public class BlackListAdapter extends ListAdapter<Parcelable, BlackListAdapter.ViewHolder>
        implements Selectable<Parcelable> {
    private List<Parcelable> dataList = new ArrayList<>();

    BlackListAdapter() {
        super(diffCallback);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemBlacklistBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_blacklist,
                parent,
                false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BlackListAdapter.ViewHolder holder, int position) {
        Parcelable parcelable = getItemKey(position);
        if(parcelable instanceof Community){
            holder.bindCommunity((Community)parcelable);
        }else if(parcelable instanceof User){
            holder.bindUser((User)parcelable);
        }
    }

    @Override
    public Parcelable getItemKey(int position) {
        return dataList.get(position);
    }

    @Override
    public int getItemPosition(Parcelable key) {
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

    /**
     * 设置用户列表列表数据
     * @param dataList 用户数据
     */
    void setUserList(List<User> dataList) {
        this.dataList.clear();
        if(dataList != null){
            this.dataList.addAll(dataList);
        }
        notifyDataSetChanged();
    }

    /**
     * 删除列表Item
     * @param pos 位置索引
     */
    void deleteItem(int pos) {
        if(pos >= 0 && pos < dataList.size()){
            dataList.remove(pos);
            notifyDataSetChanged();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemBlacklistBinding binding;

        ViewHolder(ItemBlacklistBinding binding) {
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

        /**
         * 绑定用户数据
         */
        void bindUser(User user) {
            if(null == user){
                return;
            }
            String showName = UsersUtil.getShowName(user);
            binding.tvName.setText(showName);
            String publicKey = UsersUtil.getDefaultName(user.publicKey);
            publicKey = binding.getRoot().getResources().getString(R.string.common_parentheses, publicKey);
            binding.tvPublicKey.setText(publicKey);
            String firstLetters = StringUtil.getFirstLettersOfName(showName);
            binding.leftView.setText(firstLetters);
            binding.leftView.setBgColor(Utils.getGroupColor(firstLetters));
        }
    }

    private static final DiffUtil.ItemCallback<Parcelable> diffCallback = new DiffUtil.ItemCallback<Parcelable>() {
        @Override
        public boolean areContentsTheSame(@NonNull Parcelable oldItem, @NonNull Parcelable newItem) {
            if(oldItem instanceof Community){
                return ((Community)oldItem).equals(newItem);
            }else if(oldItem instanceof User){
                return ((User)oldItem).equals(newItem);
            }
            return true;
        }

        @Override
        public boolean areItemsTheSame(@NonNull Parcelable oldItem, @NonNull Parcelable newItem) {
            if(oldItem instanceof Community){
                return ((Community)oldItem).equals(newItem);
            }else if(oldItem instanceof User){
                return ((User)oldItem).equals(newItem);
            }
            return oldItem.equals(newItem);
        }
    };
}
