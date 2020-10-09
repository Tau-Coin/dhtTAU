package io.taucoin.torrent.publishing.ui.setting;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.Formatter;
import io.taucoin.torrent.publishing.databinding.ItemJournalListBinding;

/**
 * 日志列表的Adapter
 */
public class JournalAdapter extends ListAdapter<File, JournalAdapter.ViewHolder> {
    private ClickListener listener;

    JournalAdapter(ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemJournalListBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_journal_list,
                parent,
                false);

        return new ViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemJournalListBinding binding;
        private ClickListener listener;
        private Context context;

        ViewHolder(ItemJournalListBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = binding.getRoot().getContext();
            this.listener = listener;
        }

        void bind(ViewHolder holder, File file) {
            if(null == holder || null == file){
                return;
            }
            binding.tvFileName.setText(file.getName());
            String fileSize = Formatter.formatFileSize(context, file.length());
            binding.tvFileSize.setText(fileSize.toUpperCase());

            binding.ivShare.setOnClickListener(v -> {
                if(listener != null){
                    listener.onShareClicked(file.getName());
                }
            });
        }
    }

    public interface ClickListener {
        void onShareClicked(String fileName);
    }

    private static final DiffUtil.ItemCallback<File> diffCallback = new DiffUtil.ItemCallback<File>() {
        @Override
        public boolean areContentsTheSame(@NonNull File oldItem, @NonNull File newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull File oldItem, @NonNull File newItem) {
            return oldItem.equals(newItem);
        }
    };
}
