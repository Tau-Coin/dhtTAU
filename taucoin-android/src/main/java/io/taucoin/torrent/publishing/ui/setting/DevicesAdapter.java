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
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Device;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.DeviceUtils;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.databinding.ItemDeviceBinding;

/**
 * 设备列表的Adapter
 */
public class DevicesAdapter extends ListAdapter<Device, DevicesAdapter.ViewHolder> {

    DevicesAdapter() {
        super(diffCallback);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemDeviceBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.item_device,
                parent,
                false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DevicesAdapter.ViewHolder holder, int position) {
        Device device = getItem(position);
        holder.bindDevice(device);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemDeviceBinding binding;
        private Context context;
        private String deviceID;

        ViewHolder(ItemDeviceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = binding.getRoot().getContext();
            deviceID = DeviceUtils.getCustomDeviceID(this.context);
        }

        /**
         * 绑定设备数据
         */
        void bindDevice(Device device) {
            if(null == device){
                return;
            }
            binding.tvName.setText(device.deviceID);
            binding.tvFirstLoginTime.setText(DateUtil.formatTime(device.loginTime, DateUtil.pattern6));
            boolean isCurrentDevice = StringUtil.isEquals(this.deviceID, device.deviceID);
            int bgColor = isCurrentDevice ? R.color.color_bg : R.color.color_white;
            binding.getRoot().setBackgroundColor(context.getResources().getColor(bgColor));
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
