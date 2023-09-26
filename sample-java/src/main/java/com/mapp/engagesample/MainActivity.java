package com.mapp.engagesample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.appoxee.Appoxee;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        boolean isMappSdkReady = Appoxee.instance().isReady();

        if (isMappSdkReady) {
            Appoxee.instance().setAlias("", null);
        }
    }
}