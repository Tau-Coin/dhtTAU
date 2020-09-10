package io.taucoin.torrent.publishing.ui.setting;

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
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.FavoriteAndUser;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ItemFavoriteBinding;
import io.taucoin.torrent.publishing.databinding.ItemFavoriteWiringBinding;
import io.taucoin.types.TypesConfig;

/**
 * 收藏列表显示的Adapter
 */
public class FavoriteListAdapter extends PagedListAdapter<FavoriteAndUser,
        FavoriteListAdapter.ViewHolder> {

    FavoriteListAdapter() {
        super(diffCallback);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ViewDataBinding binding;
        if(viewType == TypesConfig.TxType.WCoinsType.ordinal()){
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_favorite_wiring,
                    parent,
                    false);
        }else {
            binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_favorite,
                    parent,
                    false);
        }
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItem(position));
    }

    @Override
    public int getItemViewType(int position) {
        FavoriteAndUser tx = getItem(position);
        if(tx != null){
            return (int) tx.type;
        }
        return -1;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewDataBinding binding;
        private Context context;

        ViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());
            this.context = binding.getRoot().getContext();
            this.binding = binding;
        }

        void bind(ViewHolder holder, FavoriteAndUser favorite) {
            if(null == binding || null == holder || null == favorite){
                return;
            }
            String time = DateUtil.getWeekTime(favorite.timestamp);
            int bgColor = Utils.getGroupColor(favorite.senderPk);
            SpannableStringBuilder memo = Utils.getSpannableStringUrl(favorite.memo);
            String userName = UsersUtil.getUserName(favorite.sender, favorite.senderPk);
            String firstLettersName = StringUtil.getFirstLettersOfName(userName);
            String communityName = UsersUtil.getCommunityName(favorite.chainID);
            if(binding instanceof ItemFavoriteWiringBinding){
                ItemFavoriteWiringBinding txBinding = (ItemFavoriteWiringBinding) holder.binding;
                txBinding.roundButton.setBgColor(bgColor);
                txBinding.roundButton.setText(firstLettersName);
                txBinding.tvUserName.setText(userName);
                txBinding.tvCommunityName.setText(communityName);
                String amount = FmtMicrometer.fmtBalance(favorite.amount) + " " + UsersUtil.getCoinName(favorite.chainID);
                txBinding.tvAmount.setText(amount);
                txBinding.tvReceiver.setText(favorite.receiverPk);
                txBinding.tvFee.setText(FmtMicrometer.fmtFeeValue(favorite.fee));
                txBinding.tvHash.setText(favorite.ID);
                txBinding.tvMemo.setText(memo);
                txBinding.tvTime.setText(time);

            }else{
                ItemFavoriteBinding favoriteBinding = (ItemFavoriteBinding) holder.binding;
                favoriteBinding.roundButton.setBgColor(bgColor);
                favoriteBinding.roundButton.setText(firstLettersName);
                favoriteBinding.tvUserName.setText(userName);
                favoriteBinding.tvCommunityName.setText(communityName);
                favoriteBinding.tvMsg.setText(memo);
                favoriteBinding.tvTime.setText(time);

                if(favorite.reply != null){
                    favoriteBinding.llReply.setVisibility(View.VISIBLE);
                    String replyName = UsersUtil.getShowName(favorite.reply.senderPk, favorite.replyName);
                    favoriteBinding.tvReplyName.setText(replyName);
                    favoriteBinding.tvReplyMsg.setText(favorite.reply.memo);
                }else{
                    favoriteBinding.llReply.setVisibility(View.GONE);
                }
            }
        }
    }

    private static final DiffUtil.ItemCallback<FavoriteAndUser> diffCallback = new DiffUtil.ItemCallback<FavoriteAndUser>() {
        @Override
        public boolean areContentsTheSame(@NonNull FavoriteAndUser oldItem, @NonNull FavoriteAndUser newItem) {
            return oldItem.equals(newItem);
        }
        @Override
        public boolean areItemsTheSame(@NonNull FavoriteAndUser oldItem, @NonNull FavoriteAndUser newItem) {
            return oldItem.equals(newItem);
        }
    };
}
