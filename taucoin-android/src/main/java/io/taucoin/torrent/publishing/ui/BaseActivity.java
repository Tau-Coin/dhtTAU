package io.taucoin.torrent.publishing.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import io.taucoin.torrent.publishing.core.utils.Utils;

public abstract class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.getAppTheme(getApplicationContext()));
        super.onCreate(savedInstanceState);
    }
}
