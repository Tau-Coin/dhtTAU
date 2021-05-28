package io.taucoin.torrent.publishing.ui.customviews;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.databinding.ChatAddBinding;
import io.taucoin.torrent.publishing.databinding.ItemChatAddListBinding;
import io.taucoin.torrent.publishing.ui.Selectable;

public class ChatAddView extends LinearLayout {
    private ItemAdapter adapter;

    public ChatAddView(Context context) {
        this(context, null);
    }

    public ChatAddView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChatAddView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    public void setListener(OnItemClickListener listener) {
        adapter.setListener(listener);
    }

    private void initView() {
        ChatAddBinding binding = DataBindingUtil.inflate(LayoutInflater.from(getContext()),
                R.layout.chat_add, this, true);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 4);
        binding.itemRecyclerView.setLayoutManager(layoutManager);
        adapter = new ItemAdapter();
        binding.itemRecyclerView.setAdapter(adapter);
    }

    static class ItemAdapter extends ListAdapter<Integer, ItemAdapter.ViewHolder> implements Selectable<Integer> {
        private int[] icons = {R.mipmap.icon_album_black, R.mipmap.icon_take_picture};
        private int[] titles = {R.string.chat_album, R.string.chat_take_picture};
        private OnItemClickListener listener;

        ItemAdapter() {
            super(diffCallback);
            icons = new int[]{R.mipmap.icon_debug, R.mipmap.icon_debug,
                    R.mipmap.icon_debug, R.mipmap.icon_debug};
            titles =  new int[]{R.string.common_debug_digit1, R.string.common_debug_digit2,
                    R.string.common_debug_str1, R.string.common_debug_str2};
        }

        public void setListener(OnItemClickListener listener) {
            this.listener = listener;
        }

        @NonNull
        public ItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            ItemChatAddListBinding binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_chat_add_list,
                    parent,
                    false);
            return new ItemAdapter.ViewHolder(binding.getRoot(), binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemAdapter.ViewHolder holder, int position) {
            if (position >= 0 && position < titles.length && holder.binding != null) {
                int icon = icons[position];
                int title = titles[position];
                holder.binding.tvTitle.setText(title);
                holder.binding.ivIcon.setImageResource(icon);
                holder.itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onItemClick(title, icon);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return titles.length;
        }

        @Override
        public Integer getItemKey(int position) {
            if (position >= 0 && position < titles.length) {
                return titles[position];
            }
            return 0;
        }

        @Override
        public int getItemPosition(Integer key) {
            return getCurrentList().indexOf(key);
        }
        static class ViewHolder extends RecyclerView.ViewHolder{
            ItemChatAddListBinding binding;
            ViewHolder(@NonNull View itemView, ItemChatAddListBinding binding) {
                super(itemView);
                this.binding = binding;
            }
        }
    }

    private static final DiffUtil.ItemCallback<Integer> diffCallback = new DiffUtil.ItemCallback<Integer>() {
        @Override
        public boolean areContentsTheSame(@NonNull Integer oldItem, @NonNull Integer newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull Integer oldItem, @NonNull Integer newItem) {
            return oldItem.equals(newItem);
        }
    };

    public interface OnItemClickListener{
        void onItemClick(int title, int icon);
    }
}
