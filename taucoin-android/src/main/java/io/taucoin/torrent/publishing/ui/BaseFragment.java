package io.taucoin.torrent.publishing.ui;

import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public abstract class BaseFragment extends Fragment implements
        SwipeRefreshLayout.OnRefreshListener {

    @Override
    public void onRefresh() {

    }
}
