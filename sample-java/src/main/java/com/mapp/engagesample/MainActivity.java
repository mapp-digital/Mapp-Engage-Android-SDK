package com.mapp.engagesample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultCallback;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.appoxee.Appoxee;
import com.appoxee.shared.AppoxeeObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import eu.brrm.shared_ui.PermissionHelper;
import eu.brrm.shared_ui.Util;
import eu.brrm.shared_ui.databinding.ActivityMainBinding;

/**
 * @noinspection ConstantValue, FieldCanBeLocal, RedundantSuppression
 */
@SuppressLint("UseSwitchCompatOrMaterialCode")
public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    private PermissionHelper permissionHelper;

    private final OnBackPressedCallback onBackCallback = new OnBackPressedCallback(getSupportFragmentManager().getBackStackEntryCount() <= 1) {
        @Override
        public void handleOnBackPressed() {
            if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
                getSupportFragmentManager().popBackStack();
            } else {
                finish();
            }
        }
    };

    private final FragmentManager.OnBackStackChangedListener onBackStackChangedListener = () -> {
        int size = getSupportFragmentManager().getBackStackEntryCount();
        String title = getSupportFragmentManager().getBackStackEntryAt(size - 1).getName();
        Objects.requireNonNull(getSupportActionBar()).setTitle(Util.camelCaseToWords(title));
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(size > 1);
        onBackCallback.setEnabled(size <= 1);
    };

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private final ActivityResultCallback<Map<String, Boolean>> postNotificationPermissionCallback = (result) -> {
        String permission = Manifest.permission.POST_NOTIFICATIONS;
        if (Boolean.TRUE.equals(result.get(permission))) {
//            Toast.makeText(
//                    this,
//                    "Permission(s) granted: \n" + Util.permissionsToString(result),
//                    Toast.LENGTH_SHORT
//            ).show();
            binding.topPanel.setVisibility(View.GONE);
        } else {
            binding.topPanel.setVisibility(View.VISIBLE);
            binding.btnOpenSettings.setOnClickListener(v -> {
                permissionHelper.openApplicationSettings(MainActivity.this);
            });
        }
    };

    private final AppoxeeObserver appoxeeObserver = (status, mappResult) -> {
        if (!mappResult.isSuccess()) {
            Throwable error = mappResult.getError();
            String errorMessage = error != null ? error.getMessage() : "Unknown message";
            Util.showDialog(MainActivity.this, "Error", errorMessage);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        permissionHelper = new PermissionHelper(getActivityResultRegistry());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getOnBackPressedDispatcher().addCallback(this, onBackCallback);
        getSupportFragmentManager().addOnBackStackChangedListener(onBackStackChangedListener);
        navigate(new HomeFragment());
        Appoxee.instance().setPushBroadcast(MyPushBroadcast.class);
        Appoxee.instance().subscribe(appoxeeObserver);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPostNotificationPermission();
        }
    }

    public <T extends Fragment> void navigate(T fragment) {
        getSupportFragmentManager().beginTransaction()
                .addToBackStack(fragment.getClass().getSimpleName())
                .replace(binding.fragmentContainerView.getId(), fragment)
                .commit();
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void requestPostNotificationPermission() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        permissionHelper.requestPermissions(this, permissions, postNotificationPermissionCallback);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackCallback.handleOnBackPressed();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        getSupportFragmentManager().removeOnBackStackChangedListener(onBackStackChangedListener);
        super.onDestroy();
    }
}