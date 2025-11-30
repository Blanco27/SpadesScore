package com.nwe.spadesscore;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

/**
 * The ConfirmTicksActivity class is responsible for managing the user interface
 * where players confirm their trick predictions for the current round of the Spades game.
 * <p>
 * This activity:
 * <ul>
 * <li>Displays the current round number and player scores.
 * <li>Allows players to confirm their trick predictions using checkboxes.
 * <li>Handles navigation to the next activity based on the game state:
 * <li>If the game is over, it navigates to the result screen.
 * <li>Otherwise, it proceeds to the card dealing screen for the next round.
 * <li>Ensures the back button is disabled to prevent unintended navigation.
 * </ul>
 * <p>
 * The class interacts with the SpadesGame singleton to retrieve and update game data.
 */
public class ConfirmTicksActivity extends SpadesAppCompatActivity {
    private final SpadesGame spadesGame = SpadesGame.getInstance();
    private TextView roundTextView;
    private TextView[] currentScoreTextViews;
    private TextView[] playerNameTextViews;
    private TextView[] playerTricksTextViews;
    private TextView[] playerPointsTextViews;
    private CheckBox[] playerCheckboxes;
    private ProgressBar progressBar;

    int DEFAULT_COLOR, GREEN_COLOR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initContentView() {
        setContentView(R.layout.activity_confirm_tricks);
        GREEN_COLOR = ContextCompat.getColor(this, R.color.plusPointsText);
    }

    @Override
    protected void initializeUIComponents() {
        roundTextView = findViewById(R.id.round_TextView);
        progressBar = findViewById(R.id.progressBar);

        currentScoreTextViews = new TextView[]{
                findViewById(R.id.current_score_player1_text_view),
                findViewById(R.id.current_score_player2_text_view),
                findViewById(R.id.current_score_player3_text_view),
                findViewById(R.id.current_score_player4_text_view)
        };

        playerNameTextViews = new TextView[]{
                findViewById(R.id.player1_name_text),
                findViewById(R.id.player2_name_text),
                findViewById(R.id.player3_name_text),
                findViewById(R.id.player4_name_text)
        };
        playerTricksTextViews = new TextView[]{
                findViewById(R.id.player1_amount_of_tricks_text),
                findViewById(R.id.player2_amount_of_tricks_text),
                findViewById(R.id.player3_amount_of_tricks_text),
                findViewById(R.id.player4_amount_of_tricks_text)
        };
        playerPointsTextViews = new TextView[]{
                findViewById(R.id.player1_points_text),
                findViewById(R.id.player2_points_text),
                findViewById(R.id.player3_points_text),
                findViewById(R.id.player4_points_text)
        };

        playerCheckboxes = new CheckBox[]{
                findViewById(R.id.player1_checkbox),
                findViewById(R.id.player2_checkbox),
                findViewById(R.id.player3_checkbox),
                findViewById(R.id.player4_checkbox)
        };
    }

    @Override
    protected void setupUI() {
        if (spadesGame.getPlayerCount() == 3) {
            currentScoreTextViews[3].setVisibility(View.GONE);
            findViewById(R.id.player4_layout).setVisibility(View.GONE);
        }
        for (int playerIndex = 0; playerIndex < spadesGame.getPlayerCount(); playerIndex++) {
            int finalPlayerIndex = playerIndex;
            playerCheckboxes[playerIndex].setOnClickListener(view -> {
                if (playerCheckboxes[finalPlayerIndex].isChecked()) {
                    playerPointsTextViews[finalPlayerIndex].setTextColor(GREEN_COLOR);
                } else {
                    playerPointsTextViews[finalPlayerIndex].setTextColor(DEFAULT_COLOR);
                }
            });
        }

        progressBar.setMax(spadesGame.getAmountOfRounds());
        progressBar.setProgress(spadesGame.getCurrentRound());

        findViewById(R.id.start_Button).setOnClickListener(v -> startNextRound());
        DEFAULT_COLOR = playerPointsTextViews[0].getTextColors().getDefaultColor();
        updateView();
    }

    public void updateView() {
        roundTextView.setText(spadesGame.getCurrentRoundString(this));

        for (int playerIndex = 0; playerIndex < spadesGame.getPlayerCount(); playerIndex++) {
            currentScoreTextViews[playerIndex].setText(spadesGame.getLastScoreAndPlayerNameAsString(playerIndex));
            playerNameTextViews[playerIndex].setText(spadesGame.getPlayerName(playerIndex));
            playerTricksTextViews[playerIndex].setText(spadesGame.getPlayerTricksString(this, playerIndex));
            playerPointsTextViews[playerIndex].setText(spadesGame.getPlayerPointsString(this, playerIndex));
        }
    }

    public void startNextRound() {
        spadesGame.confirmTickPredictions(
                playerCheckboxes[0].isChecked(),
                playerCheckboxes[1].isChecked(),
                playerCheckboxes[2].isChecked(),
                spadesGame.getPlayerCount() == 4 && playerCheckboxes[3].isChecked()
        );

        if (spadesGame.isShowResultScreen()) {
            switchToResultScreenActivity();
        } else {
            switchToDealCardsActivity();
        }
    }

    private void switchToDealCardsActivity() {
        Intent intent = new Intent(this, DealCardsActivity.class);
        startActivity(intent);
    }

    private void switchToResultScreenActivity() {
        Intent intent = new Intent(this, ResultScreenActivity.class);
        startActivity(intent);
    }
}