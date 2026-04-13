package com.shy.kenken.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.shy.kenken.model.Cage;
import com.shy.kenken.model.Cell;
import com.shy.kenken.model.Puzzle;

public class KenKenView extends View {
    private Puzzle puzzle;
    private int selectedRow = -1;
    private int selectedCol = -1;
    private Paint gridPaint = new Paint();
    private Paint thickLinePaint = new Paint();
    private Paint selectedCellPaint = new Paint();
    private Paint numberPaint = new Paint();
    private Paint cageLabelPaint = new Paint();
    private Paint pencilPaint = new Paint();
    private Paint errorPaint = new Paint();
    
    private float cellSize;
    private float padding = 8;
    
    public interface OnCellClickListener {
        void onCellClick(int row, int col);
    }
    
    private OnCellClickListener listener;
    
    public KenKenView(Context context) {
        super(context);
        init();
    }
    
    public KenKenView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    private void init() {
        // 罗小黑治愈卡通风格配色 - 更柔和
        gridPaint.setColor(0xFFAAAAAA);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(2);
        gridPaint.setAntiAlias(true);
        
        thickLinePaint.setColor(0xFF555555);
        thickLinePaint.setStyle(Paint.Style.STROKE);
        thickLinePaint.setStrokeWidth(6);
        thickLinePaint.setAntiAlias(true);
        
        selectedCellPaint.setColor(0xFFE8F4F8);
        selectedCellPaint.setStyle(Paint.Style.FILL);
        
        errorPaint.setColor(0xFFFFF0F0);
        errorPaint.setStyle(Paint.Style.FILL);
        
        numberPaint.setColor(0xFF333333);
        numberPaint.setTextAlign(Paint.Align.CENTER);
        numberPaint.setAntiAlias(true);
        
        cageLabelPaint.setColor(0xFF666666);
        cageLabelPaint.setTextAlign(Paint.Align.LEFT);
        cageLabelPaint.setAntiAlias(true);
        
        pencilPaint.setColor(0xFF888888);
        pencilPaint.setTextAlign(Paint.Align.CENTER);
        pencilPaint.setAntiAlias(true);
        
        setBackgroundColor(0xFFFFFFFF);
    }
    
    public void setPuzzle(Puzzle puzzle) {
        this.puzzle = puzzle;
        selectedRow = -1;
        selectedCol = -1;
        invalidate();
    }
    
    public void setOnCellClickListener(OnCellClickListener listener) {
        this.listener = listener;
    }
    
