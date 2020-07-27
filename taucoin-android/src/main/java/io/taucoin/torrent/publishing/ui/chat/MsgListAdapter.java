package io.taucoin.torrent.publishing.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Message;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ItemMsgBinding;
import io.taucoin.torrent.publishing.ui.Selectable;

/**
 * 消息/交易列表显示的Adapter
 */
public class MsgListAdapter extends ListAdapter<Message, MsgListAdapter.ViewHolder>
    implements Selectable<Message> {
    private ClickListener listener;
    private List<Message> dataList = new ArrayList<>();

    MsgListAdapter(ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemMsgBinding binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_msg,
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
    public Message getItemKey(int position) {
        return dataList.get(position);
    }

    @Override
    public int getItemPosition(Message key) {
        return getCurrentList().indexOf(key);
    }

    /**
     * 设置列表消息展示数据
     * @param msgList 消息数据
     */
    void setDataList(List<Message> msgList) {
        dataList.clear();
        dataList.addAll(msgList);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemMsgBinding binding;
        private ClickListener listener;

        ViewHolder(ItemMsgBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(ViewHolder holder, Message msg) {
            if(null == binding || null == holder || null == msg){
                return;
            }
            binding.leftView.setBgColor(Utils.getGroupColor(msg.senderPk));
            String showName = UsersUtil.getDefaultName(msg.senderPk);
            binding.leftView.setText(StringUtil.getFirstLettersOfName(showName));
            binding.tvName.setText(showName);
            binding.tvMsg.setText(msg.context);
            String time = DateUtil.formatTime(msg.timestamp, DateUtil.pattern0);
            binding.tvTime.setText(time);

            View root = binding.getRoot();
            root.setOnClickListener(v -> {
                if(listener != null){
                    listener.onItemClicked(msg);
                }
            });
            root.setOnLongClickListener(view -> {
                if(listener != null){
                    listener.onItemLongClicked(view, msg);
                }
                return false;
            });
        }
    }

    public interface ClickListener {
        void onItemClicked(Message tx);
        void onItemLongClicked(View view, Message tx);
    }

    private static final DiffUtil.ItemCallback<Message> diffCallback = new DiffUtil.ItemCallback<Message>() {
        @Override
        public boolean areContentsTheSame(@NonNull Message oldItem, @NonNull Message newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull Message oldItem, @NonNull Message newItem) {
            return oldItem.equals(newItem);
        }
    };
}
