package com.nwe.spadesscore;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public abstract class SpadesAppCompatActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initContentView();
        initializeUIComponents();
        setupUI();
        setupBackPressHandler();
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() { /* disable back */ }
        });
    }

    protected abstract void initializeUIComponents();

    protected abstract void setupUI();

    protected abstract void initContentView();

}
