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
    
    public PuzzleGenerator(int size) {
        this.size = size;
        this.random = new Random();
    }
    
    public Puzzle generate() {
        // 1. 生成一个完整的有效解
        int[][] solution = generateValidSolution();
        
        // 2. 创建cages并计算操作符
        Puzzle puzzle = createPuzzleFromSolution(solution);
        
        // 3. 清空所有数字
        clearAllCells(puzzle);
        
        // 4. 逐步添加提示数字，直到有唯一解
        addCluesUntilUniqueSolution(puzzle, solution);
        
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
    
    private void addCluesUntilUniqueSolution(Puzzle puzzle, int[][] solution) {
        // 业界最佳实践：只保留单格cage的数字作为提示
        // 单格cage的operation是'#'，target就是该格子的正确答案
        // 这样既保证了谜题质量，又避免了随机性
        
        for (Cage cage : puzzle.cages) {
            if (cage.size() == 1) {
                // 单格cage，保留其target作为提示
                Cell cell = cage.cells.get(0);
                cell.value = cage.target;
            }
        }
        
        // 对于小尺寸（<7），检查单格cage是否足够保证唯一解
        // 如果还不够，才逐步添加提示
        if (size < 7) {
            // 先检查单格cage是否已经足够
            if (hasUniqueSolution(puzzle)) {
                // 单格cage已经足够，不需要额外提示
                return;
            }
            
            // 单格cage不够，需要添加额外提示
            List<int[]> emptyCells = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    if (puzzle.cells[i][j].value == 0) {
                        emptyCells.add(new int[]{i, j});
                    }
                }
            }
            
            Collections.shuffle(emptyCells, random);
            
            for (int[] cell : emptyCells) {
                int r = cell[0];
                int c = cell[1];
                
                // 检查是否已经有唯一解
                if (hasUniqueSolution(puzzle)) {
                    break;
                }
                
                // 添加提示
                puzzle.cells[r][c].value = solution[r][c];
            }
        }
    }
    
    private boolean hasUniqueSolution(Puzzle puzzle) {
        // 使用回溯算法计算解的数量
        // 如果超过1个解，返回false
        SolutionCounter counter = new SolutionCounter();
        countSolutions(puzzle, counter, 0, 0);
        return counter.count == 1;
    }
    
    private static class SolutionCounter {
        int count = 0;
        static final int MAX_SOLUTIONS = 2;  // 只需要知道是否超过1个
    }
    
    private void countSolutions(Puzzle puzzle, SolutionCounter counter, int row, int col) {
        if (counter.count >= SolutionCounter.MAX_SOLUTIONS) return;
        
        if (row == size) {
            counter.count++;
            return;
        }
        
        if (col == size) {
            countSolutions(puzzle, counter, row + 1, 0);
            return;
        }
        
        // 如果这个单元格已经有值（提示），跳过
        if (puzzle.cells[row][col].value != 0) {
            countSolutions(puzzle, counter, row, col + 1);
            return;
        }
        
        // 尝试所有可能的值
        for (int num = 1; num <= size; num++) {
            if (isValidPlacement(puzzle, row, col, num)) {
                puzzle.cells[row][col].value = num;
                countSolutions(puzzle, counter, row, col + 1);
                puzzle.cells[row][col].value = 0;
            }
        }
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
