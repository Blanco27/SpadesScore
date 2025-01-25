package com.nwe.spadesscore;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.button_player3).setOnClickListener(v -> switchToPlayerNamesAction(3));
        findViewById(R.id.button_player4).setOnClickListener(v -> switchToPlayerNamesAction(4));
    }

    private void switchToPlayerNamesAction(int i) {
        SpadesGame.getInstance().setPlayerCount(i);
        Intent switchToPlayerNamesActivity = new Intent(this, PlayerNamesActivity.class);
        startActivity(switchToPlayerNamesActivity);
    }
}