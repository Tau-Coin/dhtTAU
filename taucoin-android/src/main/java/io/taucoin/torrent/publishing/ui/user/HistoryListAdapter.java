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
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.databinding.ItemHistoryListBinding;
import io.taucoin.torrent.publishing.ui.Selectable;

/**
 * 主页显示的群组列表的Adapter
 */
public class HistoryListAdapter extends ListAdapter<User, HistoryListAdapter.ViewHolder>
        implements Selectable<User> {
    private List<User> dataList = new ArrayList<>();
    private ClickListener listener;

    HistoryListAdapter(ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemHistoryListBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_history_list,
                parent,
                false);

        return new ViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryListAdapter.ViewHolder holder, int position) {
        User user = getItemKey(position);
        holder.bindUser(user);
    }

    @Override
    public User getItemKey(int position) {
        return dataList.get(position);
    }

    @Override
    public int getItemPosition(User key) {
        return getCurrentList().indexOf(key);
    }

    @Override
    public int getItemCount() {
        return dataList.size();
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemHistoryListBinding binding;
        private ClickListener listener;

        ViewHolder(ItemHistoryListBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
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
            String seed = UsersUtil.getMidHideName(user.seed);
            binding.tvSeed.setText(seed);
            int nameColor = user.isCurrentUser ? R.color.primary : R.color.text_primary;
            int pkColor = user.isCurrentUser ? R.color.primary : R.color.gray_dark;
            Context context = binding.getRoot().getContext();
            binding.tvName.setTextColor(context.getResources().getColor(nameColor));
            binding.tvSeed.setTextColor(context.getResources().getColor(pkColor));
            binding.getRoot().setOnClickListener(view->{
                if(listener != null && !user.isCurrentUser){
                    listener.onItemClicked(user);
                }
            });
        }
    }

    public interface ClickListener {
        void onItemClicked(User user);
    }

    private static final DiffUtil.ItemCallback<User> diffCallback = new DiffUtil.ItemCallback<User>() {
        @Override
        public boolean areContentsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            return oldItem.equals(newItem);
        }
    };
}
