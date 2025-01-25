package com.nwe.spadesscore;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class GameActivity extends AppCompatActivity {

    private final SpadesGame spadesGame = SpadesGame.getInstance();
    private TextView round_TextView, player_name_TextView,
            current_score_player1_text_view, current_score_player2_text_view,
            current_score_player3_text_view, current_score_player4_text_view;

    @Override
    public void onBackPressed() {
        //Disable going back
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        spadesGame.setGameActivity(this);

        findViewById(R.id.wrong_Button).setOnClickListener(v -> spadesGame.addScoreForCurrentPlayer(0));
        findViewById(R.id.scored0_Button).setOnClickListener(v -> spadesGame.addScoreForCurrentPlayer(5));
        findViewById(R.id.scored1_Button).setOnClickListener(v -> spadesGame.addScoreForCurrentPlayer(6));
        findViewById(R.id.scored2_Button).setOnClickListener(v -> spadesGame.addScoreForCurrentPlayer(7));
        findViewById(R.id.scored3_Button).setOnClickListener(v -> spadesGame.addScoreForCurrentPlayer(8));
        findViewById(R.id.scored4_Button).setOnClickListener(v -> spadesGame.addScoreForCurrentPlayer(9));
        findViewById(R.id.scored5_Button).setOnClickListener(v -> spadesGame.addScoreForCurrentPlayer(10));
        findViewById(R.id.scored6_Button).setOnClickListener(v -> spadesGame.addScoreForCurrentPlayer(11));
        findViewById(R.id.scored7_Button).setOnClickListener(v -> spadesGame.addScoreForCurrentPlayer(12));
        findViewById(R.id.scored8_Button).setOnClickListener(v -> spadesGame.addScoreForCurrentPlayer(13));
        findViewById(R.id.scored9_Button).setOnClickListener(v -> spadesGame.addScoreForCurrentPlayer(14));
        findViewById(R.id.scored10_Button).setOnClickListener(v -> spadesGame.addScoreForCurrentPlayer(15));

        round_TextView = findViewById(R.id.round_TextView);
        player_name_TextView = findViewById(R.id.player_name_TextView);
        current_score_player1_text_view = findViewById(R.id.current_score_player1_text_view);
        current_score_player2_text_view = findViewById(R.id.current_score_player2_text_view);
        current_score_player3_text_view = findViewById(R.id.current_score_player3_text_view);
        current_score_player4_text_view = findViewById(R.id.current_score_player4_text_view);

        if (spadesGame.getPlayerCount() == 3) {
            current_score_player4_text_view.setVisibility(View.GONE);
            findViewById(R.id.player4_space).setVisibility(View.GONE);
        }
        updateView();
    }

    public void updateView() {
        round_TextView.setText(spadesGame.getCurrentRoundString());
        player_name_TextView.setText(spadesGame.getCurrentPlayerName());
        current_score_player1_text_view.setText(spadesGame.getLastScoreAndPlayerNameAsString(0));
        current_score_player2_text_view.setText(spadesGame.getLastScoreAndPlayerNameAsString(1));
        current_score_player3_text_view.setText(spadesGame.getLastScoreAndPlayerNameAsString(2));
        if (spadesGame.getPlayerCount() == 4) {
            current_score_player4_text_view.setText(spadesGame.getLastScoreAndPlayerNameAsString(3));
        }
    }

    public void switchToResultScreenActivity() {
        Intent switchToResultScreenActivity = new Intent(this, ResultScreenActivity.class);
        startActivity(switchToResultScreenActivity);
    }
}