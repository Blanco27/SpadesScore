package com.nwe.spadesscore;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Space;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * The ResultScreenActivity class is responsible for displaying the results of the Spades game
 * at halftime or the end of the game.
 * <p>
 * This activity:
 * <ul>
 * <li>Displays a table with scores for all players across all completed rounds.
 * <li>Highlights the most recent scores with colors and larger text size to indicate rankings.
 * <li>Disables the back button to prevent unintended navigation.
 * </ul>
 * <p>
 * The class interacts with the SpadesGame singleton to retrieve and update game data.
 */
public class ResultScreenActivity extends AppCompatActivity {

    SpadesGame spadesGame = SpadesGame.getInstance();

    LinkedList<TableRow> roundRows = new LinkedList<>();
    Map<Integer, LinkedList<TextView>> playerTextViews = new HashMap<>();
    LinkedList<Space> player4Spaces = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_screen);

        for (int i = 0; i < 4; i++) {
            playerTextViews.computeIfAbsent(i, k -> new LinkedList<>());
        }

        findRoundTableRows();
        findPlayer1TextViews();
        findPlayer2TextViews();
        findPlayer3TextViews();
        findPlayer4Components();

        hidePlayer4Components(spadesGame);
        setVisibilityForRoundRows();
        setScoresInTextViews(spadesGame);
        setPlayerNames();

        final Button continueButton = findViewById(R.id.continue_button);
        continueButton.setOnClickListener(__ -> switchBackToDealCardsActivity());

        applyColorsToLastScoreViews();

        if (spadesGame.isSecondHalfOfTheGame()) {
            continueButton.setVisibility(View.GONE);
            Space lowerSpace = findViewById(R.id.lowerSpace);
            lowerSpace.setVisibility(View.GONE);
        }


        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() { /* disable back */ }
        });
    }

    private void applyColorsToLastScoreViews() {
        Map<Integer, Integer> scores = new HashMap<>();
        for (int i = 0; i < spadesGame.getPlayerCount(); i++) {
            scores.put(i, spadesGame.getScoreListForPlayer(i).getLast());
        }

        List<ScoreObj> scoreList = new ArrayList<>();
        for (Integer playerNumber : scores.keySet()) {
            scoreList.add(new ScoreObj(scores.get(playerNumber), playerNumber));
        }
        scoreList.sort(Comparator.comparing(ScoreObj::getScore));
        Collections.reverse(scoreList);
        for (ScoreObj scoreObj : scoreList) {
            playerTextViews.get(scoreObj.getPlayerNumber()).get(spadesGame.getCurrentRound() - 2).setTextColor(getColorForPlace(scoreList.indexOf(scoreObj) + 1));
            playerTextViews.get(scoreObj.getPlayerNumber()).get(spadesGame.getCurrentRound() - 2).setTextSize(30f);
        }

    }

    private int getColorForPlace(final int place) {
        switch (place) {
            case 1:
                return Color.parseColor("#ffd700");
            case 2:
                return Color.parseColor("#e6e6e6");
            case 3:
                return Color.parseColor("#bf8970");
            default:
                return playerTextViews.get(0).get(1).getTextColors().getDefaultColor();
        }
    }

    private void setPlayerNames() {

        this.<TextView>findViewById(R.id.header_player1).setText(spadesGame.getPlayerName(0));
        this.<TextView>findViewById(R.id.header_player2).setText(spadesGame.getPlayerName(1));
        this.<TextView>findViewById(R.id.header_player3).setText(spadesGame.getPlayerName(2));
        if (spadesGame.getPlayerCount() == 4) {
            this.<TextView>findViewById(R.id.header_player4).setText(spadesGame.getPlayerName(3));
        }
    }

    private void setVisibilityForRoundRows() {
        for (TableRow row : roundRows) {
            row.setVisibility(View.GONE);
        }

        for (int i = 0; i < spadesGame.getCurrentRound() - 1; i++) {
            roundRows.get(i).setVisibility(View.VISIBLE);
        }
    }

    private void setScoresInTextViews(SpadesGame spadesGame) {
        final LinkedList<Integer> scorePlayer1 = spadesGame.getScoreListForPlayer(0);
        for (int i = 1; i < scorePlayer1.size(); i++) {
            playerTextViews.get(0).get(i - 1).setText(String.valueOf(scorePlayer1.get(i)));
        }
        final LinkedList<Integer> scorePlayer2 = spadesGame.getScoreListForPlayer(1);
        for (int i = 1; i < scorePlayer2.size(); i++) {
            playerTextViews.get(1).get(i - 1).setText(String.valueOf(scorePlayer2.get(i)));
        }
        final LinkedList<Integer> scorePlayer3 = spadesGame.getScoreListForPlayer(2);
        for (int i = 1; i < scorePlayer3.size(); i++) {
            playerTextViews.get(2).get(i - 1).setText(String.valueOf(scorePlayer3.get(i)));
        }
        if (spadesGame.getPlayerCount() == 4) {
            final LinkedList<Integer> scorePlayer4 = spadesGame.getScoreListForPlayer(3);
            for (int i = 1; i < scorePlayer4.size(); i++) {
                playerTextViews.get(3).get(i - 1).setText(String.valueOf(scorePlayer4.get(i)));
            }
        }
    }

    private void hidePlayer4Components(SpadesGame spadesGame) {
        if (spadesGame.getPlayerCount() == 3) {
            for (TextView textView : playerTextViews.get(3)) {
                textView.setVisibility(View.GONE);
            }
            for (Space space : player4Spaces) {
                space.setVisibility(View.GONE);
            }
            this.<TextView>findViewById(R.id.header_player4).setVisibility(View.GONE);
        }
    }

    private void findPlayer1TextViews() {
        if (playerTextViews.get(0).size() == 20) {
            return;
        }
        playerTextViews.get(0).add(findViewById(R.id.score_player1_round1));
        playerTextViews.get(0).add(findViewById(R.id.score_player1_round2));
        playerTextViews.get(0).add(findViewById(R.id.score_player1_round3));
        playerTextViews.get(0).add(findViewById(R.id.score_player1_round4));
        playerTextViews.get(0).add(findViewById(R.id.score_player1_round5));
        playerTextViews.get(0).add(findViewById(R.id.score_player1_round6));
        playerTextViews.get(0).add(findViewById(R.id.score_player1_round7));
        playerTextViews.get(0).add(findViewById(R.id.score_player1_round8));
        playerTextViews.get(0).add(findViewById(R.id.score_player1_round9));
        playerTextViews.get(0).add(findViewById(R.id.score_player1_round10));
        playerTextViews.get(0).add(findViewById(R.id.score_player1_round11));
        playerTextViews.get(0).add(findViewById(R.id.score_player1_round12));
        playerTextViews.get(0).add(findViewById(R.id.score_player1_round13));
        playerTextViews.get(0).add(findViewById(R.id.score_player1_round14));
        playerTextViews.get(0).add(findViewById(R.id.score_player1_round15));
        playerTextViews.get(0).add(findViewById(R.id.score_player1_round16));
        playerTextViews.get(0).add(findViewById(R.id.score_player1_round17));
        playerTextViews.get(0).add(findViewById(R.id.score_player1_round18));
        playerTextViews.get(0).add(findViewById(R.id.score_player1_round19));
        playerTextViews.get(0).add(findViewById(R.id.score_player1_round20));
    }

    private void findPlayer2TextViews() {
        if (playerTextViews.get(1).size() == 20) {
            return;
        }
        playerTextViews.get(1).add(findViewById(R.id.score_player2_round1));
        playerTextViews.get(1).add(findViewById(R.id.score_player2_round2));
        playerTextViews.get(1).add(findViewById(R.id.score_player2_round3));
        playerTextViews.get(1).add(findViewById(R.id.score_player2_round4));
        playerTextViews.get(1).add(findViewById(R.id.score_player2_round5));
        playerTextViews.get(1).add(findViewById(R.id.score_player2_round6));
        playerTextViews.get(1).add(findViewById(R.id.score_player2_round7));
        playerTextViews.get(1).add(findViewById(R.id.score_player2_round8));
        playerTextViews.get(1).add(findViewById(R.id.score_player2_round9));
        playerTextViews.get(1).add(findViewById(R.id.score_player2_round10));
        playerTextViews.get(1).add(findViewById(R.id.score_player2_round11));
        playerTextViews.get(1).add(findViewById(R.id.score_player2_round12));
        playerTextViews.get(1).add(findViewById(R.id.score_player2_round13));
        playerTextViews.get(1).add(findViewById(R.id.score_player2_round14));
        playerTextViews.get(1).add(findViewById(R.id.score_player2_round15));
        playerTextViews.get(1).add(findViewById(R.id.score_player2_round16));
        playerTextViews.get(1).add(findViewById(R.id.score_player2_round17));
        playerTextViews.get(1).add(findViewById(R.id.score_player2_round18));
        playerTextViews.get(1).add(findViewById(R.id.score_player2_round19));
        playerTextViews.get(1).add(findViewById(R.id.score_player2_round20));
    }

    private void findPlayer3TextViews() {
        if (playerTextViews.get(2).size() == 20) {
            return;
        }
        playerTextViews.get(2).add(findViewById(R.id.score_player3_round1));
        playerTextViews.get(2).add(findViewById(R.id.score_player3_round2));
        playerTextViews.get(2).add(findViewById(R.id.score_player3_round3));
        playerTextViews.get(2).add(findViewById(R.id.score_player3_round4));
        playerTextViews.get(2).add(findViewById(R.id.score_player3_round5));
        playerTextViews.get(2).add(findViewById(R.id.score_player3_round6));
        playerTextViews.get(2).add(findViewById(R.id.score_player3_round7));
        playerTextViews.get(2).add(findViewById(R.id.score_player3_round8));
        playerTextViews.get(2).add(findViewById(R.id.score_player3_round9));
        playerTextViews.get(2).add(findViewById(R.id.score_player3_round10));
        playerTextViews.get(2).add(findViewById(R.id.score_player3_round11));
        playerTextViews.get(2).add(findViewById(R.id.score_player3_round12));
        playerTextViews.get(2).add(findViewById(R.id.score_player3_round13));
        playerTextViews.get(2).add(findViewById(R.id.score_player3_round14));
        playerTextViews.get(2).add(findViewById(R.id.score_player3_round15));
        playerTextViews.get(2).add(findViewById(R.id.score_player3_round16));
        playerTextViews.get(2).add(findViewById(R.id.score_player3_round17));
        playerTextViews.get(2).add(findViewById(R.id.score_player3_round18));
        playerTextViews.get(2).add(findViewById(R.id.score_player3_round19));
        playerTextViews.get(2).add(findViewById(R.id.score_player3_round20));
    }

    private void findPlayer4Components() {
        if (playerTextViews.get(3).size() == 20) {
            return;
        }
        playerTextViews.get(3).add(findViewById(R.id.score_player4_round1));
        playerTextViews.get(3).add(findViewById(R.id.score_player4_round2));
        playerTextViews.get(3).add(findViewById(R.id.score_player4_round3));
        playerTextViews.get(3).add(findViewById(R.id.score_player4_round4));
        playerTextViews.get(3).add(findViewById(R.id.score_player4_round5));
        playerTextViews.get(3).add(findViewById(R.id.score_player4_round6));
        playerTextViews.get(3).add(findViewById(R.id.score_player4_round7));
        playerTextViews.get(3).add(findViewById(R.id.score_player4_round8));
        playerTextViews.get(3).add(findViewById(R.id.score_player4_round9));
        playerTextViews.get(3).add(findViewById(R.id.score_player4_round10));
        playerTextViews.get(3).add(findViewById(R.id.score_player4_round11));
        playerTextViews.get(3).add(findViewById(R.id.score_player4_round12));
        playerTextViews.get(3).add(findViewById(R.id.score_player4_round13));
        playerTextViews.get(3).add(findViewById(R.id.score_player4_round14));
        playerTextViews.get(3).add(findViewById(R.id.score_player4_round15));
        playerTextViews.get(3).add(findViewById(R.id.score_player4_round16));
        playerTextViews.get(3).add(findViewById(R.id.score_player4_round17));
        playerTextViews.get(3).add(findViewById(R.id.score_player4_round18));
        playerTextViews.get(3).add(findViewById(R.id.score_player4_round19));
        playerTextViews.get(3).add(findViewById(R.id.score_player4_round20));
        player4Spaces.add(findViewById(R.id.player4_space1));
        player4Spaces.add(findViewById(R.id.player4_space2));
        player4Spaces.add(findViewById(R.id.player4_space3));
        player4Spaces.add(findViewById(R.id.player4_space4));
        player4Spaces.add(findViewById(R.id.player4_space5));
        player4Spaces.add(findViewById(R.id.player4_space6));
        player4Spaces.add(findViewById(R.id.player4_space7));
        player4Spaces.add(findViewById(R.id.player4_space8));
        player4Spaces.add(findViewById(R.id.player4_space9));
        player4Spaces.add(findViewById(R.id.player4_space10));
        player4Spaces.add(findViewById(R.id.player4_space11));
        player4Spaces.add(findViewById(R.id.player4_space12));
        player4Spaces.add(findViewById(R.id.player4_space13));
        player4Spaces.add(findViewById(R.id.player4_space14));
        player4Spaces.add(findViewById(R.id.player4_space15));
        player4Spaces.add(findViewById(R.id.player4_space16));
        player4Spaces.add(findViewById(R.id.player4_space17));
        player4Spaces.add(findViewById(R.id.player4_space18));
        player4Spaces.add(findViewById(R.id.player4_space19));
        player4Spaces.add(findViewById(R.id.player4_space20));
        player4Spaces.add(findViewById(R.id.player4_space21));
    }

    private void findRoundTableRows() {
        roundRows.add(findViewById(R.id.round1Scores));
        roundRows.add(findViewById(R.id.round2Scores));
        roundRows.add(findViewById(R.id.round3Scores));
        roundRows.add(findViewById(R.id.round4Scores));
        roundRows.add(findViewById(R.id.round5Scores));
        roundRows.add(findViewById(R.id.round6Scores));
        roundRows.add(findViewById(R.id.round7Scores));
        roundRows.add(findViewById(R.id.round8Scores));
        roundRows.add(findViewById(R.id.round9Scores));
        roundRows.add(findViewById(R.id.round10Scores));
        roundRows.add(findViewById(R.id.round11Scores));
        roundRows.add(findViewById(R.id.round12Scores));
        roundRows.add(findViewById(R.id.round13Scores));
        roundRows.add(findViewById(R.id.round14Scores));
        roundRows.add(findViewById(R.id.round15Scores));
        roundRows.add(findViewById(R.id.round16Scores));
        roundRows.add(findViewById(R.id.round17Scores));
        roundRows.add(findViewById(R.id.round18Scores));
        roundRows.add(findViewById(R.id.round19Scores));
        roundRows.add(findViewById(R.id.round20Scores));
    }

    private void switchBackToDealCardsActivity() {
        SpadesGame.getInstance().startSecondHalf();
        Intent switchDealCardsActivity = new Intent(this, DealCardsActivity.class);
        startActivity(switchDealCardsActivity);
    }
}

class ScoreObj {
    public Integer getScore() {
        return score;
    }

    public Integer getPlayerNumber() {
        return playerNumber;
    }

    final Integer score;
    final Integer playerNumber;

    ScoreObj(final int score, final Integer playerNumber) {
        this.score = score;
        this.playerNumber = playerNumber;
    }
}