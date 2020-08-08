package io.taucoin.torrent.publishing.ui.contacts;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.UserAndMember;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.databinding.FragmentContactsBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.BaseFragment;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * 联系人页面
 */
public class ContactsFragment extends BaseFragment implements ContactListAdapter.ClickListener{
    private BaseActivity activity;
    private UserViewModel viewModel;
    private FragmentContactsBinding binding;
    private ContactListAdapter adapter;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_contacts, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (BaseActivity) getActivity();
        ViewModelProvider provider = new ViewModelProvider(activity);
        viewModel = provider.get(UserViewModel.class);
        initView();
        initFabSpeedDial();
    }

    private void initView() {
        adapter = new ContactListAdapter(this);
//        /*
//         * A RecyclerView by default creates another copy of the ViewHolder in order to
//         * fade the views into each other. This causes the problem because the old ViewHolder gets
//         * the payload but then the new one doesn't. So needs to explicitly tell it to reuse the old one.
//         */
        DefaultItemAnimator animator = new DefaultItemAnimator() {
            @Override
            public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
                return true;
            }
        };
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        binding.recyclerList.setLayoutManager(layoutManager);
        binding.recyclerList.setItemAnimator(animator);
        binding.recyclerList.setEmptyView(binding.emptyViewList);
        binding.recyclerList.setAdapter(adapter);
    }

    /**
     * 初始化右下角悬浮按钮组件
     */
    private void initFabSpeedDial() {
        // 自定义点击事件
        binding.fabButton.getMainFab().setOnClickListener(v ->{

        });
    }

    private void subscribeUserViewModel() {
        disposables.add(viewModel.observeUsersNotInBanList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showUserList));
    }

    private void showUserList(List<UserAndMember> users) {
        if(users != null){
            adapter.setDataList(users);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof BaseActivity)
            activity = (BaseActivity)context;
    }

    @Override
    public void onStart() {
        super.onStart();
        subscribeUserViewModel();
    }

    @Override
    public void onStop() {
        super.onStop();
        disposables.clear();
    }

    @Override
    public void onItemClicked(@NonNull User item) {

    }

}