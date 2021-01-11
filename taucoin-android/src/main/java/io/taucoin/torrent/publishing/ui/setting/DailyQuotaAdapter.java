package io.taucoin.torrent.publishing.ui.setting;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.databinding.ItemDailyQuotaBinding;

/**
 * 每日数据流量定额Adapter
 */
public class DailyQuotaAdapter extends ListAdapter<Integer, DailyQuotaAdapter.ViewHolder> {
    static final int TYPE_METERED = 0x01;
    static final int TYPE_WIFI = 0x02;
    private int selectLimit;
    private int type;
    private OnCheckedChangeListener listener;
    DailyQuotaAdapter(OnCheckedChangeListener listener, int type, int limit) {
        super(diffCallback);
        this.selectLimit = limit;
        this.listener = listener;
        this.type = type;
    }

    public void updateSelectLimit(int selectLimit) {
        this.selectLimit = selectLimit;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemDailyQuotaBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_daily_quota,
                parent,
                false);

        return new ViewHolder(binding, this);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holder, getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemDailyQuotaBinding binding;
        private Context context;
        private DailyQuotaAdapter adapter;

        ViewHolder(ItemDailyQuotaBinding binding, DailyQuotaAdapter adapter) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = binding.getRoot().getContext();
            this.adapter = adapter;
        }

        void bind(ViewHolder holder, Integer limit) {
            if(null == holder){
                return;
            }
            String limitStr;
            if (limit >= 1024) {
                limitStr = context.getString(R.string.setting_daily_quota_unit_g, limit / 1024);
            } else {
                limitStr = context.getString(R.string.setting_daily_quota_unit_m, limit);
            }
            holder.binding.radioButton.setText(limitStr);
            holder.binding.radioButton.setChecked(adapter.selectLimit == limit);
            holder.binding.radioButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (adapter.listener != null) {
                        adapter.listener.onCheckedChanged(adapter.type, limit);
                    }
                }
            });
        }
    }

    public interface OnCheckedChangeListener {
        void onCheckedChanged(int type, int limit);
    }

    private static final DiffUtil.ItemCallback<Integer> diffCallback = new DiffUtil.ItemCallback<Integer>() {
        @Override
        public boolean areContentsTheSame(@NonNull Integer oldItem, @NonNull Integer newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull Integer oldItem, @NonNull Integer newItem) {
            return oldItem.equals(newItem);
        }
    };
}
