package com.kenken.model;

import java.util.ArrayList;
import java.util.List;

public class Cage {
    public int target;
    public char operation;  // '+', '-', '×', '÷', '#' (single cell)
    public List<Cell> cells;
    
    public Cage(int target, char operation) {
        this.target = target;
        this.operation = operation;
        this.cells = new ArrayList<>();
    }
    
    public void addCell(Cell cell) {
        cells.add(cell);
    }
    
    public int size() {
        return cells.size();
    }
    
    public boolean checkConstraint() {
        if (operation == '#') {
            // single cell, just check equals target
            return cells.get(0).value == target;
        }
        
        // collect non-empty values
        List<Integer> values = new ArrayList<>();
        for (Cell cell : cells) {
            if (!cell.isEmpty()) {
                values.add(cell.value);
            }
        }
        
        // if some cells are empty, not complete yet - return true for now
        if (values.size() < cells.size()) {
            return true;
        }
        
        switch (operation) {
            case '+': {
                int sum = 0;
                for (int v : values) sum += v;
                return sum == target;
            }
            case '-': {
                int max = Math.max(values.get(0), values.get(1));
                int min = Math.min(values.get(0), values.get(1));
                return (max - min) == target;
            }
            case '×': {
                int product = 1;
                for (int v : values) product *= v;
                return product == target;
            }
            case '÷': {
                int max = Math.max(values.get(0), values.get(1));
                int min = Math.min(values.get(0), values.get(1));
                return min != 0 && (max / min) == target && (max % min) == 0;
            }
            default:
                return true;
        }
    }
    
    public boolean isComplete() {
        for (Cell cell : cells) {
            if (cell.isEmpty()) return false;
        }
        return true;
    }
}
