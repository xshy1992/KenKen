package com.kenken;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import com.kenken.generator.PuzzleGenerator;
import com.kenken.model.Cell;
import com.kenken.model.Puzzle;
import com.kenken.view.KenKenView;

import java.util.Stack;

public class GameActivity extends AppCompatActivity {
    private int size;
    private Puzzle puzzle;
    private KenKenView kenKenView;
    private int selectedRow = -1;
    private int selectedCol = -1;
    private boolean pencilMode = false;
    
    // 撤销/重做历史记录
    private static class Move {
        int row;
        int col;
        int oldValue;
        boolean[] oldPencilValues;
        boolean wasComplete;
        
        Move(int row, int col, Cell cell) {
            this.row = row;
            this.col = col;
            this.oldValue = cell.value;
            this.oldPencilValues = new boolean[10];
            System.arraycopy(cell.pencilValues, 0, this.oldPencilValues, 0, 10);
        }
    }
    
    private Stack<Move> undoStack = new Stack<>();
    private Stack<Move> redoStack = new Stack<>();
    
    // 计时功能
    private TextView timerText;
    private Handler timerHandler = new Handler();
    private long startTime = 0;
    private boolean isTimerRunning = false;
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long elapsedTime = System.currentTimeMillis() - startTime;
            int seconds = (int) (elapsedTime / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            timerText.setText(String.format("%d:%02d", minutes, seconds));
            if (isTimerRunning) {
                timerHandler.postDelayed(this, 1000);
            }
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        
        size = getIntent().getIntExtra("size", 4);
        setTitle(String.format("KenKen %dx%d", size, size));
        
        kenKenView = findViewById(R.id.kenken_view);
        timerText = findViewById(R.id.timer_text);
        
        // New layout with icon+text vertical containers
        LinearLayout containerPencil = findViewById(R.id.container_pencil);
        LinearLayout containerUndo = findViewById(R.id.container_undo);
        LinearLayout containerRedo = findViewById(R.id.container_redo);
        LinearLayout containerReset = findViewById(R.id.container_reset);
        LinearLayout containerClear = findViewById(R.id.container_clear);
        
        pencilMode = false;
        
        // Generate new puzzle
        generateNewPuzzle();
        
        kenKenView.setOnCellClickListener((row, col) -> {
            selectedRow = row;
            selectedCol = col;
            kenKenView.setSelected(row, col);
        });
        
        // 撤销重做按钮
        containerUndo.setOnClickListener(v -> undo());
        containerRedo.setOnClickListener(v -> redo());
        updateUndoRedoButtons();
        
        // Number buttons
        int[] btnIds = {
            R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };
        
        for (int i = 0; i < btnIds.length && i < size; i++) {
            final int num = i + 1;
            Button btn = findViewById(btnIds[i]);
            btn.setVisibility(View.VISIBLE);
            btn.setOnClickListener(v -> {
                if (selectedRow >= 0 && selectedCol >= 0) {
                    // 保存当前状态用于撤销
                    saveMoveForUndo();
                    if (pencilMode) {
                        // Toggle pencil mark
                        puzzle.cells[selectedRow][selectedCol].pencilValues[num] = 
                            !puzzle.cells[selectedRow][selectedCol].pencilValues[num];
                    } else {
                        puzzle.cells[selectedRow][selectedCol].value = num;
                    }
                    kenKenView.invalidate();
                    checkComplete();
                }
            });
        }
        
        // Hide extra buttons for smaller grids
        for (int i = size; i < btnIds.length; i++) {
            Button btn = findViewById(btnIds[i]);
            btn.setVisibility(View.GONE);
        }
        
        containerClear.setOnClickListener(v -> {
            if (selectedRow >= 0 && selectedCol >= 0) {
                if (!puzzle.cells[selectedRow][selectedCol].isEmpty() || hasPencilMarks(puzzle.cells[selectedRow][selectedCol])) {
                    saveMoveForUndo();
                    puzzle.cells[selectedRow][selectedCol].clear();
                    kenKenView.invalidate();
                    checkComplete();
                }
            }
        });
        
        // 重置按钮：清空所有方格，重新计时
        containerReset.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("重置")
                .setMessage("清空所有填写的数字，重新开始？")
                .setPositiveButton("是", (dialog, which) -> {
                    // 清空所有单元格
                    undoStack.clear();
                    redoStack.clear();
                    for (int i = 0; i < size; i++) {
                        for (int j = 0; j < size; j++) {
                            puzzle.cells[i][j].clear();
                        }
                    }
                    selectedRow = -1;
                    selectedCol = -1;
                    kenKenView.invalidate();
                    updateUndoRedoButtons();
                    // 重新计时
                    stopTimer();
                    startTimer();
                })
                .setNegativeButton("否", null)
                .show();
        });
        
        // 开始计时
        startTimer();
    }
    
    private void saveMoveForUndo() {
        if (selectedRow >= 0 && selectedCol >= 0) {
            Move move = new Move(selectedRow, selectedCol, puzzle.cells[selectedRow][selectedCol]);
            undoStack.push(move);
            redoStack.clear();
            updateUndoRedoButtons();
        }
    }
    
    private boolean hasPencilMarks(Cell cell) {
        for (int i = 1; i <= 9; i++) {
            if (cell.pencilValues[i]) return true;
        }
        return false;
    }
    
    private void undo() {
        if (!undoStack.isEmpty()) {
            Move move = undoStack.pop();
            Cell cell = puzzle.cells[move.row][move.col];
            // Save current state for redo
            Move redoMove = new Move(move.row, move.col, cell);
            redoStack.push(redoMove);
            // Restore old state
            cell.value = move.oldValue;
            System.arraycopy(move.oldPencilValues, 0, cell.pencilValues, 0, 10);
            kenKenView.invalidate();
            updateUndoRedoButtons();
            checkComplete();
        }
    }
    
    private void redo() {
        if (!redoStack.isEmpty()) {
            Move move = redoStack.pop();
            Cell cell = puzzle.cells[move.row][move.col];
            // Save current state for undo
            Move undoMove = new Move(move.row, move.col, cell);
            undoStack.push(undoMove);
            // Restore state
            cell.value = move.oldValue;
            System.arraycopy(move.oldPencilValues, 0, cell.pencilValues, 0, 10);
            kenKenView.invalidate();
            updateUndoRedoButtons();
            checkComplete();
            updateUndoRedoButtons();
        }
    }
    
    private void updateUndoRedoButtons() {
        // 不需要禁用容器，容器一直可点击
    }
    
    private void startTimer() {
        startTime = System.currentTimeMillis();
        isTimerRunning = true;
        timerHandler.postDelayed(timerRunnable, 0);
    }
    
    private void stopTimer() {
        isTimerRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
    }
    
    private void generateNewPuzzle() {
        PuzzleGenerator generator = new PuzzleGenerator(size);
        puzzle = generator.generate();
        selectedRow = -1;
        selectedCol = -1;
        undoStack.clear();
        redoStack.clear();
        updateUndoRedoButtons();
        kenKenView.setPuzzle(puzzle);
        // 重启计时
        stopTimer();
        startTimer();
    }
    
    private void checkErrors() {
        boolean hasErrors = puzzle.hasErrors();
        String message;
        if (hasErrors) {
            message = "There are some errors in your solution. Please check again.";
        } else {
            message = "No errors found so far! Keep going.";
        }
        new AlertDialog.Builder(this)
            .setTitle("Check Result")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show();
    }
    
    private void checkComplete() {
        if (puzzle.isComplete()) {
            stopTimer();
            long elapsedTime = System.currentTimeMillis() - startTime;
            int seconds = (int) (elapsedTime / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            String timeStr = String.format("%d:%02d", minutes, seconds);
            
            new AlertDialog.Builder(this)
                .setTitle("Congratulations! 🎉")
                .setMessage("你已经成功解开谜题！\n用时：" + timeStr + "\n\n要不要再来一局？")
                .setPositiveButton("新游戏", (dialog, which) -> generateNewPuzzle())
                .setNegativeButton("完成", null)
                .show();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        stopTimer();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (!isTimerRunning && puzzle != null && !puzzle.isComplete()) {
            startTimer();
        }
    }
}
