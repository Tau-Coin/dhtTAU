package io.taucoin.torrent.publishing.ui;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/*
 * The basic callback interface with codes and functions, returned by fragments.
 */

public interface FragmentCallback
{
    @SuppressWarnings("unused")
    String TAG = FragmentCallback.class.getSimpleName();

    enum ResultCode {
        OK, CANCEL, BACK
    }

    void onFragmentFinished(@NonNull Fragment f, Intent intent, @NonNull ResultCode code);
}