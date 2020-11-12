package io.taucoin.torrent.publishing.ui.peers;

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
import io.taucoin.torrent.publishing.core.model.data.UserAndMember;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ItemContactListBinding;

/**
 * 显示的联系人列表的Adapter
 */
public class PeersListAdapter extends PagedListAdapter<UserAndMember, PeersListAdapter.ViewHolder> {
    private ClickListener listener;
    private List<String> selectedList = new ArrayList<>();
    private int page;
    private int order;

    PeersListAdapter(ClickListener listener, int type, int order) {
        super(diffCallback);
        this.listener = listener;
        this.page = type;
    }

    void setOrder(int order) {
        this.order = order;
        diffCallback.updateOrder(order);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemContactListBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_contact_list,
                parent,
                false);

        return new ViewHolder(binding, listener, page, selectedList);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItem(position), order);
    }

    List<String> getSelectedList() {
        return selectedList;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemContactListBinding binding;
        private ClickListener listener;
        private Context context;
        private int type;
        private List<String> selectedList;

        ViewHolder(ItemContactListBinding binding, ClickListener listener, int type,
                   List<String> selectedList) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = binding.getRoot().getContext();
            this.listener = listener;
            this.type = type;
            this.selectedList = selectedList;
        }

        void bind(ViewHolder holder, UserAndMember user, int order) {
            if(null == holder || null == user){
                return;
            }
            holder.binding.cbSelect.setVisibility(type == ConnectedPeersActivity.PAGE_ADD_MEMBERS
                    ? View.VISIBLE : View.GONE);
            holder.binding.ivShare.setVisibility(type == ConnectedPeersActivity.PAGE_ADD_MEMBERS
                    ? View.VISIBLE : View.GONE);
            if(type == ConnectedPeersActivity.PAGE_ADD_MEMBERS){
                holder.binding.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    selectedList.remove(user.publicKey);
                    if(isChecked){
                        selectedList.add(user.publicKey);
                    }
                    if(listener != null){
                        listener.onSelectClicked();
                    }
                });
                holder.binding.cbSelect.setChecked(selectedList.contains(user.publicKey));
            }
            String showName = UsersUtil.getDefaultName(user.publicKey);
            if(StringUtil.isNotEmpty(user.localName)
                    && StringUtil.isNotEquals(user.localName, showName)){
                showName = context.getString(R.string.user_show_name, user.localName, showName);
            }
            holder.binding.tvName.setText(showName);
            String firstLetters = StringUtil.getFirstLettersOfName(showName);
            holder.binding.leftView.setText(firstLetters);

            String time = "";
            if (order == 0 && user.lastUpdateTime > 0) {
                time = DateUtil.formatTime(user.lastUpdateTime, DateUtil.pattern5);
                time = context.getResources().getString(R.string.contacts_last_seen, time);
            } else if (order != 0 && user.lastCommTime > 0) {
                time = DateUtil.formatTime(user.lastCommTime, DateUtil.pattern5);
                time = context.getResources().getString(R.string.contacts_last_communication, time);
            }
            holder.binding.tvTime.setText(time);

            StringBuilder communities = new StringBuilder();
            if(user.members != null && user.members.size() > 0){
                List<Member> members = user.members;
                int size = members.size();
                for (int i = 0; i < size; i++) {
                    Member member = members.get(i);
                    if(member.balance <=0 && member.power <= 0){
                        continue;
                    }
                    if(communities.length() == 0){
                        communities.append(context.getResources().getString(R.string.contacts_community_from));
                    }
                    String communityName = Utils.getCommunityName(member.chainID);
                    String community = context.getResources().getString(R.string.contacts_community_more, communityName);
                    if(i == 0){
                        community = community.substring(1);
                    }
                    communities.append(community);
                }
            }
            holder.binding.tvCommunities.setText(communities.toString());

            int bgColor = Utils.getGroupColor(user.publicKey);
            holder.binding.leftView.setBgColor(bgColor);

            holder.binding.getRoot().setOnClickListener(v -> {
                if(listener != null){
                    listener.onItemClicked(user);
                }
            });
            holder.binding.ivShare.setOnClickListener(v -> {
                if(listener != null){
                    listener.onShareClicked(user);
                }
            });
        }
    }

    public interface ClickListener {
        void onItemClicked(UserAndMember item);
        void onSelectClicked();
        void onShareClicked(UserAndMember item);
    }

    static abstract class ItemCallback extends DiffUtil.ItemCallback<UserAndMember> {
        int oldOrder;
        int order;

        void updateOrder(int order) {
            this.oldOrder = this.order;
            this.order = order;

        }
    }

    private static final ItemCallback diffCallback = new ItemCallback() {
        @Override
        public boolean areContentsTheSame(@NonNull UserAndMember oldItem, @NonNull UserAndMember newItem) {
            return oldItem.equals(newItem) && oldOrder == order;
        }

        @Override
        public boolean areItemsTheSame(@NonNull UserAndMember oldItem, @NonNull UserAndMember newItem) {
            return oldItem.equals(newItem);
        }
    };
}
