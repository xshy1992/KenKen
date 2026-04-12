package com.shy.kenken;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import com.shy.kenken.generator.PuzzleGenerator;
import com.shy.kenken.model.Cell;
import com.shy.kenken.model.Puzzle;
import com.shy.kenken.view.KenKenView;

import java.util.Stack;

public class GameActivity extends AppCompatActivity {
    private int size;
    private Puzzle puzzle;
    private KenKenView kenKenView;
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
    
    private int selectedRow = -1;
    private int selectedCol = -1;
    private boolean pencilMode = false;
    
    private Stack<Move> undoStack = new Stack<>();
    
    private static class Move {
        int row, col;
        int oldValue;
        boolean[] oldPencilValues = new boolean[10];
        
        Move(int row, int col, Cell cell) {
            this.row = row;
            this.col = col;
            this.oldValue = cell.value;
            System.arraycopy(cell.pencilValues, 0, oldPencilValues, 0, 10);
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        
        size = getIntent().getIntExtra("size", 4);
        timerText = findViewById(R.id.timer_text);
        kenKenView = findViewById(R.id.kenken_view);
        
        LinearLayout containerPencil = findViewById(R.id.container_pencil);
        LinearLayout containerUndo = findViewById(R.id.container_undo);
        LinearLayout containerReset = findViewById(R.id.container_reset);
        LinearLayout containerClear = findViewById(R.id.container_clear);
        
        pencilMode = false;
        updatePencilButtonBackground();
        
        // Generate new puzzle
        generateNewPuzzle();
        
        kenKenView.setOnCellClickListener((row, col) -> {
            selectedRow = row;
            selectedCol = col;
            kenKenView.setSelected(row, col);
        });
        
        // 草稿按钮 - toggle模式
        containerPencil.setOnClickListener(v -> {
            pencilMode = !pencilMode;
            updatePencilButtonBackground();
        });
        
        // 撤销按钮
        containerUndo.setOnClickListener(v -> undo());
        updateUndoButtonEnabled();
        
        // Number buttons
        int[] btnIds = new int[]{
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
        
        // 控制第二行按钮样式：始终保持6个按钮，不需要的按钮保持透明占位
        // 第二行布局：btn_7(位置1), btn_8(位置2), btn_9(位置3), placeholder_1(位置4), placeholder_2(位置5), placeholder_3(位置6)
        // 总共6列，所有按钮都可见，不需要的保持透明，保证第二行高度和宽度一致
        Button btn7 = findViewById(R.id.btn_7);
        Button btn8 = findViewById(R.id.btn_8);
        Button btn9 = findViewById(R.id.btn_9);
        Button placeholder1 = findViewById(R.id.placeholder_1);
        Button placeholder2 = findViewById(R.id.placeholder_2);
        Button placeholder3 = findViewById(R.id.placeholder_3);
        
        // 所有占位按钮始终可见并保持完全透明
        if (placeholder1 != null) {
            placeholder1.setVisibility(View.VISIBLE);
            placeholder1.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        }
        if (placeholder2 != null) {
            placeholder2.setVisibility(View.VISIBLE);
            placeholder2.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        }
        if (placeholder3 != null) {
            placeholder3.setVisibility(View.VISIBLE);
            placeholder3.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        }
        
        // 根据size设置按钮是否激活
        // btn7: 需要size >=7，否则透明
        if (btn7 != null) {
            btn7.setVisibility(View.VISIBLE);
            if (size >= 7) {
                // 需要显示，设置正常样式
                btn7.setTextColor(getResources().getColor(android.R.color.white));
                btn7.setBackgroundTintList(getResources().getColorStateList(R.color.primary));
            } else {
                // 不需要显示，完全透明占位
                btn7.setTextColor(getResources().getColor(android.R.color.transparent));
                btn7.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            }
        }
        // btn8: 需要size >=8
        if (btn8 != null) {
            btn8.setVisibility(View.VISIBLE);
            if (size >= 8) {
                btn8.setTextColor(getResources().getColor(android.R.color.white));
                btn8.setBackgroundTintList(getResources().getColorStateList(R.color.primary));
            } else {
                btn8.setTextColor(getResources().getColor(android.R.color.transparent));
                btn8.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            }
        }
        // btn9: 需要size >=9
        if (btn9 != null) {
            btn9.setVisibility(View.VISIBLE);
            if (size >= 9) {
                btn9.setTextColor(getResources().getColor(android.R.color.white));
                btn9.setBackgroundTintList(getResources().getColorStateList(R.color.primary));
            } else {
                btn9.setTextColor(getResources().getColor(android.R.color.transparent));
                btn9.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            }
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

        containerReset.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("重置游戏")
                .setMessage("确定要清空所有方格并重置计时吗？")
                .setPositiveButton("是", (dialog, which) -> {
                    // 清空所有方格
                    for (int i = 0; i < puzzle.size; i++) {
                        for (int j = 0; j < puzzle.size; j++) {
                            puzzle.cells[i][j].clear();
                        }
                    }
                    // 恢复单格cage的提示（默认填充）
                    for (com.shy.kenken.model.Cage cage : puzzle.cages) {
                        if (cage.size() == 1) {
                            com.shy.kenken.model.Cell cell = cage.cells.get(0);
                            cell.value = cage.target;
                        }
                    }
                    // 重置选中状态
                    selectedRow = -1;
                    selectedCol = -1;
                    // 重置撤销栈
                    undoStack.clear();
                    updateUndoButtonEnabled();
                    // 重启计时
                    stopTimer();
                    startTimer();
                    // 刷新视图
                    kenKenView.invalidate();
                })
                .setNegativeButton("否", null)
                .show();
        });
        
        // 开始计时
        startTimer();
    }
    
    // 更新草稿按钮背景 - 激活时变深
    private void updatePencilButtonBackground() {
        LinearLayout containerPencil = findViewById(R.id.container_pencil);
        if (containerPencil != null) {
            try {
                if (pencilMode) {
                    containerPencil.setBackground(getResources().getDrawable(R.drawable.btn_bg_active));
                } else {
                    containerPencil.setBackground(getResources().getDrawable(R.drawable.btn_bg_white));
                }
            } catch (Exception e) {
                // Ignore drawable errors
            }
        }
    }
    
    private void saveMoveForUndo() {
        if (selectedRow >= 0 && selectedCol >= 0) {
            Move move = new Move(selectedRow, selectedCol, puzzle.cells[selectedRow][selectedCol]);
            undoStack.push(move);
            updateUndoButtonEnabled();
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
            // Restore old state
            cell.value = move.oldValue;
            System.arraycopy(move.oldPencilValues, 0, cell.pencilValues, 0, 10);
            kenKenView.invalidate();
            updateUndoButtonEnabled();
            checkComplete();
        }
    }
    
    private void updateUndoButtonEnabled() {
        // 不需要禁用，一直可点击
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
        updateUndoButtonEnabled();
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
            .setTitle("Check Errors")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show();
    }
    
    private void checkComplete() {
        if (puzzle.isComplete()) {
            stopTimer();
            new AlertDialog.Builder(this)
                .setTitle("恭喜！")
                .setMessage("你成功完成了" + size + "x" + size + "的聪明格！")
                .setPositiveButton("完成", (dialog, which) -> finish())
                .setNegativeButton("新游戏", (dialog, which) -> {
                    // 重新生成新游戏
                    generateNewPuzzle();
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
    }
}
