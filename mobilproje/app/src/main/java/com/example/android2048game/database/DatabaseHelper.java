package com.example.android2048game.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Helper class to manage SQLite database operations
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "Game2048DB";
    private static final int DATABASE_VERSION = 1;

    // Table and column names
    private static final String TABLE_PLAYER_SCORES = "PlayerScores";
    private static final String COLUMN_ID = "ID";
    private static final String COLUMN_PLAYER_NAME = "PlayerName";
    private static final String COLUMN_SCORE = "Score";
    private static final String COLUMN_GAME_DATE = "GameDate";

    private static DatabaseHelper instance;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Called when the database is created for the first time
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create the scores table
        String createTableSQL = 
                "CREATE TABLE " + TABLE_PLAYER_SCORES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_PLAYER_NAME + " TEXT NOT NULL, " +
                COLUMN_SCORE + " INTEGER NOT NULL, " +
                COLUMN_GAME_DATE + " TEXT NOT NULL)";
        
        db.execSQL(createTableSQL);
        Log.d(TAG, "Database and tables created successfully");
    }
    
    /**
     * Called when the database needs to be upgraded
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For simplicity, drop and recreate the table on upgrade
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLAYER_SCORES);
        onCreate(db);
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Save a player's score to the database
     * 
     * @param playerName The name of the player
     * @param score The score achieved
     * @param callback Callback to notify when the operation is complete
     */
    public void saveScore(final String playerName, final int score, final DatabaseCallback<Boolean> callback) {
        // First check if this score is in the player's top 3
        checkAndSaveTopScore(playerName, score, callback);
    }
    
    /**
     * Check if a score is in the player's top 3 and save it if it is
     * 
     * @param playerName The name of the player
     * @param newScore The new score to check and potentially save
     * @param callback Callback to notify when the operation is complete
     */
    private void checkAndSaveTopScore(final String playerName, final int newScore, final DatabaseCallback<Boolean> callback) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    SQLiteDatabase db = getReadableDatabase();
                    
                    // First check if this exact score already exists for this player to prevent duplicates
                    String checkDuplicateSQL = "SELECT COUNT(*) FROM " + TABLE_PLAYER_SCORES + 
                                             " WHERE " + COLUMN_PLAYER_NAME + " = ? AND " +
                                             COLUMN_SCORE + " = ?";
                    
                    Cursor duplicateCheck = db.rawQuery(checkDuplicateSQL, 
                                                     new String[]{playerName, String.valueOf(newScore)});
                    
                    boolean isDuplicate = false;
                    if (duplicateCheck.moveToFirst()) {
                        int count = duplicateCheck.getInt(0);
                        isDuplicate = (count > 0);
                    }
                    duplicateCheck.close();
                    
                    // If this is a duplicate score, don't save it again
                    if (isDuplicate) {
                        return true; // Return success but don't save
                    }
                    
                    // Get player's current top 3 scores
                    String querySQL = "SELECT " + COLUMN_SCORE + " FROM " + TABLE_PLAYER_SCORES + 
                                     " WHERE " + COLUMN_PLAYER_NAME + " = ? " +
                                     " ORDER BY " + COLUMN_SCORE + " DESC LIMIT 3";
                    
                    Cursor cursor = db.rawQuery(querySQL, new String[]{playerName});
                    
                    // Check if the new score should be saved
                    boolean shouldSave = false;
                    
                    // If we have fewer than 3 scores, always save
                    if (cursor.getCount() < 3) {
                        shouldSave = true;
                    } else {
                        // Check if the new score is higher than the lowest of the top 3
                        int lowestTopScore = 0;
                        
                        // Move to the last row (3rd highest score)
                        if (cursor.moveToLast()) {
                            lowestTopScore = cursor.getInt(cursor.getColumnIndex(COLUMN_SCORE));
                            // If new score is higher than the lowest top score, save it
                            if (newScore > lowestTopScore) {
                                shouldSave = true;
                            }
                        }
                    }
                    
                    cursor.close();
                    
                    // If we should save the score, do it
                    if (shouldSave) {
                        db = getWritableDatabase();
                        
                        // Prepare values to insert
                        ContentValues values = new ContentValues();
                        values.put(COLUMN_PLAYER_NAME, playerName);
                        values.put(COLUMN_SCORE, newScore);
                        
                        // Get current date and time in a formatted string
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        String currentDateTime = dateFormat.format(new Date());
                        values.put(COLUMN_GAME_DATE, currentDateTime);
                        
                        // Insert the score
                        long newRowId = db.insert(TABLE_PLAYER_SCORES, null, values);
                        
                        return newRowId != -1;
                    } else {
                        // No need to save, but operation completed successfully
                        return true;
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error checking and saving score: " + e.getMessage());
                    return false;
                }
            }
            
            @Override
            protected void onPostExecute(Boolean success) {
                if (callback != null) {
                    callback.onComplete(success);
                }
            }
        }.execute();
    }

    /**
     * Get the top scores from the database
     * 
     * @param limit The number of top scores to retrieve
     * @param callback Callback to receive the list of scores
     */
    public void getTopScores(final int limit, final DatabaseCallback<List<PlayerScore>> callback) {
        new AsyncTask<Void, Void, List<PlayerScore>>() {
            @Override
            protected List<PlayerScore> doInBackground(Void... voids) {
                List<PlayerScore> scores = new ArrayList<>();
                
                try {
                    SQLiteDatabase db = getReadableDatabase();
                    
                    // Query for top scores
                    String querySQL = "SELECT " + COLUMN_PLAYER_NAME + ", " + COLUMN_SCORE + ", " + 
                                     COLUMN_GAME_DATE + " FROM " + TABLE_PLAYER_SCORES + 
                                     " ORDER BY " + COLUMN_SCORE + " DESC LIMIT ?";
                    
                    Cursor cursor = db.rawQuery(querySQL, new String[]{String.valueOf(limit)});
                    
                    while (cursor.moveToNext()) {
                        String playerName = cursor.getString(cursor.getColumnIndex(COLUMN_PLAYER_NAME));
                        int score = cursor.getInt(cursor.getColumnIndex(COLUMN_SCORE));
                        String gameDate = cursor.getString(cursor.getColumnIndex(COLUMN_GAME_DATE));
                        
                        scores.add(new PlayerScore(playerName, score, gameDate));
                    }
                    
                    cursor.close();
                    return scores;
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error getting top scores: " + e.getMessage());
                    return new ArrayList<>();
                }
            }
            
            @Override
            protected void onPostExecute(List<PlayerScore> scores) {
                if (callback != null) {
                    callback.onComplete(scores);
                }
            }
        }.execute();
    }
    
    /**
     * Get the top 3 scores for a specific player
     * 
     * @param playerName The name of the player
     * @param callback Callback to receive the list of scores
     */
    public void getPlayerTopScores(final String playerName, final DatabaseCallback<List<PlayerScore>> callback) {
        new AsyncTask<Void, Void, List<PlayerScore>>() {
            @Override
            protected List<PlayerScore> doInBackground(Void... voids) {
                List<PlayerScore> scores = new ArrayList<>();
                
                try {
                    SQLiteDatabase db = getReadableDatabase();
                    
                    // Query for player's top 3 scores
                    String querySQL = "SELECT " + COLUMN_PLAYER_NAME + ", " + COLUMN_SCORE + ", " + 
                                     COLUMN_GAME_DATE + " FROM " + TABLE_PLAYER_SCORES + 
                                     " WHERE " + COLUMN_PLAYER_NAME + " = ? " +
                                     " ORDER BY " + COLUMN_SCORE + " DESC LIMIT 3";
                    
                    Cursor cursor = db.rawQuery(querySQL, new String[]{playerName});
                    
                    while (cursor.moveToNext()) {
                        String name = cursor.getString(cursor.getColumnIndex(COLUMN_PLAYER_NAME));
                        int score = cursor.getInt(cursor.getColumnIndex(COLUMN_SCORE));
                        String gameDate = cursor.getString(cursor.getColumnIndex(COLUMN_GAME_DATE));
                        
                        scores.add(new PlayerScore(name, score, gameDate));
                    }
                    
                    cursor.close();
                    return scores;
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error getting player's top scores: " + e.getMessage());
                    return new ArrayList<>();
                }
            }
            
            @Override
            protected void onPostExecute(List<PlayerScore> scores) {
                if (callback != null) {
                    callback.onComplete(scores);
                }
            }
        }.execute();
    }

    /**
     * Get the highest score for a specific player
     * 
     * @param playerName The name of the player
     * @param callback Callback to receive the highest score
     */
    public void getPlayerHighestScore(final String playerName, final DatabaseCallback<Integer> callback) {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voids) {
                try {
                    SQLiteDatabase db = getReadableDatabase();
                    
                    // Query for player's highest score
                    String querySQL = "SELECT MAX(" + COLUMN_SCORE + ") AS HighestScore FROM " + 
                                     TABLE_PLAYER_SCORES + " WHERE " + COLUMN_PLAYER_NAME + " = ?";
                    
                    Cursor cursor = db.rawQuery(querySQL, new String[]{playerName});
                    
                    int highestScore = 0;
                    if (cursor.moveToFirst()) {
                        highestScore = cursor.getInt(0);
                    }
                    
                    cursor.close();
                    return highestScore;
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error getting player's highest score: " + e.getMessage());
                    return 0;
                }
            }
            
            @Override
            protected void onPostExecute(Integer highestScore) {
                if (callback != null) {
                    callback.onComplete(highestScore);
                }
            }
        }.execute();
    }
    
    /**
     * Clear all scores from the database
     * 
     * @param callback Callback to notify when the operation is complete
     */
    public void clearAllScores(final DatabaseCallback<Boolean> callback) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    SQLiteDatabase db = getWritableDatabase();
                    
                    // Delete all records from the scores table
                    int rowsDeleted = db.delete(TABLE_PLAYER_SCORES, null, null);
                    
                    Log.d(TAG, "Deleted " + rowsDeleted + " scores from the database");
                    return true;
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error clearing scores: " + e.getMessage());
                    return false;
                }
            }
            
            @Override
            protected void onPostExecute(Boolean success) {
                if (callback != null) {
                    callback.onComplete(success);
                }
            }
        }.execute();
    }
    
    /**
     * Clear scores for a specific player
     * 
     * @param playerName The name of the player whose scores should be cleared
     * @param callback Callback to notify when the operation is complete
     */
    public void clearPlayerScores(final String playerName, final DatabaseCallback<Boolean> callback) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    SQLiteDatabase db = getWritableDatabase();
                    
                    // Delete records for the specified player
                    int rowsDeleted = db.delete(
                        TABLE_PLAYER_SCORES, 
                        COLUMN_PLAYER_NAME + " = ?", 
                        new String[]{playerName}
                    );
                    
                    Log.d(TAG, "Deleted " + rowsDeleted + " scores for player " + playerName);
                    return true;
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error clearing player scores: " + e.getMessage());
                    return false;
                }
            }
            
            @Override
            protected void onPostExecute(Boolean success) {
                if (callback != null) {
                    callback.onComplete(success);
                }
            }
        }.execute();
    }
    
    public interface DatabaseCallback<T> {
        void onComplete(T result);
    }
}
