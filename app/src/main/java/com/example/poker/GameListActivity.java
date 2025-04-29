package com.example.poker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class GameListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_list);

        Button createGameButton = findViewById(R.id.create_game_button);
        createGameButton.setOnClickListener(view -> {
            Intent intent = new Intent(GameListActivity.this, GameActivity.class);
            startActivity(intent);
        });
    }
}
