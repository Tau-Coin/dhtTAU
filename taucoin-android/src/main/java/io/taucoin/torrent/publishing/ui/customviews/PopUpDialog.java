package io.taucoin.torrent.publishing.ui.customviews;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.databinding.ItemPopUpDialogListBinding;
import io.taucoin.torrent.publishing.databinding.PopUpDialogBinding;
import io.taucoin.torrent.publishing.ui.Selectable;

/**
 * 弹出对话框
 */
public class PopUpDialog extends Dialog{

    private PopUpDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    public static class Builder{
        private Context context;
        private List<ItemBean> items = new ArrayList<>();
        private OnItemClickListener listener;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder addItems(CharSequence name, int code) {
            this.items.add(new ItemBean(name, code));
            return this;
        }

        public Builder setOnItemClickListener(OnItemClickListener listener) {
            this.listener = listener;
            return this;
        }

        public PopUpDialog create(){
            PopUpDialog popUpDialog = new PopUpDialog(context, R.style.PopUpDialog);
            PopUpDialogBinding binding = DataBindingUtil.inflate(LayoutInflater.from(context),
                    R.layout.pop_up_dialog, null, false);
            LinearLayoutManager layoutManager = new LinearLayoutManager(context);
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

    static class ItemAdapter extends ListAdapter<ItemBean, ItemAdapter.ViewHolder> implements Selectable<ItemBean> {
        private List<ItemBean> items;
        private OnItemClickListener listener;
        private PopUpDialog popUpDialog;
        ItemAdapter(PopUpDialog popUpDialog, List<ItemBean> items, OnItemClickListener listener) {
            super(diffCallback);
            this.popUpDialog = popUpDialog;
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            ItemPopUpDialogListBinding binding = DataBindingUtil.inflate(inflater,
                    R.layout.item_pop_up_dialog_list,
                    parent,
                    false);
            return new ViewHolder(binding.getRoot(), binding);
        }

        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if(position >= 0 && position < items.size() && holder.binding != null){
                holder.binding.topLine.setVisibility(View.INVISIBLE);
                holder.binding.topLine.setVisibility(View.INVISIBLE);
                ItemBean itemBean = items.get(position);
                holder.binding.tvItemTitle.setText(itemBean.name);
                holder.itemView.setOnClickListener(v -> {
                    if(listener != null){
                        listener.onItemClick(popUpDialog, itemBean.name, itemBean.code);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return items != null ? items.size() : 0;
        }

        @Override
        public ItemBean getItemKey(int position) {
            if(position >=0 && position < items.size()){
                return items.get(position);
            }
            return null;
        }

        @Override
        public int getItemPosition(ItemBean key) {
            return getCurrentList().indexOf(key);
        }

        static class ViewHolder extends RecyclerView.ViewHolder{
            ItemPopUpDialogListBinding binding;
            ViewHolder(@NonNull View itemView, ItemPopUpDialogListBinding binding) {
                super(itemView);
                this.binding = binding;
            }
        }

        private static final DiffUtil.ItemCallback<ItemBean> diffCallback = new DiffUtil.ItemCallback<ItemBean>() {
            @Override
            public boolean areContentsTheSame(@NonNull ItemBean oldItem, @NonNull ItemBean newItem) {
                return oldItem.code == newItem.code;
            }

            @Override
            public boolean areItemsTheSame(@NonNull ItemBean oldItem, @NonNull ItemBean newItem) {
                return oldItem.code == newItem.code;
            }
        };
    }


    public void closeDialog(){
        if(isShowing()){
            dismiss();
        }
    }

    static class ItemBean{
        CharSequence name;
        int code;
        ItemBean(CharSequence name, int code){
            this.name = name;
            this.code = code;
        }
    }

    public interface OnItemClickListener{
        void onItemClick(Dialog dialog, CharSequence name, int code);
    }
}
