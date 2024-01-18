package com.mapp.engagesample;

import android.app.PendingIntent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;
import java.util.Map;

import eu.brrm.shared_ui.LocalNotifications;
import eu.brrm.shared_ui.databinding.FragmentPushTestBinding;

public class PushTestFragment extends Fragment {

    private FragmentPushTestBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPushTestBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.btnGif.setOnClickListener(v -> {
            try {
                createGifNotification();
            } catch (PendingIntent.CanceledException e) {
                throw new RuntimeException(e);
            }
        });

        binding.btnBrowser.setOnClickListener(v -> {
            try {
                createBrowserPendingIntent();
            } catch (PendingIntent.CanceledException e) {
                throw new RuntimeException(e);
            }
        });

        binding.btnVideo.setOnClickListener(v -> {
            try {
                createVideoNotification();
            } catch (PendingIntent.CanceledException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void createGifNotification() throws PendingIntent.CanceledException {
        LocalNotifications.INSTANCE.createNotification(requireContext(),
                Map.of("apx_dpl", "https://developer.android.com/training/dependency-injection/manual"),
                Map.of("gif", "https://cook.shortest-route.com/l3tech/imgproxy/img/768531997/nyan-cat.gif"),
                List.of(Map.of("apx_url", "http://www.google.com")));
    }

    public void createVideoNotification() throws PendingIntent.CanceledException {
        LocalNotifications.INSTANCE.createNotification(requireContext(),
                Map.of("apx_dpl", "https://developer.android.com/training/dependency-injection/manual"),
                Map.of("video", "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"),
                List.of(Map.of("apx_url", "http://www.google.com")));
    }

    public void createBrowserPendingIntent() throws PendingIntent.CanceledException {
        LocalNotifications.INSTANCE.createNotification(requireContext(),
                Map.of("apx_url", "https://developer.android.com/training/dependency-injection/manual"),
                null,
                List.of(Map.of("apx_dpl", "http://www.google.com")));
    }

    @Override
    public void onDestroy() {
        binding = null;
        super.onDestroy();
    }
}
