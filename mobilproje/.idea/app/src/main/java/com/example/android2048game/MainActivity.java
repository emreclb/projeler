package com.example.android2048game;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android2048game.database.DatabaseHelper;

public class MainActivity extends AppCompatActivity {
    private GameView gameView;
    private TextView tvScore;
    private TextView tvBestScore;
    private TextView tvPlayerName;
    private Button btnNewGame;
    private Button btnLeaderboard;
    private int bestScore = 0;
    private int currentScore = 0;
    private SharedPreferences preferences;
    private String playerName = "";
    private boolean isNewHighScore = false;
    
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gameView = findViewById(R.id.gameView);
        tvScore = findViewById(R.id.tvScore);
        tvBestScore = findViewById(R.id.tvBestScore);
        tvPlayerName = findViewById(R.id.tvPlayerName);
        btnNewGame = findViewById(R.id.btnNewGame);
        btnLeaderboard = findViewById(R.id.btnLeaderboard);
        
        // Initialize the database helper
        databaseHelper = DatabaseHelper.getInstance(this);
        
        // Set up long click listener for player name to enable name change
        tvPlayerName.setOnLongClickListener(v -> {
            showChangeNameDialog();
            return true;
        });

        preferences = getPreferences(Context.MODE_PRIVATE);
        bestScore = preferences.getInt("best_score", 0);
        playerName = preferences.getString("player_name", "");
        
        tvBestScore.setText("Best: " + bestScore);
        
        // If player name is empty, prompt for it
        if (playerName.isEmpty()) {
            promptForPlayerName();
        } else {
            tvPlayerName.setText("Player: " + playerName);
        }

        // Score update listener - only updates UI and local storage, not database
        gameView.setScoreUpdateListener(score -> {
            currentScore = score;
            tvScore.setText("Score: " + score);
            
            // Check if this is a new high score
            if (score > bestScore) {
                bestScore = score;
                tvBestScore.setText("Best: " + bestScore);
                isNewHighScore = true;

                // Save to SharedPreferences only
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt("best_score", bestScore);
                editor.apply();
            }
        });

        // New Game button - only save score when starting a new game
        btnNewGame.setOnClickListener(v -> {
            // Only save if there's a valid score to save
            if (!playerName.isEmpty() && currentScore > 0) {
                // Save score to database and compare with existing scores
                databaseHelper.saveScore(playerName, currentScore, new DatabaseHelper.DatabaseCallback<Boolean>() {
                    @Override
                    public void onComplete(Boolean result) {
                        if (result && isNewHighScore) {
                            Toast.makeText(MainActivity.this, "Yeni yüksek skor kaydedildi!", Toast.LENGTH_SHORT).show();
                            isNewHighScore = false;
                        }
                    }
                });
            }
            
            // Reset current score and start new game
            currentScore = 0;
            gameView.startNewGame();
        });
        
        btnLeaderboard.setOnClickListener(v -> {
            // Open the leaderboard activity
            Intent intent = new Intent(MainActivity.this, LeaderboardActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Only save game state, not score
        gameView.saveGameState(preferences);
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.loadGameState(preferences);
    }
    
    /**
     * Prompts the user to enter their name using an AlertDialog
     */
    private void promptForPlayerName() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Your Name");
        
        // Set up the input field
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        
        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
            @Override
            public void onClick(DialogInterface dialog, int which) {
                playerName = input.getText().toString().trim();
                
                if (playerName.isEmpty()) {
                    playerName = "Player";
                }
                
                // Save the player name to preferences
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("player_name", playerName);
                editor.apply();
                
                // Update the UI
                tvPlayerName.setText("Player: " + playerName);
                Toast.makeText(MainActivity.this, "Welcome, " + playerName + "!", Toast.LENGTH_SHORT).show();
                
                // Check if the player has a previous high score in the database
                databaseHelper.getPlayerHighestScore(playerName, new DatabaseHelper.DatabaseCallback<Integer>() {
                    @Override
                    public void onComplete(Integer result) {
                        if (result > 0 && result > bestScore) {
                            bestScore = result;
                            tvBestScore.setText("Best: " + bestScore);
                            
                            // Update local storage with the database value
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putInt("best_score", bestScore);
                            editor.apply();
                        }
                    }
                });
            }
        });
        
        // Make dialog non-cancelable so user must enter a name
        builder.setCancelable(false);
        
        builder.show();
    }
    
    /**
     * Shows a dialog to change the player's name
     */
    private void showChangeNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("İsminizi Değiştirin");
        
        // Set up the input field
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(playerName); // Show current name
        input.setSelection(playerName.length()); // Place cursor at the end
        builder.setView(input);
        
        // Set up the buttons
        builder.setPositiveButton("Güncelle", new DialogInterface.OnClickListener() { 
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String oldName = playerName;
                final String newName = input.getText().toString().trim();
                
                if (newName.isEmpty()) {
                    Toast.makeText(MainActivity.this, "İsim boş olamaz!", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (newName.equals(oldName)) {
                    Toast.makeText(MainActivity.this, "İsim aynı kaldı", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Update name in database
                databaseHelper.updatePlayerName(oldName, newName, new DatabaseHelper.DatabaseCallback<Boolean>() {
                    @Override
                    public void onComplete(Boolean result) {
                        if (result) {
                            // Update the name
                            playerName = newName;
                            
                            // Update SharedPreferences
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString("player_name", playerName);
                            editor.apply();
                            
                            // Update UI
                            tvPlayerName.setText("Player: " + playerName);
                            Toast.makeText(MainActivity.this, "İsminiz güncellendi: " + playerName, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "İsim güncellenemedi", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
        
        builder.setNegativeButton("İptal", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        
        builder.show();
    }
}
