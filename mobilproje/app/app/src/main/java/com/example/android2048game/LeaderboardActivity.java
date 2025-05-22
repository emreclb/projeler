package com.example.android2048game;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android2048game.database.DatabaseHelper;
import com.example.android2048game.database.PlayerScore;

import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {
    
    private LinearLayout leaderboardContainer;
    private ProgressBar progressBar;
    private TextView[] rankTextViews;
    private TextView[] nameTextViews;
    private TextView[] scoreTextViews;
    private Button btnBack;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);
        
        // Initialize views
        leaderboardContainer = findViewById(R.id.leaderboardContainer);
        progressBar = findViewById(R.id.progressBar);
        
        // Initialize arrays for the top 3 player entries
        rankTextViews = new TextView[3];
        nameTextViews = new TextView[3];
        scoreTextViews = new TextView[3];
        
        // Get references to all TextViews
        rankTextViews[0] = findViewById(R.id.tvRank1);
        rankTextViews[1] = findViewById(R.id.tvRank2);
        rankTextViews[2] = findViewById(R.id.tvRank3);
        
        nameTextViews[0] = findViewById(R.id.tvName1);
        nameTextViews[1] = findViewById(R.id.tvName2);
        nameTextViews[2] = findViewById(R.id.tvName3);
        
        scoreTextViews[0] = findViewById(R.id.tvScore1);
        scoreTextViews[1] = findViewById(R.id.tvScore2);
        scoreTextViews[2] = findViewById(R.id.tvScore3);
        
        // Initialize back button
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Bu aktiviteyi kapatıp MainActivity'ye geri dönecek
            }
        });
        
        // Load leaderboard data
        loadLeaderboard();
    }
    
    private void loadLeaderboard() {
        // Show progress bar while loading
        progressBar.setVisibility(View.VISIBLE);
        leaderboardContainer.setVisibility(View.GONE);
        
        // Get top 3 scores from the database
        DatabaseHelper.getInstance(this).getTopScores(3, new DatabaseHelper.DatabaseCallback<List<PlayerScore>>() {
            @Override
            public void onComplete(List<PlayerScore> result) {
                // Hide progress bar
                progressBar.setVisibility(View.GONE);
                leaderboardContainer.setVisibility(View.VISIBLE);
                
                if (result.isEmpty()) {
                    Toast.makeText(LeaderboardActivity.this, "No scores found", Toast.LENGTH_SHORT).show();
                    return;
                }
                
               
                for (int i = 0; i < 3; i++) {
                    if (i < result.size()) {
                        PlayerScore score = result.get(i);
                        rankTextViews[i].setText(String.valueOf(i + 1));
                        nameTextViews[i].setText(score.getPlayerName());
                        scoreTextViews[i].setText(String.valueOf(score.getScore()));
                    } else {
                        
                        rankTextViews[i].setText(String.valueOf(i + 1));
                        nameTextViews[i].setText("-");
                        scoreTextViews[i].setText("-");
                    }
                }
            }
        });
    }
}
