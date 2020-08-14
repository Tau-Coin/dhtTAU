package io.taucoin.torrent.publishing.ui.transaction;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.param.ChainParam;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ItemNoteBinding;
import io.taucoin.torrent.publishing.databinding.ItemWiringTxBinding;
import io.taucoin.torrent.publishing.databinding.MsgLeftViewBinding;
import io.taucoin.torrent.publishing.ui.Selectable;

/**
 * 消息/交易列表显示的Adapter
 */
public class TxListAdapter extends PagedListAdapter<UserAndTx, TxListAdapter.ViewHolder>
    implements Selectable<UserAndTx> {
    private ClickListener listener;
    private Community community;

    TxListAdapter(ClickListener listener, Community community) {
        super(diffCallback);
        this.listener = listener;
        this.community = community;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ViewDataBinding binding;
        if(viewType == ChainParam.TxType.WCoinsType.ordinal()){
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_wiring_tx,
                    parent,
                    false);
        }else {
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_note,
                    parent,
                    false);
        }
        return new ViewHolder(binding, listener, community);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItemKey(position));
    }

    @Override
    public UserAndTx getItemKey(int position) {
        if(getCurrentList() != null){
            return getCurrentList().get(position);
        }
        return null;
    }

    @Override
    public int getItemPosition(UserAndTx key) {
        if(getCurrentList() != null){
            return getCurrentList().indexOf(key);
        }
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        UserAndTx tx = getItemKey(position);
        if(tx != null){
            return (int) tx.txType;
        }
        return -1;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewDataBinding binding;
        private ClickListener listener;
        private Context context;
        private Community community;

        ViewHolder(ViewDataBinding binding, ClickListener listener, Community community) {
            super(binding.getRoot());
            this.context = binding.getRoot().getContext();
            this.binding = binding;
            this.listener = listener;
            this.community = community;
        }

        void bind(ViewHolder holder, UserAndTx tx) {
            if(null == binding || null == holder || null == tx || null == community){
                return;
            }
            String time = DateUtil.getWeekTime(tx.timestamp);
            int bgColor = Utils.getGroupColor(tx.senderPk);
            String showName = UsersUtil.getShowName(tx);
            SpannableStringBuilder memo = Utils.getSpannableStringUrl(tx.memo);
            String firstLettersName = StringUtil.getFirstLettersOfName(showName);
            String userName = UsersUtil.getUserName(tx);
            if(binding instanceof ItemWiringTxBinding){
                ItemWiringTxBinding txBinding = (ItemWiringTxBinding) holder.binding;
                txBinding.leftView.roundButton.setBgColor(bgColor);
                txBinding.leftView.roundButton.setText(firstLettersName);
                txBinding.leftView.tvEditName.setText(userName);
                if(tx.txStatus == 1){
                    txBinding.tvResult.setText(R.string.tx_result_successfully);
                    txBinding.tvResult.setTextColor(context.getResources().getColor(R.color.color_black));
                    txBinding.tvAmount.setTextColor(context.getResources().getColor(R.color.color_black));
                }else{
                    txBinding.tvResult.setText(R.string.tx_result_processing);
                    txBinding.tvResult.setTextColor(context.getResources().getColor(R.color.color_blue));
                    txBinding.tvAmount.setTextColor(context.getResources().getColor(R.color.color_blue));
                }
                String amount = FmtMicrometer.fmtBalance(tx.amount) + " " + UsersUtil.getCoinName(community);
                txBinding.tvAmount.setText(amount);
                txBinding.tvReceiver.setText(tx.receiverPk);
                txBinding.tvFee.setText(FmtMicrometer.fmtFeeValue(tx.fee));
                txBinding.tvHash.setText(tx.txID);
                txBinding.tvMemo.setText(memo);
                txBinding.tvTime.setText(time);

                if(StringUtil.isEquals(tx.senderPk,
                        MainApplication.getInstance().getPublicKey())){
                    txBinding.leftView.tvBlacklist.setVisibility(View.GONE);
                }
                setOnLongClickListener(txBinding.middleView, tx, tx.memo);
                setLeftViewClickListener(txBinding.leftView, tx);
            }else{
                ItemNoteBinding noteBinding = (ItemNoteBinding) holder.binding;
                noteBinding.leftView.roundButton.setBgColor(bgColor);
                noteBinding.leftView.roundButton.setText(firstLettersName);
                noteBinding.leftView.tvEditName.setText(userName);
                noteBinding.tvName.setText(showName);
                noteBinding.tvMsg.setText(memo);
                noteBinding.tvTime.setText(time);

                if(StringUtil.isEquals(tx.senderPk,
                        MainApplication.getInstance().getPublicKey())){
                    noteBinding.leftView.tvBlacklist.setVisibility(View.GONE);
                }
                setOnLongClickListener(noteBinding.middleView, tx, tx.memo);
                setLeftViewClickListener(noteBinding.leftView, tx);
            }
        }

        private void setLeftViewClickListener(MsgLeftViewBinding binding, UserAndTx tx) {
            binding.roundButton.setOnClickListener(view ->{
                if(listener != null){
                    listener.onUserClicked(tx.senderPk);
                }
            });
            binding.tvEditName.setOnClickListener(view ->{
                if(listener != null){
                    listener.onEditNameClicked(tx);
                }
            });
            binding.tvBlacklist.setOnClickListener(view ->{
                if(listener != null){
                    listener.onBanClicked(tx);
                }
            });
        }

        private void setOnLongClickListener(View llMsg, UserAndTx tx, String msg) {
            llMsg.setOnLongClickListener(view ->{
                if(listener != null){
                    listener.onItemLongClicked(tx, msg);
                }
                return false;
            });
        }
    }

    public interface ClickListener {
        void onUserClicked(String publicKey);
        void onEditNameClicked(UserAndTx tx);
        void onBanClicked(UserAndTx tx);
        void onItemLongClicked(UserAndTx tx, String msg);
    }

    private static final DiffUtil.ItemCallback<UserAndTx> diffCallback = new DiffUtil.ItemCallback<UserAndTx>() {
        @Override
        public boolean areContentsTheSame(@NonNull UserAndTx oldItem, @NonNull UserAndTx newItem) {
            return oldItem.equals(newItem);
        }
        @Override
        public boolean areItemsTheSame(@NonNull UserAndTx oldItem, @NonNull UserAndTx newItem) {
            return oldItem.equals(newItem);
        }
    };
}
