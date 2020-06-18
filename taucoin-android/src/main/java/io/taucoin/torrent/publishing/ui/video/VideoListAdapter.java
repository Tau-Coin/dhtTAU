package io.taucoin.torrent.publishing.ui.video;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ItemVideoListBinding;
import io.taucoin.torrent.publishing.ui.Selectable;

public class VideoListAdapter extends ListAdapter<VideoListItem, VideoListAdapter.ViewHolder>
    implements Selectable<VideoListItem> {
    private ClickListener listener;

    VideoListAdapter(ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemVideoListBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_video_list,
                parent,
                false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, position);
    }

    @Override
    public int getItemCount() {
        return 10;
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

        void bind(ViewHolder holder, int position) {
            holder.binding.tvType.setText("Action/test");
            holder.binding.tvName.setText("Star War");
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
