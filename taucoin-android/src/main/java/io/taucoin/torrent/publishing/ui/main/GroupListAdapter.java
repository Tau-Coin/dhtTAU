package io.taucoin.torrent.publishing.ui.main;

import android.view.LayoutInflater;
import android.view.ViewGroup;

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
import io.taucoin.torrent.publishing.ui.Selectable;

public class GroupListAdapter extends ListAdapter<CommunityItem, GroupListAdapter.ViewHolder>
    implements Selectable<CommunityItem> {
    private ClickListener listener;

    GroupListAdapter(ClickListener listener) {
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
        holder.bind(holder, getItemKey(position), position);
    }

    @Override
    public int getItemCount() {
        return 6;
    }

    @Override
    public CommunityItem getItemKey(int position) {
        if (position < 0 || position >= getCurrentList().size())
            return null;

        return null;
    }

    @Override
    public int getItemPosition(CommunityItem key) {
        return getCurrentList().indexOf(key);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemGroupListBinding binding;
        private ClickListener listener;

        ViewHolder(ItemGroupListBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(ViewHolder holder, CommunityItem listItem, int position) {
            String time = DateUtil.formatTime(DateUtil.getTime(), DateUtil.pattern0);
            holder.binding.tvMsgLastTime.setText(time);
            holder.binding.tvMsgNumber.setText("100");
            String groupName = "TAU Community " + position;
            holder.binding.tvGroupName.setText(groupName);
            String firstLetters = StringUtil.getFirstLettersOfName(groupName);
            holder.binding.leftView.setText(firstLetters);
            holder.binding.leftView.setBgColor(Utils.getGroupColor(firstLetters));

            holder.binding.getRoot().setOnClickListener(v -> {
                if(listener != null){
                    CommunityItem listItemTest = new CommunityItem();
                    listItemTest.setCommunityName(groupName);
                    listener.onItemClicked(listItemTest);
                }
            });
        }
    }

    public interface ClickListener {
        void onItemClicked(CommunityItem item);
    }

    private static final DiffUtil.ItemCallback<CommunityItem> diffCallback = new DiffUtil.ItemCallback<CommunityItem>() {
        @Override
        public boolean areContentsTheSame(@NonNull CommunityItem oldItem, @NonNull CommunityItem newItem) {
            return oldItem.equalsContent(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull CommunityItem oldItem, @NonNull CommunityItem newItem) {
            return oldItem.equals(newItem);
        }
    };
}
