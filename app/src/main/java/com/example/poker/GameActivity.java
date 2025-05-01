package com.example.poker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import android.view.View;

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

        Button startButton = findViewById(R.id.start_button);

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

        //FOLD
        foldButton.setOnClickListener(v -> {
            // Пока просто текст
            playerInfo.setText("You folded!");
        });

        //Установка блайндов
        postBigBlind();
    }

    @Override
    protected void onDestroy() {
        leaveGame();
        super.onDestroy();
    }

    protected void onStop() {
        super.onStop();
        Intent intent = new Intent(GameActivity.this, MainActivity.class);
        leaveGame();
        startActivity(intent);
    }//Кажется OnStop работает даже в случае когда приложение сворачивается. Пока так нужно, чтобы не было дохуя игр

    private void leaveGame() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        String gameId = getIntent().getStringExtra("gameId");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference gameRef = db.collection("games").document(gameId);

        gameRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) return;

            List<String> playerIds = (List<String>) snapshot.get("playerIds");
            String creatorID = snapshot.getString("creatorID");

            if (playerIds == null) playerIds = new ArrayList<>();
            playerIds.remove(uid); // Удаляем текущего игрока

            if (playerIds.isEmpty()) {
                // Нет игроков — удаляем игру
                gameRef.delete().addOnSuccessListener(aVoid ->
                        Log.d("GameExit", "Игра удалена, игроков не осталось"));
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("playerIds", playerIds);

            if (uid.equals(creatorID)) {
                // Передаём хост права первому оставшемуся игроку
                String newcreatorID = playerIds.get(0);
                updates.put("creatorID", newcreatorID);
                Log.d("GameExit", "Хост передан игроку: " + newcreatorID);
            }

            gameRef.update(updates)
                    .addOnSuccessListener(aVoid ->
                            Log.d("GameExit", "Игрок вышел, обновлены playerIds и creatorID"))
                    .addOnFailureListener(e ->
                            Log.e("GameExit", "Ошибка обновления: " + e.getMessage()));
        });
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

                        String creatorID = documentSnapshot.getString("creatorID");
                        Button startButton = findViewById(R.id.start_button);
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                        if (creatorID != null && user != null && creatorID.equals(user.getUid())) {
                            startButton.setVisibility(View.VISIBLE);
                        } else {
                            startButton.setVisibility(View.GONE);
                        }

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

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        for (int i = 0; i < playerViews.length; ++i) {
            final int index = i; //иначе не работает
            TextView tv = playerViews[i];
            if (i < playerIds.size()) {
                String uid = playerIds.get(i);
                db.collection("players").document(uid).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                String name = doc.getString("name");
                                tv.setText("Player " + index + ": " + name);
                            } else {
                                tv.setText("Player " + index + ": (no profile)");
                            }
                        })
                        .addOnFailureListener(e -> {
                            tv.setText("Player " + index + ": error");
                        });
            } else {
                tv.setText("xxx");
            }
        }
    }
}
