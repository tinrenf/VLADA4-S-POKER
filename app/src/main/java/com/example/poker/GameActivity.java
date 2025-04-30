package com.example.poker;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import android.util.Log;
import java.util.*;

public class GameActivity extends AppCompatActivity {
    private TextView playerInfo;
    private TextView player1, player2, player3, player4, player5;
    private static final String TAG = "GameActivity";
    private FirebaseFirestore db;
    private int playerChips = 1000; // Стартовые фишки
    int big_blind = 100;
    int small_blind = big_blind / 2;
    int cur_rate = big_blind;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        playerInfo = findViewById(R.id.player_info);
        player1 = findViewById(R.id.player1);
        player2 = findViewById(R.id.player2);
        player3 = findViewById(R.id.player3);
        player4 = findViewById(R.id.player4);
        player5 = findViewById(R.id.player5);

        db = FirebaseFirestore.getInstance();

        String gameId = getIntent().getStringExtra("gameId");
        if (gameId != null) {
            // Загружаем данные о игре
            loadGameDetails(gameId);
        }

        Button callButton = findViewById(R.id.button_call);
        Button raiseButton = findViewById(R.id.button_raise);
        Button foldButton = findViewById(R.id.button_fold);

        //CALL
        callButton.setOnClickListener(v -> {
            if (cur_rate <= playerChips) {
                playerChips -= cur_rate;
                updatePlayerInfo();
            } else {
                playerInfo.setText("Not enough chips");
            }
        });

        //RAISE
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

        //Установка блайндов
        postBigBlind();
    }

    private void postBigBlind() {
        playerChips -= big_blind;
        cur_rate = big_blind;
        updatePlayerInfo();
    }

    private void postSmallBlind() {
        playerChips -= small_blind;
    }

    private void updatePlayerInfo() {
        playerInfo.setText("Your chips: " + playerChips);
    }

    private void loadGameDetails(String gameId) {
        db.collection("games").document(gameId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Game game = documentSnapshot.toObject(Game.class);
                        Log.d(TAG, "Game details: " + game);

                        //Список игроков
                        if (game != null && game.getPlayerIds() != null) {
                            List<String> playerIds = game.getPlayerIds();
                            displayPlayerIds(playerIds);
                        }
                    } else {
                        Log.w(TAG, "Game not found");
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error getting game details", e));
    }

    private void displayPlayerIds(List<String> playerIds) {
        TextView[] playerViews = {player5, player1, player2, player3, player4};

        for (int i = 1; i <= playerIds.size() && i <= playerViews.length; i++) {
            playerViews[i - 1].setText("Player " + i + ": " + playerIds.get(i - 1));
        }
    }
}
