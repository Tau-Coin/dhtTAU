package io.taucoin.torrent.publishing.ui.main;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

public class MainViewModel extends AndroidViewModel {

    public MainViewModel(@NonNull Application application) {
        super(application);
    }

    void pauseAll() {

    }

    void resumeAll() {

    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}
