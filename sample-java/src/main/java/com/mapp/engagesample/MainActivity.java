package com.mapp.engagesample;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.appoxee.Appoxee;

import java.util.Objects;

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
        navigate(new HomeFragment());
        Appoxee.instance().setPushBroadcast(MyPushBroadcast.class);
    }

    public <T extends Fragment> void navigate(T fragment) {
        getSupportFragmentManager().beginTransaction()
                .addToBackStack(fragment.getClass().getSimpleName())
                .replace(binding.fragmentContainerView.getId(), fragment)
                .commit();
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