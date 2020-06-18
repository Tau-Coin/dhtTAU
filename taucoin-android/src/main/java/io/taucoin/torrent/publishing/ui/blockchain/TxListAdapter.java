package io.taucoin.torrent.publishing.ui.blockchain;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.databinding.ItemTxListBinding;
import io.taucoin.torrent.publishing.ui.Selectable;

public class TxListAdapter extends ListAdapter<TxListItem, TxListAdapter.ViewHolder>
    implements Selectable<TxListItem> {
    private ClickListener listener;

    TxListAdapter(ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemTxListBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_tx_list,
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
    public TxListItem getItemKey(int position) {
        if (position < 0 || position >= getCurrentList().size())
            return null;

        return null;
    }

    @Override
    public int getItemPosition(TxListItem key) {
        return getCurrentList().indexOf(key);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemTxListBinding binding;
        /* For selection support */
        private TxListItem selectionKey;
        private boolean isSelected;

        ViewHolder(ItemTxListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ViewHolder holder, int position) {
            holder.binding.tvHash.setText("Transaction hash/123122222222222222222222222222222222");
            holder.binding.tvName.setText("Star War");
            holder.binding.tvTime.setText("17/06/2020 11:42");
        }
    }

    public interface ClickListener {
        void onItemClicked(@NonNull TxListItem item);

        void onItemPauseClicked(@NonNull TxListItem item);
    }

    private static final DiffUtil.ItemCallback<TxListItem> diffCallback = new DiffUtil.ItemCallback<TxListItem>() {
        @Override
        public boolean areContentsTheSame(@NonNull TxListItem oldItem, @NonNull TxListItem newItem) {
            return oldItem.equalsContent(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull TxListItem oldItem, @NonNull TxListItem newItem) {
            return oldItem.equals(newItem);
        }
    };
}
