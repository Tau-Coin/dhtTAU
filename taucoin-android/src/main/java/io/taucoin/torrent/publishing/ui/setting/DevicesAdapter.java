package io.taucoin.torrent.publishing.ui.setting;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Device;
import io.taucoin.torrent.publishing.databinding.ItemBlacklistBinding;

/**
 * 设备列表的Adapter
 */
public class DevicesAdapter extends ListAdapter<Device, DevicesAdapter.ViewHolder> {
    private List<Device> dataList = new ArrayList<>();

    DevicesAdapter() {
        super(diffCallback);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemBlacklistBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_blacklist,
                parent,
                false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DevicesAdapter.ViewHolder holder, int position) {
        Device device = getItem(position);
        holder.bindDevice(device);
    }

    /**
     * 设置设备列表数据
     * @param dataList 设备数据
     */
    void setDeviceList(List<Device> dataList) {
        this.dataList.clear();
        if(dataList != null){
            this.dataList.addAll(dataList);
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemBlacklistBinding binding;

        ViewHolder(ItemBlacklistBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * 绑定设备数据
         */
        void bindDevice(Device device) {
            if(null == device){
                return;
            }
            binding.tvName.setText(device.deviceID);
        }
    }

    private static final DiffUtil.ItemCallback<Device> diffCallback = new DiffUtil.ItemCallback<Device>() {
        @Override
        public boolean areContentsTheSame(@NonNull Device oldItem, @NonNull Device newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(@NonNull Device oldItem, @NonNull Device newItem) {
            return oldItem.equals(newItem);
        }
    };
}
