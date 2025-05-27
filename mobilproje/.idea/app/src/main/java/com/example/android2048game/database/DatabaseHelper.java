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
     * Her oyuncu için en yüksek 3 skoru kaydeder
     * 
     * @param playerName The name of the player
     * @param score The score achieved
     * @param callback Callback to notify when the operation is complete
     */
    public void saveScore(final String playerName, final int score, final DatabaseCallback<Boolean> callback) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    SQLiteDatabase db = getWritableDatabase();
                    
                    // Oyuncunun mevcut skorlarını al (en fazla 3 tane)
                    Cursor cursor = db.query(
                        TABLE_PLAYER_SCORES,
                        new String[]{COLUMN_ID, COLUMN_SCORE},
                        COLUMN_PLAYER_NAME + " = ?",
                        new String[]{playerName},
                        null, null,
                        COLUMN_SCORE + " DESC",
                        "3"
                    );
                    
                    // Oyuncunun kaç skoru var kontrol et
                    int playerScoreCount = cursor.getCount();
                    
                    // Oyuncunun en düşük skorunu ve ID'sini bul (eğer 3 skor varsa)
                    long lowestScoreId = -1;
                    int lowestScore = 0;
                    
                    if (playerScoreCount >= 3) {
                        // En son kayda git (en düşük skor)
                        cursor.moveToLast();
                        lowestScore = cursor.getInt(cursor.getColumnIndex(COLUMN_SCORE));
                        lowestScoreId = cursor.getLong(cursor.getColumnIndex(COLUMN_ID));
                    }
                    
                    cursor.close();
                    
                    // Yeni tarih oluştur
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    String currentDateTime = dateFormat.format(new Date());
                    
                    // Yeni skor değerlerini hazırla
                    ContentValues values = new ContentValues();
                    values.put(COLUMN_PLAYER_NAME, playerName);
                    values.put(COLUMN_SCORE, score);
                    values.put(COLUMN_GAME_DATE, currentDateTime);
                    
                    // Skor kaydetme mantığı
                    if (playerScoreCount < 3) {
                        // 3'ten az skor varsa, yeni skor ekle
                        db.insert(TABLE_PLAYER_SCORES, null, values);
                        Log.d(TAG, "Inserted new score for player: " + playerName + ", Score: " + score);
                        return true;
                    } else if (score > lowestScore) {
                        // En düşük skoru yeni skorla değiştir
                        db.update(TABLE_PLAYER_SCORES, values, COLUMN_ID + " = ?", new String[]{String.valueOf(lowestScoreId)});
                        Log.d(TAG, "Updated lowest score for player: " + playerName + ", New Score: " + score);
                        return true;
                    } else {
                        // Yeni skor, mevcut en düşük skordan daha düşükse kaydetme
                        Log.d(TAG, "Score not saved - lower than existing scores for player: " + playerName);
                        return true;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error saving score", e);
                    return false;
                }
            }
            
            @Override
            protected void onPostExecute(Boolean result) {
                if (callback != null) {
                    callback.onComplete(result);
                }
            }
        }.execute();
    }
    
    /**
     * Get the player's highest score from the database
     * 
     * @param playerName The name of the player
     * @param callback Callback to notify when the operation is complete
     */
    public void getPlayerHighestScore(final String playerName, final DatabaseCallback<Integer> callback) {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voids) {
                try {
                    SQLiteDatabase db = getReadableDatabase();
                    
                    // Oyuncunun skorunu doğrudan sorgula
                    Cursor cursor = db.query(
                        TABLE_PLAYER_SCORES,
                        new String[]{COLUMN_SCORE},
                        COLUMN_PLAYER_NAME + " = ?",
                        new String[]{playerName},
                        null, null, null
                    );
                    
                    int highestScore = 0;
                    if (cursor.moveToFirst()) {
                        highestScore = cursor.getInt(cursor.getColumnIndex(COLUMN_SCORE));
                    }
                    
                    cursor.close();
                    return highestScore;
                } catch (Exception e) {
                    Log.e(TAG, "Error getting player's highest score", e);
                    return 0;
                }
            }
            
            @Override
            protected void onPostExecute(Integer result) {
                if (callback != null) {
                    callback.onComplete(result);
                }
            }
        }.execute();
    }
    
    /**
     * Get all scores from the database
     * @return List of PlayerScore objects
     */
    public List<PlayerScore> getAllScores() {
        List<PlayerScore> scores = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        
        Cursor cursor = db.query(
                TABLE_PLAYER_SCORES,
                new String[]{COLUMN_PLAYER_NAME, COLUMN_SCORE, COLUMN_GAME_DATE},
                null, null, null, null,
                COLUMN_SCORE + " DESC"
        );

        if (cursor != null) {
            try {
                int nameIndex = cursor.getColumnIndex(COLUMN_PLAYER_NAME);
                int scoreIndex = cursor.getColumnIndex(COLUMN_SCORE);
                int dateIndex = cursor.getColumnIndex(COLUMN_GAME_DATE);

                while (cursor.moveToNext()) {
                    String name = cursor.getString(nameIndex);
                    int score = cursor.getInt(scoreIndex);
                    String date = cursor.getString(dateIndex);
                    scores.add(new PlayerScore(name, score, date));
                }
            } finally {
                cursor.close();
            }
        }
        
        return scores;
    }
    
    /**
     * Clear all scores from the database
     * @param callback Callback to notify when the operation is complete
     */
    public void clearAllScores(final DatabaseCallback<Boolean> callback) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    SQLiteDatabase db = getWritableDatabase();
                    int rowsDeleted = db.delete(TABLE_PLAYER_SCORES, null, null);
                    Log.d(TAG, "Deleted all scores: " + rowsDeleted + " records");
                    return rowsDeleted >= 0; // Returns true even if no rows were deleted (empty table)
                } catch (Exception e) {
                    Log.e(TAG, "Error deleting all scores", e);
                    return false;
                }
            }
            
            @Override
            protected void onPostExecute(Boolean result) {
                if (callback != null) {
                    callback.onComplete(result);
                }
            }
        }.execute();
    }
    
    /**
     * Delete all scores from the database (synchronous version)
     * @return true if deletion was successful
     */
    public boolean deleteAllScores() {
        SQLiteDatabase db = getWritableDatabase();
        try {
            int rowsDeleted = db.delete(TABLE_PLAYER_SCORES, null, null);
            return rowsDeleted >= 0; // Returns true even if no rows were deleted (empty table)
        } catch (Exception e) {
            Log.e(TAG, "Error deleting all scores", e);
            return false;
        }
    }

    /**
     * Update a player's name in the database
     * 
     * @param oldName The current name of the player
     * @param newName The new name to update to
     * @param callback Callback to notify when the operation is complete
     */
    public void updatePlayerName(final String oldName, final String newName, final DatabaseCallback<Boolean> callback) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    SQLiteDatabase db = getWritableDatabase();
                    
                    ContentValues values = new ContentValues();
                    values.put(COLUMN_PLAYER_NAME, newName);
                    
                    // Update all records with the old player name
                    int rowsUpdated = db.update(
                        TABLE_PLAYER_SCORES,
                        values,
                        COLUMN_PLAYER_NAME + " = ?",
                        new String[]{oldName}
                    );
                    
                    Log.d(TAG, "Updated " + rowsUpdated + " records for player: " + oldName + " to " + newName);
                    return rowsUpdated >= 0; // Return true even if no rows were updated
                } catch (Exception e) {
                    Log.e(TAG, "Error updating player name", e);
                    return false;
                }
            }
            
            @Override
            protected void onPostExecute(Boolean result) {
                if (callback != null) {
                    callback.onComplete(result);
                }
            }
        }.execute();
    }

    /**
     * Clear all scores for a specific player
     * 
     * @param playerName The name of the player
     * @param callback Callback to notify when the operation is complete
     */
    public void clearPlayerScores(final String playerName, final DatabaseCallback<Boolean> callback) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    SQLiteDatabase db = getWritableDatabase();
                    
                    int rowsDeleted = db.delete(
                        TABLE_PLAYER_SCORES,
                        COLUMN_PLAYER_NAME + " = ?",
                        new String[]{playerName}
                    );
                    
                    Log.d(TAG, "Deleted " + rowsDeleted + " records for player: " + playerName);
                    return rowsDeleted >= 0; // Return true even if no rows were deleted
                } catch (Exception e) {
                    Log.e(TAG, "Error clearing player scores", e);
                    return false;
                }
            }
            
            @Override
            protected void onPostExecute(Boolean result) {
                if (callback != null) {
                    callback.onComplete(result);
                }
            }
        }.execute();
    }

    public interface DatabaseCallback<T> {
        void onComplete(T result);
    }
    
    /**
     * Get the top N scores from the database
     * 
     * @param limit Number of top scores to retrieve
     * @param callback Callback to notify when the operation is complete
     */
    public void getTopScores(final int limit, final DatabaseCallback<List<PlayerScore>> callback) {
        new AsyncTask<Void, Void, List<PlayerScore>>() {
            @Override
            protected List<PlayerScore> doInBackground(Void... voids) {
                List<PlayerScore> scores = new ArrayList<>();
                try {
                    SQLiteDatabase db = getReadableDatabase();
                    
                    Cursor cursor = db.query(
                        TABLE_PLAYER_SCORES,
                        new String[]{COLUMN_PLAYER_NAME, COLUMN_SCORE, COLUMN_GAME_DATE},
                        null, null, null, null,
                        COLUMN_SCORE + " DESC",
                        String.valueOf(limit)
                    );
                    
                    if (cursor != null) {
                        try {
                            int nameIndex = cursor.getColumnIndex(COLUMN_PLAYER_NAME);
                            int scoreIndex = cursor.getColumnIndex(COLUMN_SCORE);
                            int dateIndex = cursor.getColumnIndex(COLUMN_GAME_DATE);
                            
                            while (cursor.moveToNext()) {
                                String name = cursor.getString(nameIndex);
                                int score = cursor.getInt(scoreIndex);
                                String date = cursor.getString(dateIndex);
                                scores.add(new PlayerScore(name, score, date));
                            }
                        } finally {
                            cursor.close();
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error getting top scores", e);
                }
                return scores;
            }
            
            @Override
            protected void onPostExecute(List<PlayerScore> result) {
                if (callback != null) {
                    callback.onComplete(result);
                }
            }
        }.execute();
    }
}
