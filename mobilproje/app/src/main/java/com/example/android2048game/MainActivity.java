package com.example.android2048game;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private GameView gameView;
    private TextView tvScore;
    private TextView tvBestScore;
    private Button btnNewGame;
    private int bestScore = 0;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        gameView = findViewById(R.id.gameView);
        tvScore = findViewById(R.id.tvScore);
        tvBestScore = findViewById(R.id.tvBestScore);
        btnNewGame = findViewById(R.id.btnNewGame);


        preferences = getPreferences(Context.MODE_PRIVATE);
        bestScore = preferences.getInt("best_score", 0);
        tvBestScore.setText("Best: " + bestScore);


        gameView.setScoreUpdateListener(score -> {
            tvScore.setText("Score: " + score);
            if (score > bestScore) {
                bestScore = score;
                tvBestScore.setText("Best: " + bestScore);

                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt("best_score", bestScore);
                editor.apply();
            }
        });


        btnNewGame.setOnClickListener(v -> gameView.startNewGame());
    }

    @Override
    protected void onPause() {
        super.onPause();

        gameView.saveGameState(preferences);
    }

    @Override
    protected void onResume() {
        super.onResume();

        gameView.loadGameState(preferences);
    }
}
