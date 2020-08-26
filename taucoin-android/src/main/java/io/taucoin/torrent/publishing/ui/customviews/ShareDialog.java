package io.taucoin.torrent.publishing.ui.customviews;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.databinding.ItemShareDialogBinding;
import io.taucoin.torrent.publishing.databinding.ShareDialogBinding;
import io.taucoin.torrent.publishing.ui.Selectable;

/**
 * 弹出对话框
 */
public class ShareDialog extends Dialog{

    private ShareDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    public static class Builder{
        private Context context;
        private List<ShareItem> items = new ArrayList<>();
        private OnItemClickListener listener;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder addItems(ShareItem item) {
            this.items.add(item);
            return this;
        }

        public Builder addItems(int imgRid, int titleRid) {
            this.items.add(new ShareItem(imgRid, titleRid));
            return this;
        }

        public Builder addItems(Drawable img, CharSequence title) {
            this.items.add(new ShareItem(img, title));
            return this;
        }

        public Builder setOnItemClickListener(OnItemClickListener listener) {
            this.listener = listener;
            return this;
        }

        public ShareDialog create(){
            ShareDialog popUpDialog = new ShareDialog(context, R.style.PopUpDialog);
            ShareDialogBinding binding = DataBindingUtil.inflate(LayoutInflater.from(context),
                    R.layout.share_dialog, null, false);
            LinearLayoutManager layoutManager = new LinearLayoutManager(context);
            layoutManager.setOrientation(RecyclerView.HORIZONTAL);
            binding.itemRecyclerView.setLayoutManager(layoutManager);
            ItemAdapter itemAdapter = new ItemAdapter(popUpDialog, items, listener);
            binding.itemRecyclerView.setAdapter(itemAdapter);
            View root = binding.getRoot();
            popUpDialog.setContentView(root);
            Window dialogWindow = popUpDialog.getWindow();
            if(dialogWindow != null){
                dialogWindow.setGravity(Gravity.BOTTOM);
                WindowManager.LayoutParams lp = dialogWindow.getAttributes();
                lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                dialogWindow.setAttributes(lp);
            }
            return popUpDialog;
        }
    }

    static class ItemAdapter extends ListAdapter<ShareItem, ItemAdapter.ViewHolder> implements Selectable<ShareItem> {
        private List<ShareItem> items;
        private OnItemClickListener listener;
        private ShareDialog popUpDialog;
        ItemAdapter(ShareDialog popUpDialog, List<ShareItem> items, OnItemClickListener listener) {
            super(diffCallback);
            this.popUpDialog = popUpDialog;
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            ItemShareDialogBinding binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_share_dialog,
                    parent,
                    false);
            return new ViewHolder(binding.getRoot(), binding);
        }

        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if(position >= 0 && position < items.size() && holder.binding != null){
                ShareItem item = items.get(position);
                holder.binding.tvShareTitle.setText(item.getTitle(holder.itemView.getContext()));
                if(null == item.img){
                    holder.binding.ivShareType.setImageResource(item.imgRid);
                }else{
                    holder.binding.ivShareType.setImageDrawable(item.img);
                }
                holder.itemView.setOnClickListener(v -> {
                    if(listener != null){
                        listener.onItemClick(popUpDialog, item.imgRid, item.titleRid);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return items != null ? items.size() : 0;
        }

        @Override
        public ShareItem getItemKey(int position) {
            if(position >= 0 && position < items.size()){
                return items.get(position);
            }
            return null;
        }

        @Override
        public int getItemPosition(ShareItem key) {
            return getCurrentList().indexOf(key);
        }

        static class ViewHolder extends RecyclerView.ViewHolder{
            ItemShareDialogBinding binding;
            ViewHolder(@NonNull View itemView, ItemShareDialogBinding binding) {
                super(itemView);
                this.binding = binding;
            }
        }

        private static final DiffUtil.ItemCallback<ShareItem> diffCallback = new DiffUtil.ItemCallback<ShareItem>() {
            @Override
            public boolean areContentsTheSame(@NonNull ShareItem oldItem, @NonNull ShareItem newItem) {
                return oldItem.equals(newItem);
            }

            @Override
            public boolean areItemsTheSame(@NonNull ShareItem oldItem, @NonNull ShareItem newItem) {
                return oldItem.equals(newItem);
            }
        };
    }

    static class ShareItem{
        int imgRid;
        Drawable img;
        private int titleRid;
        private CharSequence title;
        ShareItem(int imgRid, int titleRid){
            this.imgRid = imgRid;
            this.titleRid = titleRid;
        }

        public ShareItem(int imgRid, CharSequence title) {
            this.imgRid = imgRid;
            this.title = title;
        }

        public ShareItem(Drawable img, CharSequence title) {
            this.img = img;
            this.title = title;
        }

        public CharSequence getTitle(Context context) {
            if(StringUtil.isEmpty(title)){
                title = context.getString(titleRid);
            }
            return title;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            return o instanceof Tx && (o == this || imgRid == ((ShareItem)o).imgRid);
        }
    }

    public void closeDialog(){
        if(isShowing()){
            dismiss();
        }
    }

    public interface OnItemClickListener{
        void onItemClick(Dialog dialog, int imgRid, int titleRid);
    }
}
