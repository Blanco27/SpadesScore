package com.nwe.spadesscore;

import java.util.LinkedList;

public class SpadesGame {
    private static SpadesGame instance;
    private int playerCount;
    private int currentRound;
    private int currentPlayer;
    private int amountOfRounds;
    private final LinkedList<LinkedList<Integer>> scoreMap = new LinkedList<>();
    private String[] playerNames;
    private boolean alreadyRematched = false;
    private GameActivity gameActivity;

    public static SpadesGame getInstance() {
        if (instance == null) {
            instance = new SpadesGame();
        }
        return instance;
    }

    public LinkedList<Integer> getScoreListForPlayer(final int playerNumber){
        return scoreMap.get(playerNumber);
    }

    public int getCurrentRound(){
        return currentRound;
    }

    public String getPlayerName(final int playerNumber){
        return playerNames[playerNumber];
    }

    public void setPlayerCount(final int playerCount) {
        this.playerCount = playerCount;
    }

    public void setPlayerNames(String... playerNames) {
        this.playerNames = playerNames;
    }

    public void startGame() {
        LinkedList<Integer> player1List = new LinkedList<>();
        LinkedList<Integer> player2List = new LinkedList<>();
        LinkedList<Integer> player3List = new LinkedList<>();
        player1List.add(0);
        player2List.add(0);
        player3List.add(0);
        scoreMap.add( player1List);
        scoreMap.add(player2List);
        scoreMap.add( player3List);
        if (playerCount == 4) {
            LinkedList<Integer> player4List = new LinkedList<>();
            player4List.add(0);
            scoreMap.add(player4List);
        }
        currentRound = 1;
        amountOfRounds = (int) Math.floor(32f / playerCount);
        currentPlayer = 0;
    }

    public void addScoreForCurrentPlayer(int points) {
        final LinkedList<Integer> scoreList = scoreMap.get(currentPlayer);
        scoreList.add(scoreList.getLast() + points);

        System.out.println("New Score For " + getCurrentPlayerName() + ": " + scoreMap.get(currentPlayer));

        if (currentPlayer == playerCount - 1) {
            currentPlayer = 0;
            currentRound++;
            if (currentRound > amountOfRounds) {
                gameActivity.switchToResultScreenActivity();
            } else gameActivity.updateView();
        } else {
            currentPlayer++;
            gameActivity.updateView();
        }
    }

    public void startRematch() {
        amountOfRounds = amountOfRounds * 2;
        alreadyRematched = true;
        gameActivity.updateView();
    }

    public String getCurrentRoundString() {
        return "Round " + currentRound;
    }

    public int getPlayerCount() {
        return playerCount;
    }


    public String getCurrentPlayerName() {
        return playerNames[currentPlayer];
    }

    public boolean getAlreadyRematched(){
        return  alreadyRematched;
    }

    public int getAmountOfRounds(){
        return amountOfRounds;
    }

    public String getLastScoreAndPlayerNameAsString(final int playerNumber) {
        final String playerName = playerNames[playerNumber];
        return playerName + ": " + scoreMap.get(playerNumber).getLast();
    }

    public String getScoreAndPlayerNameAsString(final int playerNumber) {
        final String playerName = playerNames[playerNumber];
        return playerName + ": " + scoreMap.get(playerNumber);
    }

    public void setGameActivity(GameActivity gameActivity) {
        this.gameActivity = gameActivity;
    }
}
