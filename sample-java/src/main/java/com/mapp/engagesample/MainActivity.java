package com.mapp.engagesample;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.appoxee.Appoxee;
import com.appoxee.internal.model.response.DevicePayload;
import com.appoxee.shared.AppoxeeObserver;
import com.appoxee.shared.MappResult;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * @noinspection ConstantValue
 */
@SuppressLint("UseSwitchCompatOrMaterialCode")
public class MainActivity extends AppCompatActivity implements AppoxeeObserver {

    private static final String TAG = MainActivity.class.getName();

    private final String alias = "abc1@maptest.com";

    private Switch switchReady;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        switchReady = findViewById(R.id.switchReady);
        switchReady.setEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Appoxee.instance().subscribe(this);
    }

    @Override
    public void onReadyStatusChanged(boolean status, MappResult<DevicePayload> mappResult) {
        Log.d(TAG, "SUCCESS IN MAIN ACTIVITY - Is Ready: " + status + "; Payload: " + mappResult.getData() + "; Error: " + mappResult.getError());
        if (status) {
            updateUI(status, mappResult.getData());
        }
    }

    private void updateUI(boolean status, @Nullable DevicePayload payload) {
        Log.d(TAG, "UI Updating - Is Ready: " + status + "; Payload: " + payload);

        TextView tvDevice = findViewById(R.id.tvDevice);
        StringBuilder sb = new StringBuilder();
        if (payload != null) {
            sb.append("UDIDHashed: ").append("\n").append(payload.getUdidHashed()).append("\n\n")
                .append("DmcUserId: ").append("\n").append(payload.getDmcUserId()).append("\n\n")
                .append("OptIn token: ").append("\n").append(payload.getPushToken()).append("\n\n")
                .append("OptOut token: ").append("\n").append(payload.getPushTokenBk()).append("\n\n")
                .append("Alias: ").append("\n").append(payload.getAlias()).append("\n\n");
        }
        getWindow().getDecorView().post(() -> {
            switchReady.setChecked(status);
            tvDevice.setText(sb.toString());
        });
    }

    private void setAlias() {
        Appoxee.instance().setAlias(alias, result -> {
            getDevice();
        });
    }

    private void optIn() {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                String token = task.getResult();
                Log.i(TAG, "PUSH TOKEN FROM APP: " + token);
                Appoxee.instance().optIn(token, result -> {
                    getDevice();
                });
            });
    }

    private void getDevice() {
        Appoxee.instance().getDevice(mappResult -> {
            if (mappResult.isSuccess()) {
                DevicePayload payload = mappResult.getData();
                boolean ready = Appoxee.instance().isReady();
                updateUI(ready, payload);
            } else {
                Log.e(TAG, "ERROR IN MAIN ACTIVITY: " + mappResult.getError());
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Appoxee.instance().unsubscribe(this);
    }
}