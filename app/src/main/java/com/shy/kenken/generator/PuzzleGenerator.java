package com.shy.kenken.generator;

import com.shy.kenken.model.Cage;
import com.shy.kenken.model.Cell;
import com.shy.kenken.model.Puzzle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PuzzleGenerator {
    private int size;
    private Random random;
    private static final int MAX_GENERATION_ATTEMPTS = 10;  // 最大生成尝试次数
    private static final int MIN_CLUES_TO_KEEP = 4;  // 最少保留的提示数
    
    public PuzzleGenerator(int size) {
        this.size = size;
        this.random = new Random();
    }
    
    public Puzzle generate() {
        // 多次尝试，直到生成一个有唯一解的puzzle
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            // 1. 生成一个完整的有效解
            int[][] solution = generateValidSolution();
            
            // 2. 创建cages并计算操作符
            Puzzle puzzle = createPuzzleFromSolution(solution);
            
            // 3. 清空所有数字
            clearAllCells(puzzle);
            
            // 4. 逐步添加提示数字，直到有唯一解
            if (addCluesUntilUniqueSolution(puzzle, solution)) {
                return puzzle;  // 找到唯一解，成功返回
            }
        }
        
        // 如果多次尝试都失败，退化为使用所有单格提示+额外随机提示保证唯一
        int[][] solution = generateValidSolution();
        Puzzle puzzle = createPuzzleFromSolution(solution);
        clearAllCells(puzzle);
        forceUniqueSolutionByAddingClues(puzzle, solution);
        return puzzle;
    }
    
    private int[][] generateValidSolution() {
        int[][] grid = new int[size][size];
        fillGrid(grid, 0, 0);
        // 随机化网格
        randomizeGrid(grid);
        return grid;
    }
    
    private boolean fillGrid(int[][] grid, int row, int col) {
        if (row == size) return true;
        if (col == size) return fillGrid(grid, row + 1, 0);
        
        List<Integer> nums = new ArrayList<>();
        for (int i = 1; i <= size; i++) nums.add(i);
        Collections.shuffle(nums, random);
        
        for (int num : nums) {
            if (isValid(grid, row, col, num)) {
                grid[row][col] = num;
                if (fillGrid(grid, row, col + 1)) {
                    return true;
                }
                grid[row][col] = 0;
            }
        }
        return false;
    }
    
    private boolean isValid(int[][] grid, int row, int col, int num) {
        // 检查行
        for (int c = 0; c < col; c++) {
            if (grid[row][c] == num) return false;
        }
        // 检查列
        for (int r = 0; r < row; r++) {
            if (grid[r][col] == num) return false;
        }
        return true;
    }
    
    private void randomizeGrid(int[][] grid) {
        // 随机交换行以增加随机性
        for (int i = 0; i < size; i++) {
            if (random.nextBoolean()) {
                int r1 = random.nextInt(size);
                int r2 = random.nextInt(size);
                int[] temp = grid[r1].clone();
                System.arraycopy(grid[r2], 0, grid[r1], 0, size);
                System.arraycopy(temp, 0, grid[r2], 0, size);
            }
        }
    }
    
    private Puzzle createPuzzleFromSolution(int[][] solution) {
        Puzzle puzzle = new Puzzle(size);
        
        boolean[][] used = new boolean[size][size];
        List<int[]> available = new ArrayList<>();
        
        // 初始化可用单元格
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                available.add(new int[]{i, j});
            }
        }
        
        Collections.shuffle(available, random);
        
        while (!available.isEmpty()) {
            // 从随机可用单元格开始一个新的cage
            int[] start = available.remove(available.size() - 1);
            int r = start[0];
            int c = start[1];
            
            if (used[r][c]) continue;
            
            // 决定cage大小：1-4
            int maxSize = Math.min(4, available.size() + 1);
            int cageSize = random.nextInt(maxSize) + 1;
            
            Cage cage = new Cage(0, '#');
            addCellToCage(cage, puzzle, solution, r, c, used);
            
            // 区域增长 - 添加相邻单元格
            for (int s = 1; s < cageSize && !available.isEmpty(); s++) {
                List<int[]> neighbors = getAdjacentUnused(cage, used);
                if (neighbors.isEmpty()) break;
                int[] neighbor = neighbors.get(random.nextInt(neighbors.size()));
                available.removeIf(p -> p[0] == neighbor[0] && p[1] == neighbor[1]);
                addCellToCage(cage, puzzle, solution, neighbor[0], neighbor[1], used);
            }
            
            // 计算目标和操作符
            computeOperationAndTarget(cage, solution);
            
            puzzle.addCage(cage);
        }
        
        return puzzle;
    }
    
    private void addCellToCage(Cage cage, Puzzle puzzle, int[][] solution, int r, int c, boolean[][] used) {
        used[r][c] = true;
        puzzle.cells[r][c].value = 0;  // 初始为空
        cage.addCell(puzzle.cells[r][c]);
    }
    
    private List<int[]> getAdjacentUnused(Cage cage, boolean[][] used) {
        List<int[]> result = new ArrayList<>();
        for (Cell cell : cage.cells) {
            int r = cell.row;
            int c = cell.col;
            checkAndAdd(r - 1, c, used, result);
            checkAndAdd(r + 1, c, used, result);
            checkAndAdd(r, c - 1, used, result);
            checkAndAdd(r, c + 1, used, result);
        }
        return result;
    }
    
    private void checkAndAdd(int r, int c, boolean[][] used, List<int[]> result) {
        if (r >= 0 && r < size && c >= 0 && c < size && !used[r][c]) {
            if (!result.stream().anyMatch(p -> p[0] == r && p[1] == c)) {
                result.add(new int[]{r, c});
            }
        }
    }
    
    private void computeOperationAndTarget(Cage cage, int[][] solution) {
        if (cage.size() == 1) {
            // 单个单元格 - 直接是值
            int val = solution[cage.cells.get(0).row][cage.cells.get(0).col];
            cage.target = val;
            cage.operation = '#';
            return;
        }
        
        // 获取所有值
        List<Integer> values = new ArrayList<>();
        for (Cell cell : cage.cells) {
            values.add(solution[cell.row][cell.col]);
        }
        
        // 根据大小和值选择操作符
        char[] possibleOps;
        if (cage.size() == 2) {
            possibleOps = new char[]{'+', '-', '*', '/'};
        } else {
            possibleOps = new char[]{'+', '*'};  // - 和 / 只适用于2个单元格
        }
        
        // 随机选择操作符
        char op = possibleOps[random.nextInt(possibleOps.length)];
        cage.operation = op;
        
        switch (op) {
            case '+': {
                int sum = 0;
                for (int v : values) sum += v;
                cage.target = sum;
                break;
            }
            case '-': {
                int a = values.get(0);
                int b = values.get(1);
                cage.target = Math.abs(a - b);
                break;
            }
            case '*': {
                int product = 1;
                for (int v : values) product *= v;
                cage.target = product;
                break;
            }
            case '/': {
                int a = Math.max(values.get(0), values.get(1));
                int b = Math.min(values.get(0), values.get(1));
                // 确保可整除
                if (a % b != 0) {
                    // 回退到减法
                    cage.operation = '-';
                    cage.target = Math.abs(a - b);
                } else {
                    cage.target = a / b;
                }
                break;
            }
        }
    }
    
    private void clearAllCells(Puzzle puzzle) {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                puzzle.cells[i][j].value = 0;
            }
        }
    }
    
    private boolean addCluesUntilUniqueSolution(Puzzle puzzle, int[][] solution) {
        // 第一步：只填充单格cage（操作符为'#'），这是默认要求
        int filledCount = 0;
        List<int[]> emptyCells = new ArrayList<>();
        
        for (Cage cage : puzzle.cages) {
            if (cage.size() == 1) {
                Cell cell = cage.cells.get(0);
                cell.value = solution[cell.row][cell.col];
                filledCount++;
            }
        }
        
        // 收集所有空单元格
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (puzzle.cells[r][c].value == 0) {
                    emptyCells.add(new int[]{r, c});
                }
            }
        }
        
        // 检查现在是否已经有唯一解
        if (hasUniqueSolution(puzzle)) {
            return true;
        }
        
        // 如果不唯一，逐步随机添加提示直到唯一解
        Collections.shuffle(emptyCells, random);
        
        while (!emptyCells.isEmpty() && filledCount < size * size - 1) {
            // 添加一个提示
            int[] cell = emptyCells.remove(emptyCells.size() - 1);
            int r = cell[0];
            int c = cell[1];
            puzzle.cells[r][c].value = solution[r][c];
            filledCount++;
            
            // 检查现在是否唯一
            if (hasUniqueSolution(puzzle)) {
                return true;
            }
        }
        
        // 即使填满了也不唯一？这很不可能
        return hasUniqueSolution(puzzle);
    }
    
    private void forceUniqueSolutionByAddingClues(Puzzle puzzle, int[][] solution) {
        // 后备方法：保证唯一解，强制添加足够多的提示
        // 先填充所有单格cage
        for (Cage cage : puzzle.cages) {
            if (cage.size() == 1) {
                Cell cell = cage.cells.get(0);
                cell.value = solution[cell.row][cell.col];
            }
        }
        
        // 如果还不唯一，继续添加直到唯一
        while (!hasUniqueSolution(puzzle)) {
            // 随机找一个空单元格填充
            List<int[]> emptyCells = new ArrayList<>();
            for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {
                    if (puzzle.cells[r][c].value == 0) {
                        emptyCells.add(new int[]{r, c});
                    }
                }
            }
            
            if (emptyCells.isEmpty()) break;
            
            int[] cell = emptyCells.get(random.nextInt(emptyCells.size()));
            puzzle.cells[cell[0]][cell[1]].value = solution[cell[0]][cell[1]];
        }
    }
    
    private boolean hasUniqueSolution(Puzzle puzzle) {
        // 使用回溯算法计算解的数量
        // 如果超过1个解，返回false
        // 使用启发式搜索：优先搜索可能性最少的单元格，提高剪枝效率
        SolutionCounter counter = new SolutionCounter();
        backtrackCountSolutions(puzzle, counter);
        restorePuzzle(puzzle);  // 恢复原始状态
        return counter.count == 1;
    }
    
    private static class SolutionCounter {
        int count = 0;
        static final int MAX_SOLUTIONS = 2;  // 只需要知道是否超过1个
        long startTime;
        static final long MAX_DURATION_MS = 2000;  // 最多搜索2秒，防止卡住
        
        public SolutionCounter() {
            this.startTime = System.currentTimeMillis();
        }
        
        public boolean shouldStop() {
            return count >= MAX_SOLUTIONS || 
                   (System.currentTimeMillis() - startTime) > MAX_DURATION_MS;
        }
    }
    
    private void backtrackCountSolutions(Puzzle puzzle, SolutionCounter counter) {
        if (counter.shouldStop()) return;
        
        // 找到下一个空单元格（选择约束最多的，启发式加快搜索）
        Cell next = findBestEmptyCell(puzzle);
        if (next == null) {
            // 所有单元格都填满了，找到一个解
            counter.count++;
            return;
        }
        
        // 获取所有可能的有效值
        List<Integer> candidates = getValidCandidates(puzzle, next.row, next.col);
        if (candidates.isEmpty()) {
            return;  // 剪枝
        }
        
        // 尝试每个候选值
        for (int num : candidates) {
            if (counter.shouldStop()) break;
            puzzle.cells[next.row][next.col].value = num;
            backtrackCountSolutions(puzzle, counter);
            puzzle.cells[next.row][next.col].value = 0;
        }
    }
    
    // 找到约束最多的空单元格，优先搜索它（MRV启发式）
    private Cell findBestEmptyCell(Puzzle puzzle) {
        Cell best = null;
        int minCandidates = Integer.MAX_VALUE;
        
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (puzzle.cells[r][c].value == 0) {
                    int candidates = countValidCandidates(puzzle, r, c);
                    if (candidates <= 1) {
                        // 只有0或1个候选，这是最好的选择
                        return puzzle.cells[r][c];
                    }
                    if (candidates < minCandidates) {
                        minCandidates = candidates;
                        best = puzzle.cells[r][c];
                    }
                }
            }
        }
        
        return best;
    }
    
    // 计算某个单元格的有效候选数
    private int countValidCandidates(Puzzle puzzle, int row, int col) {
        int count = 0;
        for (int num = 1; num <= size; num++) {
            if (isValidPlacement(puzzle, row, col, num)) {
                count++;
            }
        }
        return count;
    }
    
    // 获取某个单元格的所有有效候选
    private List<Integer> getValidCandidates(Puzzle puzzle, int row, int col) {
        List<Integer> candidates = new ArrayList<>();
        for (int num = 1; num <= size; num++) {
            if (isValidPlacement(puzzle, row, col, num)) {
                candidates.add(num);
            }
        }
        return candidates;
    }
    
    // 恢复puzzle到原始状态（清除回溯时填入的数字）
    private void restorePuzzle(Puzzle puzzle) {
        // 只保留原来就有的提示（值不为0且不是我们搜索添加的）
        // 实际上我们在回溯时已经重置了所有添加的值，所以这里不需要做任何事情
        // 这个方法只是为了安全，防止提前退出时留下值
    }
    
    private boolean isValidPlacement(Puzzle puzzle, int row, int col, int num) {
        // 检查行
        for (int c = 0; c < size; c++) {
            if (c != col && puzzle.cells[row][c].value == num) return false;
        }
        // 检查列
        for (int r = 0; r < size; r++) {
            if (r != row && puzzle.cells[r][col].value == num) return false;
        }
        // 检查cage约束
        Cage cage = puzzle.getCage(row, col);
        if (cage != null && !cage.checkPartialConstraint(row, col, num, size)) {
            return false;
        }
        return true;
    }
}
