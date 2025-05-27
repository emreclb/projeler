package com.example.android2048game;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameView extends View {

    private static final int GRID_SIZE = 4;
    private static final int TARGET_VALUE = 2048;
    

    private static final float CELL_PADDING = 10;
    private static final float ROUND_RECT_RADIUS = 10;
    private float cellSize;
    private float textSize;
    

    private int[][] gameBoard;
    private int score;
    private boolean gameOver;
    private boolean gameWon;
    private Random random;
    

    private Paint backgroundPaint;
    private Paint emptyCellPaint;
    private Paint textPaint;
    private Paint messagePaint;
    private SparseArray<Paint> cellPaints;
    

    private GestureDetector gestureDetector;
    

    public interface ScoreUpdateListener {
        void onScoreUpdate(int score);
    }
    
    private ScoreUpdateListener scoreUpdateListener;
    
    public GameView(Context context) {
        super(context);
        init(context);
    }
    
    public GameView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public GameView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        random = new Random();
        gameBoard = new int[GRID_SIZE][GRID_SIZE];
        

        initPaints(context);
        

        gestureDetector = new GestureDetector(context, new GestureListener());
        

        startNewGame();
    }
    
    private void initPaints(Context context) {

        backgroundPaint = new Paint();
        backgroundPaint.setColor(ContextCompat.getColor(context, R.color.background_color));
        

        emptyCellPaint = new Paint();
        emptyCellPaint.setColor(ContextCompat.getColor(context, R.color.empty_cell));
        

        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        

        messagePaint = new Paint();
        messagePaint.setColor(ContextCompat.getColor(context, R.color.black));
        messagePaint.setAlpha(200);
        

        initCellPaints(context);
    }
    
    private void initCellPaints(Context context) {

        cellPaints = new SparseArray<>();
        
        int[] values = {2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048};
        int[] colorResIds = {
            R.color.cell_2, R.color.cell_4, R.color.cell_8, R.color.cell_16,
            R.color.cell_32, R.color.cell_64, R.color.cell_128, R.color.cell_256,
            R.color.cell_512, R.color.cell_1024, R.color.cell_2048
        };
        
        for (int i = 0; i < values.length; i++) {
            Paint paint = new Paint();
            paint.setColor(ContextCompat.getColor(context, colorResIds[i]));
            cellPaints.put(values[i], paint);
        }
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        cellSize = Math.min(w, h) / GRID_SIZE;
        textSize = cellSize / 3;
        textPaint.setTextSize(textSize);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);
        
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                drawCell(canvas, row, col);
            }
        }
        
        if (gameOver || gameWon) {
            drawGameEndMessage(canvas);
        }
    }
    
    private void drawCell(Canvas canvas, int row, int col) {
        float left = col * cellSize + CELL_PADDING;
        float top = row * cellSize + CELL_PADDING;
        float right = left + cellSize - CELL_PADDING * 2;
        float bottom = top + cellSize - CELL_PADDING * 2;
        
        RectF rect = new RectF(left, top, right, bottom);
        
        int value = gameBoard[row][col];
        
        if (value == 0) {
            canvas.drawRoundRect(rect, ROUND_RECT_RADIUS, ROUND_RECT_RADIUS, emptyCellPaint);
        } else {
            Paint cellPaint = cellPaints.get(value, cellPaints.get(2048)); // Default to 2048 color if not found
            
            canvas.drawRoundRect(rect, ROUND_RECT_RADIUS, ROUND_RECT_RADIUS, cellPaint);
            
            String valueStr = String.valueOf(value);
            
            textPaint.setColor(ContextCompat.getColor(getContext(),
                    value <= 4 ? R.color.text_color_light : R.color.text_color_dark));
            
            float textSizeAdjusted = textSize;
            if (value >= 100) {
                textSizeAdjusted = textSize * 0.9f;
            }
            if (value >= 1000) {
                textSizeAdjusted = textSize * 0.8f;
            }
            textPaint.setTextSize(textSizeAdjusted);
            
            float textX = left + (cellSize - CELL_PADDING * 2) / 2;
            float textY = top + (cellSize - CELL_PADDING * 2) / 2 + textSizeAdjusted / 3;
            canvas.drawText(valueStr, textX, textY, textPaint);
        }
    }
    
    private void drawGameEndMessage(Canvas canvas) {
        String message = gameWon ? "You Win!" : "Game Over!";
        
        canvas.drawRect(0, 0, getWidth(), getHeight(), messagePaint);
        
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.white));
        textPaint.setTextSize(textSize * 2);
        
        float textX = getWidth() / 2f;
        float textY = getHeight() / 2f + textSize / 2;
        
        canvas.drawText(message, textX, textY, textPaint);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }
    
    public void startNewGame() {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                gameBoard[row][col] = 0;
            }
        }
        
        score = 0;
        gameOver = false;
        gameWon = false;
        
        addRandomTile();
        addRandomTile();
        
        updateScore();
        
        invalidate();
    }
    
    private void updateScore() {
        if (scoreUpdateListener != null) {
            scoreUpdateListener.onScoreUpdate(score);
        }
    }
    
    private void addRandomTile() {
        List<int[]> emptyCells = findEmptyCells();
        
        if (emptyCells.isEmpty()) {
            return;
        }
        
        int[] cell = emptyCells.get(random.nextInt(emptyCells.size()));
        
        int value = random.nextFloat() < 0.9f ? 2 : 4;
        
        gameBoard[cell[0]][cell[1]] = value;
    }
    
    private List<int[]> findEmptyCells() {
        List<int[]> emptyCells = new ArrayList<>();
        
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (gameBoard[row][col] == 0) {
                    emptyCells.add(new int[]{row, col});
                }
            }
        }
        
        return emptyCells;
    }
    
    private boolean moveTiles(Direction direction) {
        boolean moved = false;
        
        switch (direction) {
            case UP:
                moved = moveUp();
                break;
            case DOWN:
                moved = moveDown();
                break;
            case LEFT:
                moved = moveLeft();
                break;
            case RIGHT:
                moved = moveRight();
                break;
        }
        
        if (moved) {
            addRandomTile();
            
            updateScore();
            
            checkGameStatus();
            
            invalidate();
        }
        
        return moved;
    }
    
    private boolean moveLeft() {
        boolean moved = false;
        
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (gameBoard[row][col] == 0) continue;
                
                int nextCol = col;
                while (nextCol > 0 && gameBoard[row][nextCol - 1] == 0) {
                    gameBoard[row][nextCol - 1] = gameBoard[row][nextCol];
                    gameBoard[row][nextCol] = 0;
                    nextCol--;
                    moved = true;
                }
            }
            
            for (int col = 0; col < GRID_SIZE - 1; col++) {
                if (gameBoard[row][col] != 0 && gameBoard[row][col] == gameBoard[row][col + 1]) {
                    gameBoard[row][col] *= 2;
                    gameBoard[row][col + 1] = 0;
                    score += gameBoard[row][col];
                    moved = true;
                    
                    for (int k = col + 1; k < GRID_SIZE - 1; k++) {
                        gameBoard[row][k] = gameBoard[row][k + 1];
                    }
                    gameBoard[row][GRID_SIZE - 1] = 0;
                }
            }
        }
        
        return moved;
    }
    
    private boolean moveRight() {
        boolean moved = false;
        
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = GRID_SIZE - 2; col >= 0; col--) {
                if (gameBoard[row][col] == 0) continue;
                
                int nextCol = col;
                while (nextCol < GRID_SIZE - 1 && gameBoard[row][nextCol + 1] == 0) {
                    gameBoard[row][nextCol + 1] = gameBoard[row][nextCol];
                    gameBoard[row][nextCol] = 0;
                    nextCol++;
                    moved = true;
                }
            }
            
            for (int col = GRID_SIZE - 1; col > 0; col--) {
                if (gameBoard[row][col] != 0 && gameBoard[row][col] == gameBoard[row][col - 1]) {
                    gameBoard[row][col] *= 2;
                    gameBoard[row][col - 1] = 0;
                    score += gameBoard[row][col];
                    moved = true;
                    
                    for (int k = col - 1; k > 0; k--) {
                        gameBoard[row][k] = gameBoard[row][k - 1];
                    }
                    gameBoard[row][0] = 0;
                }
            }
        }
        
        return moved;
    }
    
    private boolean moveUp() {
        boolean moved = false;
        
        for (int col = 0; col < GRID_SIZE; col++) {

            for (int row = 0; row < GRID_SIZE; row++) {
                if (gameBoard[row][col] == 0) continue;
                
                int nextRow = row;
                while (nextRow > 0 && gameBoard[nextRow - 1][col] == 0) {
                    gameBoard[nextRow - 1][col] = gameBoard[nextRow][col];
                    gameBoard[nextRow][col] = 0;
                    nextRow--;
                    moved = true;
                }
            }
            

            for (int row = 0; row < GRID_SIZE - 1; row++) {
                if (gameBoard[row][col] != 0 && gameBoard[row][col] == gameBoard[row + 1][col]) {
                    gameBoard[row][col] *= 2;
                    gameBoard[row + 1][col] = 0;
                    score += gameBoard[row][col];
                    moved = true;
                    

                    for (int k = row + 1; k < GRID_SIZE - 1; k++) {
                        gameBoard[k][col] = gameBoard[k + 1][col];
                    }
                    gameBoard[GRID_SIZE - 1][col] = 0;
                }
            }
        }
        
        return moved;
    }
    
    private boolean moveDown() {
        boolean moved = false;
        
        for (int col = 0; col < GRID_SIZE; col++) {

            for (int row = GRID_SIZE - 2; row >= 0; row--) {
                if (gameBoard[row][col] == 0) continue;
                
                int nextRow = row;
                while (nextRow < GRID_SIZE - 1 && gameBoard[nextRow + 1][col] == 0) {
                    gameBoard[nextRow + 1][col] = gameBoard[nextRow][col];
                    gameBoard[nextRow][col] = 0;
                    nextRow++;
                    moved = true;
                }
            }
            

            for (int row = GRID_SIZE - 1; row > 0; row--) {
                if (gameBoard[row][col] != 0 && gameBoard[row][col] == gameBoard[row - 1][col]) {
                    gameBoard[row][col] *= 2;
                    gameBoard[row - 1][col] = 0;
                    score += gameBoard[row][col];
                    moved = true;
                    

                    for (int k = row - 1; k > 0; k--) {
                        gameBoard[k][col] = gameBoard[k - 1][col];
                    }
                    gameBoard[0][col] = 0;
                }
            }
        }
        
        return moved;
    }
    
    private void checkGameStatus() {

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (gameBoard[row][col] == TARGET_VALUE) {
                    gameWon = true;
                    Toast.makeText(getContext(), "You Win!", Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
        

        if (!findEmptyCells().isEmpty()) {
            return;
        }
        

        if (hasPossibleMoves()) {
            return;
        }
        

        gameOver = true;
        Toast.makeText(getContext(), "Game Over!", Toast.LENGTH_LONG).show();
    }
    
    private boolean hasPossibleMoves() {

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE - 1; col++) {
                if (gameBoard[row][col] == gameBoard[row][col + 1]) {
                    return true;
                }
            }
        }
        

        for (int col = 0; col < GRID_SIZE; col++) {
            for (int row = 0; row < GRID_SIZE - 1; row++) {
                if (gameBoard[row][col] == gameBoard[row + 1][col]) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public void setScoreUpdateListener(ScoreUpdateListener listener) {
        this.scoreUpdateListener = listener;
    }
    
    public void saveGameState(SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        

        editor.putInt("score", score);
        

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                editor.putInt("cell_" + row + "_" + col, gameBoard[row][col]);
            }
        }
        

        editor.putBoolean("game_over", gameOver);
        editor.putBoolean("game_won", gameWon);
        
        editor.apply();
    }
    
    public void loadGameState(SharedPreferences preferences) {

        score = preferences.getInt("score", 0);
        

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                gameBoard[row][col] = preferences.getInt("cell_" + row + "_" + col, 0);
            }
        }
        

        gameOver = preferences.getBoolean("game_over", false);
        gameWon = preferences.getBoolean("game_won", false);
        

        updateScore();
        
        invalidate();
    }
    

    private enum Direction {
        UP, DOWN, LEFT, RIGHT
    }
    

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;
        
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
        
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (gameOver || gameWon) {
                return false;
            }
            
            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();
            
            if (Math.abs(diffX) > Math.abs(diffY)) {

                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {

                        return moveTiles(Direction.RIGHT);
                    } else {

                        return moveTiles(Direction.LEFT);
                    }
                }
            } else {

                if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {

                        return moveTiles(Direction.DOWN);
                    } else {
                        return moveTiles(Direction.UP);
                    }
                }
            }
            
            return false;
        }
    }
}
