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
import com.shy.kenken.model.Cage;
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
    private long pausedElapsedTime = 0;
    private boolean isTimerRunning = false;
    
    // 预生成缓存：提前生成好的下一题Puzzle，按size缓存
    private static Puzzle cachedPuzzle = null;
    private static int cachedSize = -1;
    private static boolean isPreGenerating = false;
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
        
        // 恢复已保存的计时状态（系统回收Activity后重新打开）
        if (savedInstanceState != null) {
            startTime = savedInstanceState.getLong("startTime", 0);
            pausedElapsedTime = savedInstanceState.getLong("pausedElapsedTime", 0);
            isTimerRunning = savedInstanceState.getBoolean("isTimerRunning", false);
            // 如果计时本来应该在运行，重新启动
            if (!isTimerRunning && !puzzle.isComplete()) {
                startTime = System.currentTimeMillis() - pausedElapsedTime;
            }
        }
        
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
        int[] btnIds = {
            R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };
        
        // 所有按钮都保持可见占位，不需要的设置为透明
        // 这样保证两行高度一致，对齐正确
        for (int i = 0; i < btnIds.length; i++) {
            final int num = i + 1;
            Button btn = findViewById(btnIds[i]);
            btn.setVisibility(View.VISIBLE);
            if (i < size) {
                // 需要显示，设置正常样式
                btn.setTextColor(getResources().getColor(android.R.color.white));
                btn.setBackgroundTintList(getResources().getColorStateList(R.color.primary));
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
            } else {
                // 不需要显示，完全透明占位
                btn.setTextColor(getResources().getColor(android.R.color.transparent));
                btn.setBackgroundColor(getResources().getColor(R.color.background));
                btn.setBackgroundTintList(getResources().getColorStateList(R.color.background));
                btn.setClickable(false);
                btn.setEnabled(false);
                btn.setOnClickListener(null);
            }
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
            placeholder1.setTextColor(getResources().getColor(android.R.color.transparent));
            placeholder1.setBackgroundColor(getResources().getColor(R.color.background));
            placeholder1.setBackgroundTintList(getResources().getColorStateList(R.color.background));
            placeholder1.setClickable(false);
            placeholder1.setEnabled(false);
        }
        if (placeholder2 != null) {
            placeholder2.setVisibility(View.VISIBLE);
            placeholder2.setTextColor(getResources().getColor(android.R.color.transparent));
            placeholder2.setBackgroundColor(getResources().getColor(R.color.background));
            placeholder2.setBackgroundTintList(getResources().getColorStateList(R.color.background));
            placeholder2.setClickable(false);
            placeholder2.setEnabled(false);
        }
        if (placeholder3 != null) {
            placeholder3.setVisibility(View.VISIBLE);
            placeholder3.setTextColor(getResources().getColor(android.R.color.transparent));
            placeholder3.setBackgroundColor(getResources().getColor(R.color.background));
            placeholder3.setBackgroundTintList(getResources().getColorStateList(R.color.background));
            placeholder3.setClickable(false);
            placeholder3.setEnabled(false);
        }
        
        // 根据size设置按钮是否激活
        // btn7: 需要size >=7，否则透明
        if (btn7 != null) {
            btn7.setVisibility(View.VISIBLE);
            if (size >= 7) {
                // 需要显示，设置正常样式
                btn7.setTextColor(getResources().getColor(android.R.color.white));
                btn7.setBackgroundTintList(getResources().getColorStateList(R.color.primary));
                btn7.setClickable(true);
                btn7.setEnabled(true);
            } else {
                // 不需要显示，完全透明占位
                btn7.setTextColor(getResources().getColor(android.R.color.transparent));
                btn7.setBackgroundColor(getResources().getColor(R.color.background));
                btn7.setBackgroundTintList(getResources().getColorStateList(R.color.background));
                btn7.setClickable(false);
                btn7.setEnabled(false);
            }
        }
        // btn8: 需要size >=8
        if (btn8 != null) {
            btn8.setVisibility(View.VISIBLE);
            if (size >= 8) {
                btn8.setTextColor(getResources().getColor(android.R.color.white));
                btn8.setBackgroundTintList(getResources().getColorStateList(R.color.primary));
                btn8.setClickable(true);
                btn8.setEnabled(true);
            } else {
                btn8.setTextColor(getResources().getColor(android.R.color.transparent));
                btn8.setBackgroundColor(getResources().getColor(R.color.background));
                btn8.setBackgroundTintList(getResources().getColorStateList(R.color.background));
                btn8.setClickable(false);
                btn8.setEnabled(false);
            }
        }
        // btn9: 需要size >=9
        if (btn9 != null) {
            btn9.setVisibility(View.VISIBLE);
            if (size >= 9) {
                btn9.setTextColor(getResources().getColor(android.R.color.white));
                btn9.setBackgroundTintList(getResources().getColorStateList(R.color.primary));
                btn9.setClickable(true);
                btn9.setEnabled(true);
            } else {
                btn9.setTextColor(getResources().getColor(android.R.color.transparent));
                btn9.setBackgroundColor(getResources().getColor(R.color.background));
                btn9.setBackgroundTintList(getResources().getColorStateList(R.color.background));
                btn9.setClickable(false);
                btn9.setEnabled(false);
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
                .setMessage("确定要清空所有用户填充内容并重置计时吗？默认提示会保留。")
                .setPositiveButton("是", (dialog, which) -> {
                    // 只清空用户填充内容，保留默认填充（单格cage）
                    for (int i = 0; i < puzzle.size; i++) {
                        for (int j = 0; j < puzzle.size; j++) {
                            // 判断这个单元格所在的cage是否是单格
                            Cage cage = puzzle.getCage(i, j);
                            if (cage != null && cage.size() != 1) {
                                // 多格cage，清空用户填充内容
                                puzzle.cells[i][j].clear();
                            }
                            // 单格cage，保留默认填充内容，不清除
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
        selectedRow = -1;
        selectedCol = -1;
        undoStack.clear();
        
        // 优先使用预生成的缓存（如果size匹配）
        if (cachedPuzzle != null && cachedSize == size && !isPreGenerating) {
            // 直接用缓存好的
            puzzle = cachedPuzzle;
            cachedPuzzle = null;
            cachedSize = -1;
        } else {
            // 没有缓存，当前线程生成
            PuzzleGenerator generator = new PuzzleGenerator(size, true);
            puzzle = generator.generate();
        }
        
        updateUndoButtonEnabled();
        kenKenView.setPuzzle(puzzle);
        // 重启计时
        stopTimer();
        startTimer();
        
        // 异步预生成下一题
        preGenerateNextPuzzle();
    }
    
    // 异步预生成下一题
    private void preGenerateNextPuzzle() {
        if (isPreGenerating) return;  // 已经在生成了
        if (cachedPuzzle != null && cachedSize == size) return;  // 已经有缓存了
        
        isPreGenerating = true;
        new Thread(() -> {
            // 在后台生成下一题
            PuzzleGenerator generator = new PuzzleGenerator(size, true);
            Puzzle newPuzzle = generator.generate();
            // 更新缓存，切回主线程
            runOnUiThread(() -> {
                cachedPuzzle = newPuzzle;
                cachedSize = size;
                isPreGenerating = false;
            });
        }).start();
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
            long elapsedTotalMs = System.currentTimeMillis() - startTime;
            long elapsedSeconds = elapsedTotalMs / 1000;
            String message = getCompletionMessage(size, elapsedSeconds);
            new AlertDialog.Builder(this)
                .setTitle("恭喜！")
                .setMessage(message)
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
    protected void onPause() {
        super.onPause();
        // 离开页面时暂停计时
        if (isTimerRunning && !puzzle.isComplete()) {
            stopTimer();
            // 保存暂停时已经过去的时间
            pausedElapsedTime = System.currentTimeMillis() - startTime;
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 返回页面时继续计时（如果游戏没完成）
        if (!isTimerRunning && !puzzle.isComplete()) {
            // 从暂停时间继续
            startTime = System.currentTimeMillis() - pausedElapsedTime;
            startTimer();
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // 保存计时状态，防止系统回收Activity后丢失已用时间
        outState.putLong("startTime", startTime);
        outState.putLong("pausedElapsedTime", pausedElapsedTime);
        outState.putBoolean("isTimerRunning", isTimerRunning);
        super.onSaveInstanceState(outState);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
    }
    
    // 根据难度和时间生成不同完成文案
    private String getCompletionMessage(int size, long seconds) {
        if (size <= 4) {
            if (seconds < 60) {
                return String.format("太棒了！你只用了%d秒就完成了%d×%d的聪明格，反应很快！", seconds, size, size);
            } else if (seconds < 180) {
                return String.format("恭喜！你用了%d分%d秒完成了%d×%d的聪明格，很棒！", seconds / 60, seconds % 60, size, size);
            } else {
                return String.format("恭喜完成！你用了%d分%d秒完成了%d×%d的聪明格，继续加油！", seconds / 60, seconds % 60, size, size);
            }
        } else if (size <= 6) {
            if (seconds < 180) {
                return String.format("太厉害了！你只用了%d分%d秒完成了%d×%d的聪明格，逻辑思维超强！", seconds / 60, seconds % 60, size, size);
            } else if (seconds < 480) {
                return String.format("恭喜！你用了%d分%d秒完成了%d×%d的聪明格，非常不错！", seconds / 60, seconds % 60, size, size);
            } else {
                return String.format("恭喜完成！你用了%d分%d秒完成了%d×%d的聪明格，坚持就是胜利！", seconds / 60, seconds % 60, size, size);
            }
        } else {
            if (seconds < 480) {
                return String.format("大神！你只用了%d分%d秒就完成了%d×%d的聪明格，这水平可以去比赛了！", seconds / 60, seconds % 60, size, size);
            } else if (seconds < 900) {
                return String.format("太强了！你用了%d分%d秒完成了%d×%d的聪明格，相当出色！", seconds / 60, seconds % 60, size, size);
            } else {
                return String.format("恭喜毅力王者！你用了%d分%d秒完成了%d×%d的大尺寸聪明格，太不容易了！", seconds / 60, seconds % 60, size, size);
            }
        }
    }
}
