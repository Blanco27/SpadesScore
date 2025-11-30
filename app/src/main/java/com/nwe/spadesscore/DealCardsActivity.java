package com.nwe.spadesscore;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Locale;

/**
 * The DealCardsActivity class is responsible for managing the user interface
 * where players deal cards for the current round of the Spades game.
 * <p>
 * This activity:
 * <ul>
 * <li>Displays the current round number, the player responsible for dealing, and the card amount.
 * <li>Updates and shows the current scores of all players.
 * <li>Disables the back button to prevent unintended navigation.
 * <li>Navigates to the DeclareTricksActivity to proceed with the game after dealing is complete.
 *  </ul>
 * <p>
 * The class interacts with the SpadesGame singleton to retrieve and update game data.
 */
public class DealCardsActivity extends SpadesAppCompatActivity {
    private final SpadesGame spadesGame = SpadesGame.getInstance();

    private TextView round_TextView, player_name_TextView, amount_TextView,
            current_score_player1_text_view, current_score_player2_text_view,
            current_score_player3_text_view, current_score_player4_text_view;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initializeUIComponents() {
        amount_TextView = findViewById(R.id.amount_TextView);
        round_TextView = findViewById(R.id.round_TextView);
        player_name_TextView = findViewById(R.id.player_name_TextView);
        progressBar = findViewById(R.id.progressBar);
        current_score_player1_text_view = findViewById(R.id.current_score_player1_text_view);
        current_score_player2_text_view = findViewById(R.id.current_score_player2_text_view);
        current_score_player3_text_view = findViewById(R.id.current_score_player3_text_view);
        current_score_player4_text_view = findViewById(R.id.current_score_player4_text_view);
    }

    @Override
    protected void setupUI() {
        if (spadesGame.getPlayerCount() == 3) {
            current_score_player4_text_view.setVisibility(View.GONE);
        }

        progressBar.setMax(spadesGame.getAmountOfRounds());
        progressBar.setProgress(spadesGame.getCurrentRound());

        findViewById(R.id.start_Button).setOnClickListener(v -> startNextRound());
        updateView();
    }

    @Override
    void initContentView() {
        setContentView(R.layout.activity_deal_cards);
    }

    public void updateView() {
        round_TextView.setText(spadesGame.getCurrentRoundString(this));
        player_name_TextView.setText(getPlayerDealMessage());
        amount_TextView.setText(getCardAmountMessage());
        updatePlayerScores();
    }

    private String getPlayerDealMessage() {
        return String.format(Locale.getDefault(), getString(R.string.player_must_deal_cards), spadesGame.getCurrentPlayerName());
    }

    private String getCardAmountMessage() {
        return String.format(Locale.getDefault(), "%dx", spadesGame.getAmountOfCards());
    }

    private void updatePlayerScores() {
        current_score_player1_text_view.setText(spadesGame.getLastScoreAndPlayerNameAsString(0));
        current_score_player2_text_view.setText(spadesGame.getLastScoreAndPlayerNameAsString(1));
        current_score_player3_text_view.setText(spadesGame.getLastScoreAndPlayerNameAsString(2));
        if (spadesGame.getPlayerCount() == 4) {
            current_score_player4_text_view.setText(spadesGame.getLastScoreAndPlayerNameAsString(3));
        }
    }

    private void startNextRound() {
        Intent switchToDeclareCardsActivity = new Intent(this, DeclareTricksActivity.class);
        startActivity(switchToDeclareCardsActivity);
    }
}