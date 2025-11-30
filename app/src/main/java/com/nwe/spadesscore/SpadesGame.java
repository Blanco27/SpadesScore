package com.nwe.spadesscore;

import android.content.Context;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * The SpadesGame class is a singleton that manages the core game logic and state
 * for a Spades card game. It acts as the central data structure for the game.
 * <p>
 * This class:
 * <ul>
 * <li>Tracks the number of players, their names, scores, and trick predictions.
 * <li>Manages the current round, the current player, and the total number of rounds.
 * <li>Handles the transition between the first and second halves of the game.
 * <li>Provides methods to start a new game, start the second half, and confirm trick predictions.
 * <li>Calculates the number of cards to deal based on the current round.
 * <li>Determines when to display the result screen at the end of the game.
 * </ul>
 * <p>
 * The SpadesGame class ensures that all game-related data is encapsulated and
 * accessible through its methods, making it easier to manage the game state across
 * different activities in the application.
 */
public class SpadesGame {
    private static SpadesGame instance;
    private Languages languages;

    // State
    private int playerCount = 4;
    private int currentRound;
    private int currentPlayer = 0;
    private int amountOfRounds;
    private boolean secondHalfOfTheGame = false;
    private boolean showResultScreen = false;

    // Player information's
    private String[] playerNames;
    private final LinkedList<LinkedList<Integer>> scoreMap = new LinkedList<>();
    private final List<Integer> tickPredictions = new ArrayList<>();

    private SpadesGame() {
    }

    public static SpadesGame getInstance() {
        if (instance == null) {
            instance = new SpadesGame();
        }
        return instance;
    }

    public void startGame() {
        scoreMap.clear();
        for (int i = 0; i < playerCount; i++) {
            LinkedList<Integer> playerScoreList = new LinkedList<>();
            playerScoreList.add(0);
            scoreMap.add(playerScoreList);
        }
        currentRound = 1;
        amountOfRounds = (int) Math.floor(32f / playerCount);
        secondHalfOfTheGame = false;
        showResultScreen = false;
        tickPredictions.clear();
    }

    public void startSecondHalf() {
        showResultScreen = false;
        amountOfRounds *= 2;
        secondHalfOfTheGame = true;
    }

    public void setTickPredictions(final int... values) {
        tickPredictions.clear();
        for (final int value : values) {
            tickPredictions.add(value);
        }
    }

    public void confirmTickPredictions(final boolean... values) {
        int length = playerCount == 3 ? values.length - 1 : values.length;
        for (int i = 0; i < length; i++) {
            int currentScore = scoreMap.get(i).getLast();
            if (values[i]) {
                scoreMap.get(i).add(currentScore + tickPredictions.get(i) + 5);
            } else {
                scoreMap.get(i).add(currentScore);
            }
        }
        currentRound++;
        currentPlayer = (currentPlayer + 1) % playerCount;
        if (currentRound > amountOfRounds) {
            showResultScreen = true;
        }
    }

    public void setPlayerCount(final int playerCount) {
        this.playerCount = playerCount;
    }

    public void setPlayerNames(String... playerNames) {
        this.playerNames = playerNames;
    }

    public LinkedList<Integer> getScoreListForPlayer(final int playerNumber) {
        return scoreMap.get(playerNumber);
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public String getPlayerName(final int playerNumber) {
        return playerNames[playerNumber];
    }

    public String getLastScoreAndPlayerNameAsString(final int playerNumber) {
        return getPlayerName(playerNumber) + ": " + scoreMap.get(playerNumber).getLast();
    }

    public String getPlayerTricksString(Context context, final int playerNumber) {
        final int prediction = tickPredictions.get(playerNumber);
        final String tricks = prediction == 1 ? context.getString(R.string.trick) : context.getString(R.string.tricks);
        return String.format(Locale.getDefault(), "%d %s", prediction, tricks);
    }

    public String getPlayerPointsString(Context context, final int playerNumber) {
        final int prediction = tickPredictions.get(playerNumber);
        final int points = prediction + 5;
        return String.format(Locale.getDefault(), context.getString(R.string.points_added), points);
    }


    public int getAmountOfCards() {
        return !secondHalfOfTheGame ? currentRound : Math.max(1, amountOfRounds - currentRound + 1);
    }

    public boolean isSecondHalfOfTheGame() {
        return secondHalfOfTheGame;
    }

    public boolean isShowResultScreen() {
        return showResultScreen;
    }

    public String getCurrentRoundString(Context context) {
        return String.format(Locale.getDefault(), context.getString(R.string.round), currentRound);
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public String getCurrentPlayerName() {
        return getPlayerName(currentPlayer);
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

    public void setRandomDealer(boolean checked) {
        if (checked) {
            currentPlayer = (int) (Math.random() * playerCount);
        } else {
            currentPlayer = 0;
        }
    }

    public int getAmountOfRounds() {
        return amountOfRounds;
    }
}
