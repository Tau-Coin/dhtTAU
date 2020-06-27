package io.taucoin.torrent.publishing.ui.video;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ItemListFootBinding;
import io.taucoin.torrent.publishing.databinding.ItemVideoListBinding;
import io.taucoin.torrent.publishing.ui.Selectable;

public class VideoListAdapter extends ListAdapter<VideoListItem, VideoListAdapter.ViewHolder>
        implements Selectable<VideoListItem> {
    private ClickListener listener;
    private int itemCount = 10;
    private static final int TYPE_ITEM = 0;
    private static final int TYPE_FOOTER = 1;

    void setDataCount(int itemCount){
        this.itemCount = itemCount;
    }

    VideoListAdapter(ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_ITEM) {
            ItemVideoListBinding binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_video_list,
                    parent,
                    false);
            return new ViewHolder(binding);
        } else if (viewType == TYPE_FOOTER) {
            ItemListFootBinding binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_list_foot,
                    parent,
                    false);
            return new ViewHolder(binding);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, position);
    }

    @Override
    public int getItemViewType(int position) {
        if (position + 1 == getItemCount()) {
            return TYPE_FOOTER;
        } else {
            return TYPE_ITEM;
        }
    }

    @Override
    public int getItemCount() {
        return itemCount;
    }

    @Override
    public VideoListItem getItemKey(int position) {
        if (position < 0 || position >= getCurrentList().size())
            return null;

        return null;
    }

    @Override
    public int getItemPosition(VideoListItem key) {
        return getCurrentList().indexOf(key);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemVideoListBinding binding;
        /* For selection support */
        private VideoListItem selectionKey;
        private boolean isSelected;

        ViewHolder(ItemVideoListBinding binding) {
            super(binding.getRoot());

            this.binding = binding;
            Utils.colorizeProgressBar(itemView.getContext(), binding.progress);
        }

        ViewHolder(ItemListFootBinding binding) {
            super(binding.getRoot());
        }

        void bind(ViewHolder holder, int position) {
            if(holder != null && holder.binding != null){
                holder.binding.tvType.setText("Action/test");
                holder.binding.tvName.setText("Star War");
            }
        }
    }

    public interface ClickListener {
        void onItemClicked(@NonNull VideoListItem item);

        void onItemPauseClicked(@NonNull VideoListItem item);
    }

    private static final DiffUtil.ItemCallback<VideoListItem> diffCallback = new DiffUtil.ItemCallback<VideoListItem>() {
        @Override
        public boolean areContentsTheSame(@NonNull VideoListItem oldItem, @NonNull VideoListItem newItem) {
            return oldItem.equalsContent(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull VideoListItem oldItem, @NonNull VideoListItem newItem) {
            return oldItem.equals(newItem);
        }
    };

}
