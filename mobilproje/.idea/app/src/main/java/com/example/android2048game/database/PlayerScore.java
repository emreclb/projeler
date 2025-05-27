package com.example.android2048game.database;

/**
 * Class representing a player's score record
 */
public class PlayerScore {
    private String playerName;
    private int score;
    private String gameDate;
    
    public PlayerScore(String playerName, int score, String gameDate) {
        this.playerName = playerName;
        this.score = score;
        this.gameDate = gameDate;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public int getScore() {
        return score;
    }
    
    public String getGameDate() {
        return gameDate;
    }
    
    @Override
    public String toString() {
        return playerName + " - " + score;
    }
}
