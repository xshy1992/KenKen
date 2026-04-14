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
    private boolean isTimerRunning = false;
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long elapsedTime = System.currentTimeMillis() - startTime;
            int seconds = (int) (elapsedTime / 1000);
            int minutes = seconds / 60;
            int hours = minutes / 60;
            minutes = minutes % 60;
            seconds = seconds % 60;
            if (hours > 0) {
                timerText.setText(String.format("%d:%02d:%02d", hours, minutes, seconds));
            } else {
                timerText.setText(String.format("%d:%02d", minutes, seconds));
            }
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
            long elapsedTime = System.currentTimeMillis() - startTime;
            int totalSeconds = (int) (elapsedTime / 1000);
            int minutes = totalSeconds / 60;
            int hours = minutes / 60;
            minutes = minutes % 60;
            int seconds = totalSeconds % 60;
            
            String timeStr;
            if (hours > 0) {
                timeStr = String.format("%d小时%d分%d秒", hours, minutes, seconds);
            } else if (minutes > 0) {
                timeStr = String.format("%d分%d秒", minutes, seconds);
            } else {
                timeStr = String.format("%d秒", seconds);
            }
            
            String title;
            String message;
            
            // 根据难度(size)和用时给出不同文案，提供情绪价值
            if (size <= 3) {
                // 入门难度
                if (totalSeconds < 60) {
                    title = "🚀 神速完成！";
                    message = String.format("太棒了！你仅用%s就通关了%d×%d的聪明格\n不愧是天才！🎉", timeStr, size, size);
                } else if (totalSeconds < 180) {
                    title = "🎉 恭喜通关！";
                    message = String.format("完美解决！你用%s完成了%d×%d的入门挑战\n新手村毕业啦！👍", timeStr, size, size);
                } else {
                    title = "👏 成功过关！";
                    message = String.format("恭喜你完成了%d×%d的聪明格\n坚持就是胜利，太棒了！✨", size, size);
                }
            } else if (size <= 5) {
                // 中等难度
                if (totalSeconds < 180) {
                    title = "⚡ 太猛了！";
                    message = String.format("%s就解决了%d×%d的题目\n这手速，这脑力，不服不行！🏆", timeStr, size, size);
                } else if (totalSeconds < 480) {
                    title = "🏅 挑战成功！";
                    message = String.format("恭喜你，用%s完成了%d×%d的挑战\n逻辑满分，真棒！🎊", timeStr, size, size);
                } else {
                    title = "🎉 终于搞定啦！";
                    message = String.format("恭喜你攻克了%d×%d的聪明格\n耗时%s，慢慢来也一样能成功！💪", size, size, timeStr);
                }
            } else if (size <= 7) {
                // 较难难度
                if (totalSeconds < 480) {
                    title = "🔥 大神操作！";
                    message = String.format("%s通关%d×%d，这是什么神仙实力\n简直是降维打击！👑", timeStr, size, size);
                } else if (totalSeconds < 900) {
                    title = "🏆 硬核通关！";
                    message = String.format("恭喜大佬通关%d×%d难度\n用时%s，强大的逻辑思维！🥳", size, size, timeStr);
                } else {
                    title = "💪 恭喜攻克！";
                    message = String.format("不容易！你终于解决了%d×%d的难题\n耗时%s，你真的超有耐心超棒的！🌟", size, size, timeStr);
                }
            } else {
                // 8x9 超高难度
                if (totalSeconds < 900) {
                    title = "👑 神仙下凡！";
                    message = String.format("%s解决%d×%d？你是人类吗\n这已经是顶级水平了！🤯", timeStr, size, size);
                } else if (totalSeconds < 1800) {
                    title = "👑 传奇通关！";
                    message = String.format("居然真的被你通关了%d×%d\n用时%s，大佬请受我一拜！🙇", size, size, timeStr);
                } else {
                    title = "🌟 史诗完成！";
                    message = String.format("恭喜你！征服了最难的%d×%d聪明格\n耗时%s，能坚持下来你就是赢家！🏅", size, size, timeStr);
                }
            }
            
            new AlertDialog.Builder(this)
                .setTitle(title)
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
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
    }
}
