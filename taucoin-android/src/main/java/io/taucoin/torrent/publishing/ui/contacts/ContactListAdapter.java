package io.taucoin.torrent.publishing.ui.contacts;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.UserAndMember;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ItemContactListBinding;
import io.taucoin.torrent.publishing.ui.Selectable;

/**
 * 显示的联系人列表的Adapter
 */
public class ContactListAdapter extends ListAdapter<UserAndMember, ContactListAdapter.ViewHolder>
    implements Selectable<UserAndMember> {
    private ClickListener listener;
    private List<UserAndMember> dataList = new ArrayList<>();

    ContactListAdapter(ClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemContactListBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_contact_list,
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
    public UserAndMember getItemKey(int position) {
        return dataList.get(position);
    }

    @Override
    public int getItemPosition(UserAndMember key) {
        return getCurrentList().indexOf(key);
    }

    /**
     * 设置联系人展示数据
     * @param user 用户数据
     */
    void setDataList(List<UserAndMember> user) {
        dataList.clear();
        dataList.addAll(user);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemContactListBinding binding;
        private ClickListener listener;
        private Context context;

        ViewHolder(ItemContactListBinding binding, ClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = binding.getRoot().getContext();
            this.listener = listener;
        }

        void bind(ViewHolder holder, UserAndMember user) {
            if(null == holder || null == user){
                return;
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
            if(user.lastUpdateTime > 0){
                time = DateUtil.formatTime(user.lastUpdateTime, DateUtil.pattern5);
                time = context.getResources().getString(R.string.contacts_last_seen, time);
            }
            holder.binding.tvTime.setText(time);

            StringBuilder communities = new StringBuilder();
            if(user.members != null && user.members.size() > 0){
                List<Member> members = user.members;
                int size = members.size();
                for (int i = 0; i < size; i++) {
                    Member member = members.get(i);
//                    if(member.balance <=0 && member.power <= 0){
//                        continue;
//                    }
                    if(communities.length() == 0){
                        communities.append(context.getResources().getString(R.string.contacts_community));
                    }
                    String communityName = Utils.getCommunityName(member.chainID);
                    LoggerFactory.getLogger("1111").info("communityName::{}, chainID::{}", communityName, member.chainID);
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
        }
    }

    public interface ClickListener {
        void onItemClicked(User item);
    }

    private static final DiffUtil.ItemCallback<UserAndMember> diffCallback = new DiffUtil.ItemCallback<UserAndMember>() {
        @Override
        public boolean areContentsTheSame(@NonNull UserAndMember oldItem, @NonNull UserAndMember newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull UserAndMember oldItem, @NonNull UserAndMember newItem) {
            return oldItem.equals(newItem);
        }
    };
}