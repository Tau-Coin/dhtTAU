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
import io.taucoin.torrent.publishing.core.storage.entity.Tx;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.databinding.ItemGroupListBinding;
import io.taucoin.torrent.publishing.ui.Selectable;

/**
 * 主页显示的群组列表的Adapter
 */
public class TxListAdapter extends ListAdapter<Tx, TxListAdapter.ViewHolder>
    implements Selectable<Tx> {
    private ClickListener listener;
    private List<Tx> dataList = new ArrayList<>();

    TxListAdapter(ClickListener listener) {
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
    public Tx getItemKey(int position) {
        return dataList.get(position);
    }

    @Override
    public int getItemPosition(Tx key) {
        return getCurrentList().indexOf(key);
    }

    /**
     * 设置列表交易展示数据
     * @param txs 社区数据
     */
    void setDataList(List<Tx> txs) {
        dataList.clear();
        dataList.addAll(txs);
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

        void bind(ViewHolder holder, Tx tx) {
            if(null == holder || null == tx){
                return;
            }
            String time = DateUtil.formatTime(DateUtil.getTime(), DateUtil.pattern0);
            holder.binding.tvMsgLastTime.setText(time);
            holder.binding.tvMsgNumber.setText("100");

//            holder.binding.tvGroupName.setText(Html.fromHtml(tx.communityName));
//            String firstLetters = StringUtil.getFirstLettersOfName(tx.communityName);
//            holder.binding.leftView.setText(firstLetters);
//            holder.binding.leftView.setBgColor(Utils.getGroupColor(firstLetters));
//
//            holder.binding.getRoot().setOnClickListener(v -> {
//                if(listener != null){
//                    listener.onItemClicked(tx);
//                }
//            });
        }
    }

    public interface ClickListener {
        void onItemClicked(Tx item);
    }

    private static final DiffUtil.ItemCallback<Tx> diffCallback = new DiffUtil.ItemCallback<Tx>() {
        @Override
        public boolean areContentsTheSame(@NonNull Tx oldItem, @NonNull Tx newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull Tx oldItem, @NonNull Tx newItem) {
            return oldItem.equals(newItem);
        }
    };
}
