package com.nwe.spadesscore;

import android.content.Context;

import com.nwe.spadesscore.domain.GameState;
import com.nwe.spadesscore.domain.SpadesEngine;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Thin singleton adapter around the pure {@link SpadesEngine} / {@link GameState} core.
 * <p>
 * Holds the setup inputs (player count, names, starting dealer, language) plus the current
 * immutable {@link GameState}, and exposes the same API the Activities already use. All
 * game-rule transitions delegate to {@link SpadesEngine}; this class only adapts types and
 * formats Context-dependent strings.
 * <p>
 * There is still no persistence – state survives only as long as the process.
 */
public class SpadesGame {
    private static SpadesGame instance;

    // Setup inputs (set before the game starts)
    private int playerCount = 4;
    private String[] playerNames;
    private int startingPlayer = 0;
    private Languages languages;

    // Immutable in-progress game state (null until startGame())
    private GameState state;

    private final Random random = new Random();

    private SpadesGame() {
    }

    public static SpadesGame getInstance() {
        if (instance == null) {
            instance = new SpadesGame();
        }
        return instance;
    }

    public void startGame() {
        final List<String> names = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            names.add(playerNames[i]);
        }
        state = SpadesEngine.INSTANCE.newGame(playerCount, names, startingPlayer);
    }

    public void startSecondHalf() {
        state = SpadesEngine.INSTANCE.startSecondHalf(state);
    }

    public void setTickPredictions(final int... values) {
        final List<Integer> predictions = new ArrayList<>();
        for (final int value : values) {
            predictions.add(value);
        }
        state = SpadesEngine.INSTANCE.setTickPredictions(state, predictions);
    }

    public void confirmTickPredictions(final boolean... values) {
        final List<Boolean> made = new ArrayList<>();
        for (final boolean value : values) {
            made.add(value);
        }
        state = SpadesEngine.INSTANCE.confirmTricks(state, made);
    }

    public void setPlayerCount(final int playerCount) {
        this.playerCount = playerCount;
    }

    public void setPlayerNames(String... playerNames) {
        this.playerNames = playerNames;
    }

    public void setRandomDealer(boolean checked) {
        startingPlayer = checked ? SpadesEngine.INSTANCE.randomStartingPlayer(playerCount, random) : 0;
    }

    public LinkedList<Integer> getScoreListForPlayer(final int playerNumber) {
        return new LinkedList<>(state.getScores().get(playerNumber));
    }

    public int getCurrentRound() {
        return state.getCurrentRound();
    }

    public String getPlayerName(final int playerNumber) {
        return playerNames[playerNumber];
    }

    public String getLastScoreAndPlayerNameAsString(final int playerNumber) {
        final List<Integer> playerScores = state.getScores().get(playerNumber);
        final int last = playerScores.get(playerScores.size() - 1);
        return getPlayerName(playerNumber) + ": " + last;
    }

    public String getPlayerTricksString(Context context, final int playerNumber) {
        final int prediction = state.getTickPredictions().get(playerNumber);
        final String tricks = prediction == 1 ? context.getString(R.string.trick) : context.getString(R.string.tricks);
        return String.format(Locale.getDefault(), "%d %s", prediction, tricks);
    }

    public String getPlayerPointsString(Context context, final int playerNumber) {
        final int prediction = state.getTickPredictions().get(playerNumber);
        final int points = prediction + 5;
        return String.format(Locale.getDefault(), context.getString(R.string.points_added), points);
    }

    public int getAmountOfCards() {
        return SpadesEngine.INSTANCE.amountOfCards(state);
    }

    public boolean isSecondHalfOfTheGame() {
        return state.getSecondHalf();
    }

    public boolean isShowResultScreen() {
        return state.getShowResultScreen();
    }

    public String getCurrentRoundString(Context context) {
        return String.format(Locale.getDefault(), context.getString(R.string.round), state.getCurrentRound());
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public String getCurrentPlayerName() {
        return getPlayerName(state.getCurrentPlayer());
    }

    public void setLanguages(Languages languages) {
        this.languages = languages;
    }

    public Languages getLanguages() {
        return languages;
    }

    public String getCombinedTrickPredictionString(DeclareTricksActivity declareTricksActivity, int tricks1, int tricks2, int tricks3, int tricks4) {
        final int combinedTricks = tricks1 + tricks2 + tricks3 + (playerCount == 4 ? tricks4 : 0);
        final int possibleTricks = getAmountOfCards();
        return String.format(Locale.getDefault(), declareTricksActivity.getString(R.string.combined_trick_prediction), combinedTricks, possibleTricks, declareTricksActivity.getString(R.string.tricks));
    }

    public int getAmountOfRounds() {
        return state.getAmountOfRounds();
    }
}
