package com.mapp.engagesample;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.appoxee.internal.ui.activity.FullScreenActivity;

import java.util.Random;

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
        binding.btnGifIntent.setOnClickListener(v -> {
            try {
                createGifNotification();
            } catch (PendingIntent.CanceledException e) {
                throw new RuntimeException(e);
            }
        });

        binding.btnBrowserIntent.setOnClickListener(v -> {
            try {
                createBrowserPendingIntent();
            } catch (PendingIntent.CanceledException e) {
                throw new RuntimeException(e);
            }
        });

        binding.btnVideoIntent.setOnClickListener(v -> {
            try {
                createVideoNotification();
            } catch (PendingIntent.CanceledException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void createGifNotification() throws PendingIntent.CanceledException {
        Intent intent = FullScreenActivity.getIntent(requireContext());
        Bundle bundle = new Bundle();
        bundle.putLong("id", new Random().nextInt(10000));
        bundle.putString("type", "gif");
        bundle.putString("iosApxMedia", "https://cook.shortest-route.com/l3tech/imgproxy/img/768531997/nyan-cat.gif");
        intent.putExtra("pushData", bundle);
        intent.setAction("OPEN_RICH_PUSH");
        int requestCode = (int) (System.currentTimeMillis() % 1_000_000_000);
        PendingIntent pendingIntent = PendingIntent.getActivity(requireContext(), requestCode, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);
        pendingIntent.send();
    }

    public void createVideoNotification() throws PendingIntent.CanceledException {
        Intent intent = FullScreenActivity.getIntent(requireContext());
        intent.setAction("OPEN_RICH_PUSH");
        Bundle bundle = new Bundle();
        bundle.putLong("id", new Random().nextInt(10000));
        bundle.putString("type", "video");
        bundle.putString("iosApxMedia", "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4");
        intent.putExtra("pushData", bundle);
        int requestCode = (int) (System.currentTimeMillis() % 1_000_000_000);
        PendingIntent pendingIntent = PendingIntent.getActivity(requireContext(), requestCode, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);
        pendingIntent.send();
    }

    public void createBrowserPendingIntent() throws PendingIntent.CanceledException {
        Intent intent = FullScreenActivity.getIntent(requireContext());
        intent.setAction("OPEN_LANDING_PAGE");
        intent.setData(Uri.parse("https://www.google.com"));
        int requestCode = (int) (System.currentTimeMillis() % 1_000_000_000);
        PendingIntent pendingIntent = PendingIntent.getActivity(requireContext(), requestCode, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);
        pendingIntent.send();
    }

    @Override
    public void onDestroy() {
        binding = null;
        super.onDestroy();
    }
}
