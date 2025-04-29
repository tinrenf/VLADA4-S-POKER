package com.example.poker;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class GameActivity extends AppCompatActivity {

    private TextView playerInfo;
    private int playerChips = 1000; // Стартовые фишки
    int blind = 100;
    int cur_rate = blind;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        playerInfo = findViewById(R.id.player_info);

        Button callButton = findViewById(R.id.button_call);
        Button raiseButton = findViewById(R.id.button_raise);
        Button foldButton = findViewById(R.id.button_fold);

        callButton.setOnClickListener(v -> {
            if (cur_rate <= playerChips) {
                playerChips -= cur_rate;
                updatePlayerInfo();
            } else {
                playerInfo.setText("Not enough chips");
            }
        });

        raiseButton.setOnClickListener(v -> {
            if (playerChips >= cur_rate * 2) {
                cur_rate *= 2;
                playerChips -= cur_rate;
                updatePlayerInfo();
            } else {
                playerInfo.setText("Not enough chips");
            }
        });

        foldButton.setOnClickListener(v -> {
            // Пока просто текст
            playerInfo.setText("You folded!");
        });

        // Установить большой блайнд при начале игры
        postBigBlind();
    }

    private void postBigBlind() {
        playerChips -= blind;
        cur_rate = blind;
        updatePlayerInfo();
    }

    private void updatePlayerInfo() {
        playerInfo.setText("Your chips: " + playerChips);
    }
}
