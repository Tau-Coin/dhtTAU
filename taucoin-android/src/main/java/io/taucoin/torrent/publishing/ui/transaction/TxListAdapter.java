package io.taucoin.torrent.publishing.ui.transaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.entity.Tx;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ItemWiringTxBinding;
import io.taucoin.torrent.publishing.ui.Selectable;
import io.taucoin.types.MsgType;

/**
 * 消息/交易列表显示的Adapter
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
        ViewDataBinding binding;
        if(viewType == MsgType.RegularForum.getVaLue()){
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_wiring_tx,
                    parent,
                    false);
        }else if(viewType == MsgType.ForumComment.getVaLue()){
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_wiring_tx,
                    parent,
                    false);
        }else if(viewType == MsgType.CommunityAnnouncement.getVaLue()){
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_wiring_tx,
                    parent,
                    false);
        }else if(viewType == MsgType.DHTBootStrapNodeAnnouncement.getVaLue()){
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_wiring_tx,
                    parent,
                    false);
        }else if(viewType == MsgType.Wiring.getVaLue()){
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_wiring_tx,
                    parent,
                    false);
        }else if(viewType == MsgType.IdentityAnnouncement.getVaLue()){
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_wiring_tx,
                    parent,
                    false);
        }else {
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_wiring_tx,
                    parent,
                    false);
        }
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

    @Override
    public int getItemViewType(int position) {
        Tx tx = getItemKey(position);
        return tx.txType;
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
        private ViewDataBinding binding;
        private ClickListener listener;

        ViewHolder(ViewDataBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(ViewHolder holder, Tx tx) {
            if(null == binding || null == holder || null == tx){
                return;
            }
            String msg = "";
            if(binding instanceof ItemWiringTxBinding){
                ItemWiringTxBinding txBinding = (ItemWiringTxBinding) holder.binding;
                String time = DateUtil.formatTime(tx.timestamp, DateUtil.pattern0);
                txBinding.leftView.setBgColor(Utils.getGroupColor(tx.senderPk));
                String showName = UsersUtil.getDefaultName(tx.senderPk);
                txBinding.leftView.setText(StringUtil.getFirstLettersOfName(showName));
                txBinding.tvName.setText(showName);
                msg = tx.memo;
                txBinding.tvMsg.setText(msg);
                txBinding.tvTime.setText(time);
            }else{
                ItemWiringTxBinding txBinding = (ItemWiringTxBinding) holder.binding;
                String time = DateUtil.formatTime(tx.timestamp, DateUtil.pattern0);
                txBinding.leftView.setBgColor(Utils.getGroupColor(tx.senderPk));
                String showName = UsersUtil.getDefaultName(tx.senderPk);
                txBinding.leftView.setText(StringUtil.getFirstLettersOfName(showName));
                txBinding.tvName.setText(showName);
                msg = tx.memo;
                txBinding.tvMsg.setText(msg);
                txBinding.tvTime.setText(time);
            }
            View root = binding.getRoot();
            root.setOnClickListener(v -> {
                if(listener != null){
                    listener.onItemClicked(tx);
                }
            });
            String finalMsg = msg;
            root.setOnLongClickListener(view -> {
                if(listener != null){
                    listener.onItemLongClicked(tx, finalMsg);
                }
                return false;
            });
        }
    }

    public interface ClickListener {
        void onItemClicked(Tx tx);
        void onItemLongClicked(Tx tx, String msg);
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
