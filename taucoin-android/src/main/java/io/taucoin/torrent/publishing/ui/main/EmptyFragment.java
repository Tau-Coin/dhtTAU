package io.taucoin.torrent.publishing.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.databinding.FragmentEmptyBinding;
import io.taucoin.torrent.publishing.ui.BaseFragment;

/**
 * 群组列表页面
 */
public class EmptyFragment extends BaseFragment {

    private FragmentEmptyBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_empty, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        binding.toolbarRight.ivBack.setVisibility(View.GONE);
        binding.toolbarRight.ivAction.setVisibility(View.GONE);
    }
}