package io.taucoin.torrent.publishing.ui.notify;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.NotificationAndUser;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ItemNotifyListBinding;

/**
 * 显示的联系人列表的Adapter
 */
public class NotificationAdapter extends PagedListAdapter<NotificationAndUser,
        NotificationAdapter.ViewHolder> {
    private ClickListener listener;
    private List<NotificationAndUser> selectedList = new ArrayList<>();
    private static boolean isEdit;

    NotificationAdapter(ClickListener listener, boolean isEdit) {
        super(diffCallback);
        this.listener = listener;
        NotificationAdapter.isEdit = isEdit;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemNotifyListBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_notify_list,
                parent,
                false);

        return new ViewHolder(binding, listener, selectedList);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItem(position));
    }

    List<NotificationAndUser> getSelectedList() {
        return selectedList;
    }

    void setEdit(boolean isEdit) {
        NotificationAdapter.isEdit = isEdit;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemNotifyListBinding binding;
        private ClickListener listener;
        private Context context;
        private List<NotificationAndUser> selectedList;

        ViewHolder(ItemNotifyListBinding binding, ClickListener listener,
                   List<NotificationAndUser> selectedList) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = binding.getRoot().getContext();
            this.listener = listener;
            this.selectedList = selectedList;
        }

        void bind(ViewHolder holder, NotificationAndUser notify) {
            if(null == holder || null == notify){
                return;
            }
            holder.binding.cbSelect.setVisibility(isEdit ? View.VISIBLE : View.GONE);
            if(isEdit){
                holder.binding.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    selectedList.remove(notify);
                    if(isChecked){
                        selectedList.add(notify);
                    }
                });
                holder.binding.cbSelect.setChecked(selectedList.contains(notify));
            }
            String showName = UsersUtil.getUserName(notify.user, notify.senderPk);
            showName = context.getString(R.string.notifications_from, showName);
            holder.binding.tvUserName.setText(showName);

            String communityName = UsersUtil.getCommunityName(notify.chainID);
            holder.binding.tvCommunityName.setText(communityName);

            String firstLetters = StringUtil.getFirstLettersOfName(communityName);
            holder.binding.leftView.setText(firstLetters);
            holder.binding.leftView.setBgColor(Utils.getGroupColor(notify.senderPk));

            holder.binding.btnJoin.setVisibility(null == notify.community ? View.VISIBLE : View.GONE);
            if(null == notify.community){
                holder.binding.btnJoin.setOnClickListener(v -> {
                    if(listener != null){
                        listener.onJoinClicked(notify);
                    }
                });
            }
        }
    }

    public interface ClickListener {
        void onJoinClicked(NotificationAndUser notify);
    }

    private static final DiffUtil.ItemCallback<NotificationAndUser> diffCallback = new DiffUtil.ItemCallback<NotificationAndUser>() {
        @Override
        public boolean areContentsTheSame(@NonNull NotificationAndUser oldItem, @NonNull NotificationAndUser newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull NotificationAndUser oldItem, @NonNull NotificationAndUser newItem) {
            return oldItem.equals(newItem);
        }
    };
}
