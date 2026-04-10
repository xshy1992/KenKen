package com.kenken.generator;

import com.kenken.model.Cage;
import com.kenken.model.Cell;
import com.kenken.model.Puzzle;

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
        // First generate a valid solved grid
        int[][] solution = generateValidSolution();
        // Then split into cages and compute operations
        return createPuzzleFromSolution(solution);
    }
    
    private int[][] generateValidSolution() {
        int[][] grid = new int[size][size];
        fillGrid(grid, 0, 0);
        // shuffle a bit to randomize
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
        // check row
        for (int c = 0; c < col; c++) {
            if (grid[row][c] == num) return false;
        }
        // check col
        for (int r = 0; r < row; r++) {
            if (grid[r][col] == num) return false;
        }
        return true;
    }
    
    private void randomizeGrid(int[][] grid) {
        // swap rows occasionally to increase randomness
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
        
        // Build connectivity map for region growing
        boolean[][] used = new boolean[size][size];
        List<int[]> available = new ArrayList<>();
        
        // Initialize available cells
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                available.add(new int[]{i, j});
            }
        }
        
        Collections.shuffle(available, random);
        
        while (!available.isEmpty()) {
            // Start a new cage from a random available cell
            int[] start = available.remove(available.size() - 1);
            int r = start[0];
            int c = start[1];
            
            if (used[r][c]) continue;
            
            // Decide cage size: 1-4 cells
            int maxSize = Math.min(4, available.size() + 1);
            int cageSize;
            if (size <= 4) {
                cageSize = random.nextInt(maxSize) + 1;
            } else {
                cageSize = random.nextInt(maxSize) + 1;
            }
            
            Cage cage = new Cage(0, '#');
            addCellToCage(cage, puzzle, solution, r, c, used);
            
            // Region growing - add adjacent cells
            for (int s = 1; s < cageSize && !available.isEmpty(); s++) {
                List<int[]> neighbors = getAdjacentUnused(cage, used);
                if (neighbors.isEmpty()) break;
                int[] neighbor = neighbors.get(random.nextInt(neighbors.size()));
                available.removeIf(p -> p[0] == neighbor[0] && p[1] == neighbor[1]);
                addCellToCage(cage, puzzle, solution, neighbor[0], neighbor[1], used);
            }
            
            // compute target and operation
            computeOperationAndTarget(cage, solution);
            
            puzzle.addCage(cage);
        }
        
        return puzzle;
    }
    
    private void addCellToCage(Cage cage, Puzzle puzzle, int[][] solution, int r, int c, boolean[][] used) {
        used[r][c] = true;
        puzzle.cells[r][c].value = 0;  // start empty
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
            // single cell - just the value
            int val = solution[cage.cells.get(0).row][cage.cells.get(0).col];
            cage.target = val;
            cage.operation = '#';
            return;
        }
        
        // Get all values
        List<Integer> values = new ArrayList<>();
        for (Cell cell : cage.cells) {
            values.add(solution[cell.row][cell.col]);
        }
        
        // Choose operation based on size and values
        // For 2 cells, any operation possible; for more, + or ×
        char[] possibleOps;
        if (cage.size() == 2) {
            possibleOps = new char[]{'+', '-', '×', '÷'};
        } else {
            possibleOps = new char[]{'+', '×'};  // - and ÷ only for 2 cells
        }
        
        // pick a random operation
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
            case '×': {
                int product = 1;
                for (int v : values) product *= v;
                cage.target = product;
                break;
            }
            case '÷': {
                int a = Math.max(values.get(0), values.get(1));
                int b = Math.min(values.get(0), values.get(1));
                // ensure divisible
                if (a % b != 0) {
                    // fall back to subtraction
                    cage.operation = '-';
                    cage.target = Math.abs(a - b);
                } else {
                    cage.target = a / b;
                }
                break;
            }
        }
    }
    
    private boolean canDivide(int a, int b) {
        return b != 0 && a % b == 0;
    }
}
