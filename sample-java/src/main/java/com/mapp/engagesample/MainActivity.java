package com.mapp.engagesample;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.appoxee.Appoxee;
import com.appoxee.shared.AppoxeeObserver;
@SuppressLint("UseSwitchCompatOrMaterialCode")
public class MainActivity extends AppCompatActivity implements AppoxeeObserver {

    private static final String TAG = MainActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Appoxee.instance().subscribe(this);
    }

    @Override
    public void onReadyStatusChanged(boolean status) {
        Switch switchReady = findViewById(R.id.switchReady);
        switchReady.setEnabled(false);
        switchReady.setChecked(status);

        if (status) {
            Appoxee.instance().getDevice(mappResult -> {
                if (mappResult.isSuccess()) {
                    Log.d(TAG, "SUCCESS IN MAIN ACTIVITY: " + mappResult.getData());
                    if (mappResult.getData() != null) {
                        TextView tvDevice = findViewById(R.id.tvDevice);
                        tvDevice.setText(mappResult.getData().toString());
                    }
                } else {
                    Log.e(TAG, "ERROR IN MAIN ACTIVITY: " + mappResult.getError());
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        Appoxee.instance().unsubscribe(this);
        super.onDestroy();
    }
}