package com.example.android2048game.database;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to manage SQL Server database operations
 */
public class DatabaseHelper {
    private static final String TAG = "DatabaseHelper";
    
    // SQL Server connection parameters
    private static final String SERVER_IP = "YOUR_SQL_SERVER_IP"; // Replace with your SQL Server IP
    private static final String DATABASE_NAME = "Game2048DB";
    private static final String USERNAME = "YOUR_USERNAME"; // Replace with your SQL Server username
    private static final String PASSWORD = "YOUR_PASSWORD"; // Replace with your SQL Server password
    private static final String CONNECTION_URL = 
            "jdbc:jtds:sqlserver://" + SERVER_IP + ":1433/" + 
            DATABASE_NAME + ";user=" + USERNAME + ";password=" + PASSWORD + ";";
    
    private static DatabaseHelper instance;
    private Context context;
    
    private DatabaseHelper(Context context) {
        this.context = context.getApplicationContext();
        initializeDatabase();
    }
    
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context);
        }
        return instance;
    }
    
    /**
     * Initialize the database and create tables if they don't exist
     */
    private void initializeDatabase() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                Connection connection = null;
                Statement statement = null;
                
                try {
                    // Load the JTDS driver
                    Class.forName("net.sourceforge.jtds.jdbc.Driver");
                    
                    // Connect to the database
                    connection = DriverManager.getConnection(CONNECTION_URL);
                    statement = connection.createStatement();
                    
                    // Create the scores table if it doesn't exist
                    String createTableSQL = 
                            "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'PlayerScores') " +
                            "CREATE TABLE PlayerScores (" +
                            "    ID INT IDENTITY(1,1) PRIMARY KEY," +
                            "    PlayerName NVARCHAR(100) NOT NULL," +
                            "    Score INT NOT NULL," +
                            "    GameDate DATETIME DEFAULT GETDATE()" +
                            ")";
                    
                    statement.execute(createTableSQL);
                    Log.d(TAG, "Database and tables initialized successfully");
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error initializing database: " + e.getMessage());
                } finally {
                    try {
                        if (statement != null) statement.close();
                        if (connection != null) connection.close();
                    } catch (SQLException e) {
                        Log.e(TAG, "Error closing database connection: " + e.getMessage());
                    }
                }
                return null;
            }
        }.execute();
    }
    
    /**
     * Save a player's score to the database
     * 
     * @param playerName The name of the player
     * @param score The score achieved
     * @param callback Callback to notify when the operation is complete
     */
    public void saveScore(final String playerName, final int score, final DatabaseCallback<Boolean> callback) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                Connection connection = null;
                PreparedStatement preparedStatement = null;
                
                try {
                    // Load the JTDS driver
                    Class.forName("net.sourceforge.jtds.jdbc.Driver");
                    
                    // Connect to the database
                    connection = DriverManager.getConnection(CONNECTION_URL);
                    
                    // Insert the score
                    String insertSQL = "INSERT INTO PlayerScores (PlayerName, Score) VALUES (?, ?)";
                    preparedStatement = connection.prepareStatement(insertSQL);
                    preparedStatement.setString(1, playerName);
                    preparedStatement.setInt(2, score);
                    
                    int rowsAffected = preparedStatement.executeUpdate();
                    return rowsAffected > 0;
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error saving score: " + e.getMessage());
                    return false;
                } finally {
                    try {
                        if (preparedStatement != null) preparedStatement.close();
                        if (connection != null) connection.close();
                    } catch (SQLException e) {
                        Log.e(TAG, "Error closing database connection: " + e.getMessage());
                    }
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
                Connection connection = null;
                PreparedStatement preparedStatement = null;
                ResultSet resultSet = null;
                List<PlayerScore> scores = new ArrayList<>();
                
                try {
                    // Load the JTDS driver
                    Class.forName("net.sourceforge.jtds.jdbc.Driver");
                    
                    // Connect to the database
                    connection = DriverManager.getConnection(CONNECTION_URL);
                    
                    // Query for top scores
                    String querySQL = "SELECT PlayerName, Score, GameDate FROM PlayerScores " +
                                     "ORDER BY Score DESC OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY";
                    preparedStatement = connection.prepareStatement(querySQL);
                    preparedStatement.setInt(1, limit);
                    
                    resultSet = preparedStatement.executeQuery();
                    
                    while (resultSet.next()) {
                        String playerName = resultSet.getString("PlayerName");
                        int score = resultSet.getInt("Score");
                        String gameDate = resultSet.getString("GameDate");
                        
                        scores.add(new PlayerScore(playerName, score, gameDate));
                    }
                    
                    return scores;
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error getting top scores: " + e.getMessage());
                    return new ArrayList<>();
                } finally {
                    try {
                        if (resultSet != null) resultSet.close();
                        if (preparedStatement != null) preparedStatement.close();
                        if (connection != null) connection.close();
                    } catch (SQLException e) {
                        Log.e(TAG, "Error closing database connection: " + e.getMessage());
                    }
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
                Connection connection = null;
                PreparedStatement preparedStatement = null;
                ResultSet resultSet = null;
                
                try {
                    // Load the JTDS driver
                    Class.forName("net.sourceforge.jtds.jdbc.Driver");
                    
                    // Connect to the database
                    connection = DriverManager.getConnection(CONNECTION_URL);
                    
                    // Query for player's highest score
                    String querySQL = "SELECT MAX(Score) AS HighestScore FROM PlayerScores WHERE PlayerName = ?";
                    preparedStatement = connection.prepareStatement(querySQL);
                    preparedStatement.setString(1, playerName);
                    
                    resultSet = preparedStatement.executeQuery();
                    
                    if (resultSet.next()) {
                        return resultSet.getInt("HighestScore");
                    }
                    
                    return 0;
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error getting player's highest score: " + e.getMessage());
                    return 0;
                } finally {
                    try {
                        if (resultSet != null) resultSet.close();
                        if (preparedStatement != null) preparedStatement.close();
                        if (connection != null) connection.close();
                    } catch (SQLException e) {
                        Log.e(TAG, "Error closing database connection: " + e.getMessage());
                    }
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
     * Interface for database operation callbacks
     */
    public interface DatabaseCallback<T> {
        void onComplete(T result);
    }
}
