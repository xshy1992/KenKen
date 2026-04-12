package com.shy.kenken.model;

import java.util.ArrayList;
import java.util.List;

public class Cage {
    public int target;
    public char operation;  // '+', '-', '*', '/', '#' (single cell)
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
            case '*': {
                int product = 1;
                for (int v : values) product *= v;
                return product == target;
            }
            case '/': {
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
    
    // 检查部分约束 - 用于回溯求解器
    public boolean checkPartialConstraint(int row, int col, int num, int puzzleSize) {
        // 临时设置值
        int oldValue = 0;
        for (Cell cell : cells) {
            if (cell.row == row && cell.col == col) {
                oldValue = cell.value;
                cell.value = num;
                break;
            }
        }
        
        // 检查约束
        boolean valid = checkPartialConstraintInternal(puzzleSize);
        
        // 恢复值
        for (Cell cell : cells) {
            if (cell.row == row && cell.col == col) {
                cell.value = oldValue;
                break;
            }
        }
        
        return valid;
    }
    
    private boolean checkPartialConstraintInternal(int puzzleSize) {
        if (operation == '#') {
            // 单个单元格
            return cells.get(0).value == target || cells.get(0).value == 0;
        }
        
        // 收集非空值
        List<Integer> values = new ArrayList<>();
        for (Cell cell : cells) {
            if (!cell.isEmpty()) {
                values.add(cell.value);
            }
        }
        
        // 如果所有单元格都有值，检查完整约束
        if (values.size() == cells.size()) {
            return checkConstraint();
        }
        
        // 部分填充，检查是否可能满足约束
        switch (operation) {
            case '+': {
                int sum = 0;
                for (int v : values) sum += v;
                // 剩余单元格最小值
                int remaining = cells.size() - values.size();
                int minPossible = sum + remaining * 1;
                int maxPossible = sum + remaining * puzzleSize;
                return target >= minPossible && target <= maxPossible;
            }
            case '-': {
                // 对于减法，如果已经有2个值以上，无法确定
                if (values.size() >= 2) {
                    int max = Math.max(values.get(0), values.get(1));
                    int min = Math.min(values.get(0), values.get(1));
                    return (max - min) == target;
                }
                return true;
            }
            case '*': {
                int product = 1;
                for (int v : values) product *= v;
                // 剩余单元格最小值
                int remaining = cells.size() - values.size();
                int minPossible = product * (int)Math.pow(1, remaining);
                int maxPossible = product * (int)Math.pow(puzzleSize, remaining);
                return target >= minPossible && target <= maxPossible;
            }
            case '/': {
                // 对于除法，如果已经有2个值以上，无法确定
                if (values.size() >= 2) {
                    int max = Math.max(values.get(0), values.get(1));
                    int min = Math.min(values.get(0), values.get(1));
                    return min != 0 && (max / min) == target && (max % min) == 0;
                }
                return true;
            }
            default:
                return true;
        }
    }
}
