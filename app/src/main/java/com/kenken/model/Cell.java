package com.kenken.model;

public class Cell {
    public int row;
    public int col;
    public int value;  // 0 means empty
    public boolean[] pencilValues;  // for pencil marks
    
    public Cell(int row, int col) {
        this.row = row;
        this.col = col;
        this.value = 0;
        this.pencilValues = new boolean[10];  // 1-9
    }
    
    public boolean isEmpty() {
        return value == 0;
    }
    
    public boolean hasPencilMarks() {
        for (int i = 1; i <= 9; i++) {
            if (pencilValues[i]) return true;
        }
        return false;
    }
    
    public void clear() {
        value = 0;
        for (int i = 0; i < 10; i++) {
            pencilValues[i] = false;
        }
    }
}
