package com.nwe.spadesscore;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class PlayerNamesActivity extends AppCompatActivity {

    private TextInputLayout player1_name_input, player2_name_input, player3_name_input, player4_name_input;
    private final SpadesGame spadesGame = SpadesGame.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_names);

        player1_name_input = findViewById(R.id.player1_name_input);
        player2_name_input = findViewById(R.id.player2_name_input);
        player3_name_input = findViewById(R.id.player3_name_input);
        player4_name_input = findViewById(R.id.player4_name_input);

        if (spadesGame.getPlayerCount() == 3) {
            player4_name_input.setVisibility(View.GONE);
            findViewById(R.id.player4_sapce).setVisibility(View.GONE);
        }
        findViewById(R.id.start_game_button).setOnClickListener(v -> startGame());
    }

    private void startGame() {
        final String name_player1 = Objects.requireNonNull(player1_name_input.getEditText()).getText().toString();
        final String name_player2 = Objects.requireNonNull(player2_name_input.getEditText()).getText().toString();
        final String name_player3 = Objects.requireNonNull(player3_name_input.getEditText()).getText().toString();
        final String name_player4 = Objects.requireNonNull(player4_name_input.getEditText()).getText().toString();

        if (spadesGame.getPlayerCount() == 3) {
            if (isInputInvalid(name_player1, name_player2, name_player3)) {
                showWarningDialog();
                return;
            } else {
                spadesGame.setPlayerNames(name_player1, name_player2, name_player3);
            }
        } else {
            if (isInputInvalid(name_player1, name_player2, name_player3, name_player4)) {
                showWarningDialog();
                return;
            } else {
                spadesGame.setPlayerNames(name_player1, name_player2, name_player3, name_player4);
            }
        }
        spadesGame.startGame();
        Intent switchToStartGameActivity = new Intent(this, GameActivity.class);
        startActivity(switchToStartGameActivity);
    }

    private boolean isInputInvalid(String... playerNames) {
        for (String playerName : playerNames) {
            if (playerName.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void showWarningDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Please give each player a name").setTitle("Empty Name");
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}