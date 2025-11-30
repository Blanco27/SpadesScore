package com.nwe.spadesscore;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

/**
 * The PlayerNamesActivity class is responsible for managing the user interface
 * where players input their names before starting the Spades game.
 * <p>
 * This activity:
 * <ul>
 * <li>Validates player names to ensure they are not empty and do not exceed 10 characters.
 * <li>Displays warning dialogs if validation fails.
 * <li>Updates the SpadesGame singleton with the entered player names.
 * <li>Navigates to the DealCardsActivity to start the game after successful validation.
 * <li>Disables the back button to prevent unintended navigation.
 *  </ul>
 */
public class PlayerNamesActivity extends SpadesAppCompatActivity {

    private TextInputLayout player1_name_input, player2_name_input, player3_name_input, player4_name_input;

    private SwitchCompat random_dealer_switch;
    private final SpadesGame spadesGame = SpadesGame.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    void initializeUIComponents() {
        player1_name_input = findViewById(R.id.player1_name_input);
        player2_name_input = findViewById(R.id.player2_name_input);
        player3_name_input = findViewById(R.id.player3_name_input);
        player4_name_input = findViewById(R.id.player4_name_input);
        random_dealer_switch = findViewById(R.id.random_dealer_switch);

        DEBUG_DELETE_LATER();
    }

    private void DEBUG_DELETE_LATER() {
        assert player1_name_input.getEditText() != null;
        player1_name_input.getEditText().setText("Player 1");
        assert player2_name_input.getEditText() != null;
        player2_name_input.getEditText().setText("Player 2");
        assert player3_name_input.getEditText() != null;
        player3_name_input.getEditText().setText("Player 3");
        assert player4_name_input.getEditText() != null;
        player4_name_input.getEditText().setText("Player 4");
    }

    @Override
    void setupUI() {
        if (spadesGame.getPlayerCount() == 3) {
            player4_name_input.setVisibility(View.GONE);
            findViewById(R.id.player4_space).setVisibility(View.GONE);
        }

        random_dealer_switch.setThumbTintList(ContextCompat.getColorStateList(this, R.color.switchThumbTint));
        random_dealer_switch.setTrackTintList(ContextCompat.getColorStateList(this, R.color.switchTrackTint));

        findViewById(R.id.start_game_button).setOnClickListener(v -> startGame());
    }

    @Override
    void initContentView() {
        setContentView(R.layout.activity_player_names);
    }

    private void startGame() {
        final String name_player1 = Objects.requireNonNull(player1_name_input.getEditText()).getText().toString();
        final String name_player2 = Objects.requireNonNull(player2_name_input.getEditText()).getText().toString();
        final String name_player3 = Objects.requireNonNull(player3_name_input.getEditText()).getText().toString();
        final String name_player4 = Objects.requireNonNull(player4_name_input.getEditText()).getText().toString();

        if (spadesGame.getPlayerCount() == 3) {
            if (isAnyNameEmpty(name_player1, name_player2, name_player3)
                    || isAnyNameTooLong(name_player1, name_player2, name_player3)) {
                return;
            } else {
                spadesGame.setPlayerNames(name_player1, name_player2, name_player3);
            }
        } else {
            if (isAnyNameEmpty(name_player1, name_player2, name_player3, name_player4)
                    || isAnyNameTooLong(name_player1, name_player2, name_player3, name_player4)) {
                return;
            } else {
                spadesGame.setPlayerNames(name_player1, name_player2, name_player3, name_player4);
            }
        }

        spadesGame.setRandomDealer(random_dealer_switch.isChecked());
        spadesGame.startGame();
        Intent switchToDealCardsActivity = new Intent(this, DealCardsActivity.class);
        startActivity(switchToDealCardsActivity);
    }

    private boolean isAnyNameEmpty(String... playerNames) {
        for (String playerName : playerNames) {
            if (playerName.trim().isEmpty()) {
                showEmptyNameWarningDialog();
                return true;
            }
        }
        return false;
    }

    private boolean isAnyNameTooLong(String... playerNames) {
        for (String playerName : playerNames) {
            if (playerName.length() > 10) {
                showTooLongNameWarningDialog();
                return true;
            }
        }
        return false;
    }

    private void showEmptyNameWarningDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Please give each player a name").setTitle("Empty Name");
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showTooLongNameWarningDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Names can only be a maximum of 10 characters long").setTitle("Name too long");
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}