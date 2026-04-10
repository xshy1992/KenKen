package com.kenken;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        MaterialButton easyBtn = findViewById(R.id.btn_easy);
        MaterialButton mediumBtn = findViewById(R.id.btn_medium);
        MaterialButton hardBtn = findViewById(R.id.btn_hard);
        MaterialButton veryHardBtn = findViewById(R.id.btn_very_hard);
        MaterialButton expertBtn = findViewById(R.id.btn_expert);
        MaterialButton masterBtn = findViewById(R.id.btn_master);
        
        easyBtn.setOnClickListener(v -> startGame(4));
        mediumBtn.setOnClickListener(v -> startGame(5));
        hardBtn.setOnClickListener(v -> startGame(6));
        veryHardBtn.setOnClickListener(v -> startGame(7));
        expertBtn.setOnClickListener(v -> startGame(8));
        masterBtn.setOnClickListener(v -> startGame(9));
    }
    
    private void startGame(int size) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("size", size);
        startActivity(intent);
    }
}
