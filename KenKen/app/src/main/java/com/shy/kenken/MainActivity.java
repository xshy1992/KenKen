package com.shy.kenken;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        MaterialButton easyBtn = (MaterialButton) findViewById(R.id.btn_easy);
        MaterialButton mediumBtn = (MaterialButton) findViewById(R.id.btn_medium);
        MaterialButton hardBtn = (MaterialButton) findViewById(R.id.btn_hard);
        MaterialButton veryHardBtn = (MaterialButton) findViewById(R.id.btn_very_hard);
        MaterialButton expertBtn = (MaterialButton) findViewById(R.id.btn_expert);
        MaterialButton masterBtn = (MaterialButton) findViewById(R.id.btn_master);
        MaterialButton rulesBtn = (MaterialButton) findViewById(R.id.btn_rules);
        
        easyBtn.setOnClickListener(v -> startGame(4));
        mediumBtn.setOnClickListener(v -> startGame(5));
        hardBtn.setOnClickListener(v -> startGame(6));
        veryHardBtn.setOnClickListener(v -> startGame(7));
        expertBtn.setOnClickListener(v -> startGame(8));
        masterBtn.setOnClickListener(v -> startGame(9));
        
        if (rulesBtn != null) {
            rulesBtn.setOnClickListener(v -> showRules());
        }
    }
    
    private void startGame(int size) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("size", size);
        startActivity(intent);
    }
    
    private void showRules() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = getLayoutInflater();
            builder.setView(inflater.inflate(R.layout.dialog_rules, null))
                   .setPositiveButton("知道了", null)
                   .show();
        } catch (Exception e) {
            // Fallback if dialog fails
            new AlertDialog.Builder(this)
                .setTitle("游戏规则")
                .setMessage("在网格中填入数字，使每行每列不重复。\n每个粗线框内的数字需满足标注的运算。")
                .setPositiveButton("知道了", null)
                .show();
        }
    }
}
