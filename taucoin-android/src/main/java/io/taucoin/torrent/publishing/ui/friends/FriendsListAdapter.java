package io.taucoin.torrent.publishing.ui.friends;

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
import io.taucoin.torrent.publishing.core.model.data.UserAndFriend;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.SpanUtils;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ItemFriendListBinding;

/**
 * 显示的联系人列表的Adapter
 */
public class FriendsListAdapter extends PagedListAdapter<UserAndFriend, FriendsListAdapter.ViewHolder> {
    private ClickListener listener;
    private List<String> selectedList = new ArrayList<>();
    private int page;
    private int order;
    private String friendPk;

    FriendsListAdapter(ClickListener listener, int type, int order, String friendPk) {
        super(diffCallback);
        this.listener = listener;
        this.page = type;
        this.order = order;
        this.friendPk = friendPk;
    }

    void setOrder(int order) {
        this.order = order;
        diffCallback.updateOrder(order);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemFriendListBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_friend_list,
                parent,
                false);

        return new ViewHolder(binding, listener, page, selectedList, friendPk);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItem(position), order);
    }

    List<String> getSelectedList() {
        return selectedList;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemFriendListBinding binding;
        private ClickListener listener;
        private Context context;
        private int type;
        private List<String> selectedList;
        private String friendPk;

        ViewHolder(ItemFriendListBinding binding, ClickListener listener, int type,
                   List<String> selectedList, String friendPk) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = binding.getRoot().getContext();
            this.listener = listener;
            this.type = type;
            this.selectedList = selectedList;
            this.friendPk = friendPk;
        }

        void bind(ViewHolder holder, UserAndFriend user, int order) {
            if(null == holder || null == user){
                return;
            }
            holder.binding.cbSelect.setVisibility(type == FriendsActivity.PAGE_ADD_MEMBERS
                    ? View.VISIBLE : View.GONE);
            holder.binding.ivShare.setVisibility(type == FriendsActivity.PAGE_ADD_MEMBERS
                    ? View.VISIBLE : View.GONE);
            holder.binding.ivShare.setVisibility(type == FriendsActivity.PAGE_ADD_MEMBERS
                    ? View.VISIBLE : View.GONE);
            if(type == FriendsActivity.PAGE_ADD_MEMBERS){
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
            String showName = UsersUtil.getShowNameWithYourself(user, user.publicKey);
            SpanUtils showNameBuilder = new SpanUtils()
                    .append(showName);
            int stateProgress = 100;
            if (user.isDiscovered()) {
                stateProgress = 0;
                showNameBuilder.append(" ")
                    .append(context.getString(R.string.contacts_discovered))
                    .setForegroundColor(context.getResources().getColor(R.color.color_blue))
                    .setFontSize(12, true);
            } else if (user.isAdded()) {
                stateProgress = 50;
                showNameBuilder.append(" ")
                    .append(context.getString(R.string.contacts_added))
                    .setForegroundColor(context.getResources().getColor(R.color.color_blue))
                        .setFontSize(12, true);
            }
            holder.binding.tvName.setText(showNameBuilder.create());
            if (type == FriendsActivity.PAGE_FRIENDS_LIST) {
                holder.binding.stateProgress.setVisibility(View.VISIBLE);
                holder.binding.stateProgress.setProgress(stateProgress);
            }
            String firstLetters = StringUtil.getFirstLettersOfName(showName);
            holder.binding.leftView.setText(firstLetters);

            String time = "";
            if (order == 0 && user.lastSeenTime > 0) {
                time = DateUtil.formatTime(user.lastSeenTime, DateUtil.pattern6);
                time = context.getResources().getString(R.string.contacts_last_seen, time);
            } else if (order != 0 && user.lastCommTime > 0) {
                time = DateUtil.formatTime(user.lastCommTime, DateUtil.pattern6);
                time = context.getResources().getString(R.string.contacts_last_communication, time);
            }
            holder.binding.tvTime.setVisibility(StringUtil.isEmpty(time) ? View.GONE : View.VISIBLE);
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
            holder.binding.tvCommunities.setVisibility(communities.length() == 0
                    ? View.GONE : View.VISIBLE);
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
            holder.binding.stateProgress.setOnClickListener(v -> {
                if(listener != null){
                    listener.onProcessClicked(user);
                }
            });
            // 新朋友高亮显示
            boolean isNewScanFriend = StringUtil.isEquals(friendPk, user.publicKey);
            bgColor = isNewScanFriend ? R.color.color_bg : R.color.color_white;
            holder.binding.getRoot().setBackgroundColor(context.getResources().getColor(bgColor));
        }
    }

    public interface ClickListener {
        void onItemClicked(UserAndFriend item);
        void onSelectClicked();
        void onShareClicked(UserAndFriend item);
        void onProcessClicked(UserAndFriend item);
    }

    static abstract class ItemCallback extends DiffUtil.ItemCallback<UserAndFriend> {
        int oldOrder;
        int order;

        void updateOrder(int order) {
            this.oldOrder = this.order;
            this.order = order;

        }
    }

    private static final ItemCallback diffCallback = new ItemCallback() {
        @Override
        public boolean areContentsTheSame(@NonNull UserAndFriend oldItem, @NonNull UserAndFriend newItem) {
            return oldItem.equals(newItem) && oldOrder == order &&
                    oldItem.status == newItem.status &&
                    ((order == 0 && oldItem.lastSeenTime == newItem.lastSeenTime) ||
                            (order == 1 && oldItem.lastCommTime == newItem.lastCommTime));
        }

        @Override
        public boolean areItemsTheSame(@NonNull UserAndFriend oldItem, @NonNull UserAndFriend newItem) {
            return oldItem.equals(newItem);
        }
    };
}
