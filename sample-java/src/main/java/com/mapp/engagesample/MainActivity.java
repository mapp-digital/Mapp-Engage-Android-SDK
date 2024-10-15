package com.mapp.engagesample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.appoxee.Appoxee;
import com.appoxee.shared.AppoxeeObserver;

import java.util.ArrayList;
import java.util.List;
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

    private final AppoxeeObserver appoxeeObserver = (status, mappResult) -> {
        if (status && mappResult.isSuccess()) {
            requestPostNotificationPermission();
        }
    };

    private final FragmentManager.OnBackStackChangedListener onBackStackChangedListener = () -> {
        int size = getSupportFragmentManager().getBackStackEntryCount();
        String title = getSupportFragmentManager().getBackStackEntryAt(size - 1).getName();
        Objects.requireNonNull(getSupportActionBar()).setTitle(Util.camelCaseToWords(title));
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(size > 1);
        onBackCallback.setEnabled(size <= 1);
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getOnBackPressedDispatcher().addCallback(this, onBackCallback);
        getSupportFragmentManager().addOnBackStackChangedListener(onBackStackChangedListener);
        Appoxee.instance().subscribe(appoxeeObserver);
        navigate(new HomeFragment());
        Appoxee.instance().setPushBroadcast(MyPushBroadcast.class);
    }

    public <T extends Fragment> void navigate(T fragment) {
        getSupportFragmentManager().beginTransaction()
                .addToBackStack(fragment.getClass().getSimpleName())
                .replace(binding.fragmentContainerView.getId(), fragment)
                .commit();
    }

    private void requestPostNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionHelper permissionHelper = new PermissionHelper(this.getActivityResultRegistry());
            List<String> permissions = new ArrayList<>();
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            permissionHelper.requestPermissions(this, permissions, result -> {
                if (Boolean.TRUE.equals(result.get(Manifest.permission.POST_NOTIFICATIONS))) {
                    Toast.makeText(
                            this,
                            "Permission(s) granted: \n" + Util.permissionsToString(result),
                            Toast.LENGTH_SHORT
                    ).show();
                    Appoxee.instance().enablePush(true, null);
                }
            });
        } else {
            Appoxee.instance().enablePush(true, null);
        }
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
        Appoxee.instance().unsubscribe(appoxeeObserver);
        getSupportFragmentManager().removeOnBackStackChangedListener(onBackStackChangedListener);
        super.onDestroy();
    }
}