    public void setSelected(int row, int col) {
        this.selectedRow = row;
        this.selectedCol = col;
        invalidate();
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = Math.min(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
        setMeasuredDimension(size, size);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        if (puzzle == null) return;
        
        int viewSize = getWidth();
        cellSize = (viewSize - 2 * padding) / (float)puzzle.size;
        
        // Fill cells
        for (int i = 0; i < puzzle.size; i++) {
            for (int j = 0; j < puzzle.size; j++) {
                float left = padding + j * cellSize;
                float top = padding + i * cellSize;
                float right = left + cellSize;
                float bottom = top + cellSize;
                
                // Check if has error
                boolean hasError = false;
                if (!puzzle.checkRow(i) || !puzzle.checkCol(j)) {
                    hasError = true;
                } else {
                    Cage cage = puzzle.getCage(i, j);
                    if (cage != null && cage.isComplete() && !cage.checkConstraint()) {
                        hasError = true;
                    }
                }
                
                if (i == selectedRow && j == selectedCol) {
                    canvas.drawRect(left, top, right, bottom, selectedCellPaint);
                } else if (hasError) {
                    canvas.drawRect(left, top, right, bottom, errorPaint);
                }
            }
        }
        
        // Draw numbers
        for (int i = 0; i < puzzle.size; i++) {
            for (int j = 0; j < puzzle.size; j++) {
                Cell cell = puzzle.cells[i][j];
                float centerX = padding + j * cellSize + cellSize / 2;
                float centerY = padding + i * cellSize + cellSize / 2;
                
                if (!cell.isEmpty()) {
                    // 根据grid大小自适应数字大小，保证9x9也能完整显示
                    // 计算公式：cellSize * (0.5 - (size-4) * 0.03) 保证逐渐缩小，最小保留cellSize的25%
                    float baseFactor = 0.5f;
                    float reduction = Math.max(0, (puzzle.size - 4) * 0.03f);
                    float textSizeFactor = baseFactor - reduction;
                    float textSize = cellSize * textSizeFactor;
                    float minTextSize = cellSize * 0.25f; // 最小不低于25%，保证能看清
                    numberPaint.setTextSize(Math.max(textSize, minTextSize));
                    canvas.drawText(String.valueOf(cell.value), centerX, centerY + numberPaint.getTextSize()/3, numberPaint);
                } else if (cell.hasPencilMarks()) {
                    // Draw pencil marks strictly in bottom 50% of cell
                    int availableNumbers = 0;
                    for (int num = 1; num <= puzzle.size; num++) {
                        if (cell.pencilValues[num]) availableNumbers++;
                    }
                    if (availableNumbers == 0) return;
                    
                    // 严格计算行列，保证所有数字都在方格下半部分
                    int cols;
                    if (puzzle.size <= 4) cols = puzzle.size;
                    else if (puzzle.size <= 6) cols = 3;
                    else if (puzzle.size <= 9) cols = 3; // 3列足够放下9个数字在三行
                    else cols = 4;
                    
                    int rows = (int) Math.ceil((double) puzzle.size / cols);
                    
                    // 严格约束在下半部分
                    float bottomHalfHeight = cellSize * 0.48f; // 只占下半部分48%，留有边距不碰边框
                    float leftRightMargin = cellSize * 0.05f; // 左右边距各5%
                    float totalWidth = cellSize - 2 * leftRightMargin;
                    
                    float spacingX = totalWidth / cols;
                    float spacingY = bottomHalfHeight / rows;
                    
                    float startX = padding + (j * cellSize) + leftRightMargin + spacingX/2;
                    float startY = padding + (i * cellSize) + cellSize/2 + spacingY/2;
                    
                    // 计算最大能容纳的文字大小，保证不会超出spacing
                    float maxTextSize = Math.min(spacingX * 0.65f, spacingY * 0.75f);
                    pencilPaint.setTextSize(maxTextSize);
                    
                    int count = 0;
                    for (int num = 1; num <= puzzle.size; num++) {
                        if (cell.pencilValues[num]) {
                            int r = count / cols;
                            int c = count % cols;
                            float px = startX + c * spacingX;
                            float py = startY + r * spacingY;
                            canvas.drawText(String.valueOf(num), px, py, pencilPaint);
                            count++;
                        }
                    }
                }
            }
        }
        
        // Draw cage labels
        for (Cage cage : puzzle.cages) {
            if (cage.size() > 0) {
                Cell first = cage.cells.get(0);
                float left = padding + first.col * cellSize;
                float top = padding + first.row * cellSize;
                String label = String.valueOf(cage.target);
                if (cage.operation != '#') {
                    char op = cage.operation;
                    if (op == '*') {
                        op = '×';
                    } else if (op == '/') {
                        op = '÷';
                    }
                    label = label + op;
                }
                cageLabelPaint.setTextSize(cellSize * 0.22f);
                // 标签严格放在方格左上角，距离边界2dp
                canvas.drawText(label, left + 2, top + cageLabelPaint.getTextSize() + 2, cageLabelPaint);
            }
        }
        
        // Draw grid lines
        for (int i = 0; i <= puzzle.size; i++) {
            float pos = padding + i * cellSize;
            canvas.drawLine(padding, pos, padding + puzzle.size * cellSize, pos, gridPaint);
            canvas.drawLine(pos, padding, pos, padding + puzzle.size * cellSize, gridPaint);
        }
        
        // Draw thick lines for cage borders
        drawCageBorders(canvas);
    }
    
    private void drawCageBorders(Canvas canvas) {
        boolean[][] topBorder = new boolean[puzzle.size + 1][puzzle.size + 1];
        boolean[][] leftBorder = new boolean[puzzle.size + 1][puzzle.size + 1];
        
        for (int i = 0; i < puzzle.size; i++) {
            for (int j = 0; j < puzzle.size; j++) {
                int id = puzzle.cageId[i][j];
                // Check top neighbor
                if (i == 0 || puzzle.cageId[i-1][j] != id) {
                    topBorder[i][j] = true;
                }
                // Check left neighbor
                if (j == 0 || puzzle.cageId[i][j-1] != id) {
                    leftBorder[i][j] = true;
                }
                // Check bottom neighbor
                if (i == puzzle.size - 1 || puzzle.cageId[i+1][j] != id) {
                    topBorder[i+1][j] = true;
                }
                // Check right neighbor
                if (j == puzzle.size - 1 || puzzle.cageId[i][j+1] != id) {
                    leftBorder[i][j+1] = true;
                }
            }
        }
        
        // Draw horizontal borders
        for (int i = 0; i <= puzzle.size; i++) {
            for (int j = 0; j < puzzle.size; j++) {
                if (topBorder[i][j]) {
                    float y = padding + i * cellSize;
                    float x1 = padding + j * cellSize;
                    float x2 = padding + (j + 1) * cellSize;
                    canvas.drawLine(x1, y, x2, y, thickLinePaint);
                }
            }
        }
        
        // Draw vertical borders
        for (int i = 0; i < puzzle.size; i++) {
            for (int j = 0; j <= puzzle.size; j++) {
                if (leftBorder[i][j]) {
                    float x = padding + j * cellSize;
                    float y1 = padding + i * cellSize;
                    float y2 = padding + (i + 1) * cellSize;
                    canvas.drawLine(x, y1, x, y2, thickLinePaint);
                }
            }
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && listener != null) {
            float x = event.getX() - padding;
            float y = event.getY() - padding;
            if (x >= 0 && y >= 0) {
                int col = (int)(x / cellSize);
                int row = (int)(y / cellSize);
                if (row < puzzle.size && col < puzzle.size) {
                    listener.onCellClick(row, col);
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }
}
