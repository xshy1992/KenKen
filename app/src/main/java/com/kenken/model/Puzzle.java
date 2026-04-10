package com.kenken.model;

import java.util.ArrayList;
import java.util.List;

public class Puzzle {
    public int size;  // 4, 5, or 6
    public Cell[][] cells;
    public List<Cage> cages;
    public int[][] cageId;  // which cage each cell belongs to
    
    public Puzzle(int size) {
        this.size = size;
        this.cells = new Cell[size][size];
        this.cageId = new int[size][size];
        this.cages = new ArrayList<>();
        
        // initialize cells
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                cells[i][j] = new Cell(i, j);
                cageId[i][j] = -1;
            }
        }
    }
    
    public void addCage(Cage cage) {
        int cageIndex = cages.size();
        cages.add(cage);
        for (Cell cell : cage.cells) {
            cageId[cell.row][cell.col] = cageIndex;
        }
    }
    
    public Cage getCage(int row, int col) {
        int id = cageId[row][col];
        if (id >= 0 && id < cages.size()) {
            return cages.get(id);
        }
        return null;
    }
    
    public boolean checkRow(int row) {
        boolean[] seen = new boolean[size + 1];
        for (int col = 0; col < size; col++) {
            int val = cells[row][col].value;
            if (val > 0) {
                if (seen[val]) return false;
                seen[val] = true;
            }
        }
        return true;
    }
    
    public boolean checkCol(int col) {
        boolean[] seen = new boolean[size + 1];
        for (int row = 0; row < size; row++) {
            int val = cells[row][col].value;
            if (val > 0) {
                if (seen[val]) return false;
                seen[val] = true;
            }
        }
        return true;
    }
    
    public boolean checkAllCages() {
        for (Cage cage : cages) {
            if (cage.isComplete() && !cage.checkConstraint()) {
                return false;
            }
        }
        return true;
    }
    
    public boolean isComplete() {
        // check all cells filled
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (cells[i][j].isEmpty()) return false;
            }
        }
        
        // check all rows
        for (int i = 0; i < size; i++) {
            if (!checkRow(i)) return false;
        }
        
        // check all cols
        for (int j = 0; j < size; j++) {
            if (!checkCol(j)) return false;
        }
        
        // check all cages
        return checkAllCages();
    }
    
    public boolean hasErrors() {
        // check rows
        for (int i = 0; i < size; i++) {
            if (!checkRow(i)) return true;
        }
        // check cols
        for (int j = 0; j < size; j++) {
            if (!checkCol(j)) return true;
        }
        // check cages
        if (!checkAllCages()) return true;
        return false;
    }
    
    public void clearAll() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                cells[i][j].clear();
            }
        }
    }
}
