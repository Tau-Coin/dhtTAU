package io.taucoin.torrent.publishing.ui.transaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.ReplyAndTx;
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
public class TxListAdapter extends PagedListAdapter<ReplyAndTx, TxListAdapter.ViewHolder>
    implements Selectable<ReplyAndTx> {
    private ClickListener listener;

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
        }else if(viewType == MsgType.Wiring.getVaLue()){
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
    public ReplyAndTx getItemKey(int position) {
        if(getCurrentList() != null){
            return getCurrentList().get(position);
        }
        return null;
    }

    @Override
    public int getItemPosition(ReplyAndTx key) {
        if(getCurrentList() != null){
            return getCurrentList().indexOf(key);
        }
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        ReplyAndTx tx = getItemKey(position);
        if(tx != null){
            return tx.txType;
        }
        return -1;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewDataBinding binding;
        private ClickListener listener;

        ViewHolder(ViewDataBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(ViewHolder holder, ReplyAndTx tx) {
            if(null == binding || null == holder || null == tx){
                return;
            }
            if(binding instanceof ItemWiringTxBinding){
                ItemWiringTxBinding txBinding = (ItemWiringTxBinding) holder.binding;
                String time = DateUtil.formatTime(tx.timestamp, DateUtil.pattern0);
                txBinding.leftView.setBgColor(Utils.getGroupColor(tx.senderPk));
                String showName = UsersUtil.getShowName(tx);
                txBinding.leftView.setText(StringUtil.getFirstLettersOfName(showName));
                txBinding.tvName.setText(showName);
                String msg = tx.memo;
                msg = tx.txType + "--" + msg;
                txBinding.tvMsg.setText(msg);
                txBinding.tvTime.setText(time);

                if(tx.txType == MsgType.ForumComment.getVaLue()){
                    if(tx.replyTx != null){
                        showName = UsersUtil.getShowReplyName(tx);
                        msg = "@" + showName + "\n" + tx.replyTx.memo + "\n\n" + msg;
                    }
                    txBinding.tvMsg.setText(msg);
                }

                if(StringUtil.isEquals(tx.senderPk,
                        MainApplication.getInstance().getPublicKey())){
                    txBinding.tvBlacklist.setVisibility(View.INVISIBLE);
                }

                setOnLongClickListener(txBinding.middleView, tx, msg);
            }else{
                ItemWiringTxBinding txBinding = (ItemWiringTxBinding) holder.binding;
                String time = DateUtil.formatTime(tx.timestamp, DateUtil.pattern0);
                txBinding.leftView.setBgColor(Utils.getGroupColor(tx.senderPk));
                String showName = UsersUtil.getDefaultName(tx.senderPk);
                txBinding.leftView.setText(StringUtil.getFirstLettersOfName(showName));
                txBinding.tvName.setText(showName);
                String msg = tx.memo;
                txBinding.tvMsg.setText(msg);
                txBinding.tvTime.setText(time);
            }
            View root = binding.getRoot();
            root.setOnClickListener(v -> {
                if(listener != null){
                    listener.onItemClicked(tx);
                }
            });
        }

        private void setOnLongClickListener(View llMsg, ReplyAndTx tx, String msg) {
            llMsg.setOnLongClickListener(view ->{
                if(listener != null){
                    listener.onItemLongClicked(tx, msg);
                }
                return false;
            });
        }
    }

    public interface ClickListener {
        void onItemClicked(ReplyAndTx tx);
        void onItemLongClicked(ReplyAndTx tx, String msg);
    }

    private static final DiffUtil.ItemCallback<ReplyAndTx> diffCallback = new DiffUtil.ItemCallback<ReplyAndTx>() {
        @Override
        public boolean areContentsTheSame(@NonNull ReplyAndTx oldItem, @NonNull ReplyAndTx newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull ReplyAndTx oldItem, @NonNull ReplyAndTx newItem) {
            return oldItem.equals(newItem);
        }
    };
}
