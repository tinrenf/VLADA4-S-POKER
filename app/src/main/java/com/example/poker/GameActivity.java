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
    private TextView[] holeCardViews;
    private TextView[] playerViews;
    private List<String> playerIds = new ArrayList<>();
    private Button startButton;
    private List<String> playerNames = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        playerInfo = findViewById(R.id.player_info);
        playerViews = new TextView[]{findViewById(R.id.player5),
                findViewById(R.id.player1_name),
                findViewById(R.id.player2_name),
                findViewById(R.id.player3_name),
                findViewById(R.id.player4_name)
        };

        holeCardViews = new TextView[]{
                findViewById(R.id.cards_player5),
                findViewById(R.id.cards_player1),
                findViewById(R.id.cards_player2),
                findViewById(R.id.cards_player3),
                findViewById(R.id.cards_player4)
        };

        db = FirebaseFirestore.getInstance();

        startButton = findViewById(R.id.start_button);
        Button callButton = findViewById(R.id.button_call);
        Button raiseButton = findViewById(R.id.button_raise);
        Button foldButton = findViewById(R.id.button_fold); /**КНОПКИ**/

        String gameId = getIntent().getStringExtra("gameId");
        loadGameDetails(gameId);

        startButton.setOnClickListener(v -> {
            Map<String, List<Card>> holeCards = PreFlop.deal(playerIds);
            for (int i = 0; i < playerIds.size() && i < holeCardViews.length; i++) {
                List<Card> hand = holeCards.get(playerIds.get(i));
                holeCardViews[i].setText(hand.get(0).toString() + "  " + hand.get(1).toString());
            }
            startButton.setVisibility(View.GONE);
        });//ПРЕ-ФЛОП
    }

    @Override
    protected void onDestroy() {
        leaveGame();
        super.onDestroy();
    }

    protected void onStop() {
        super.onStop();
        Intent intent = new Intent(GameActivity.this, GameListActivity.class);
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

            playerIds = (List<String>) snapshot.get("playerIds");
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
                .addSnapshotListener((docSnapshot, error) -> {
                    if (error != null || docSnapshot == null || !docSnapshot.exists()) {
                        Log.w(TAG, "Listen failed or document missing", error);
                        return;
                    }

                    Game game = docSnapshot.toObject(Game.class);
                    String creatorID = docSnapshot.getString("creatorID");
                    String currentUID = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    if (creatorID != null && creatorID.equals(currentUID)) {
                        startButton.setVisibility(View.VISIBLE);
                    } else {
                        startButton.setVisibility(View.GONE);
                    }

                    // отобразить имена
                    if (game != null && game.getPlayerIds() != null) {
                        playerIds.clear();
                        playerIds.addAll(game.getPlayerIds());
                        displayPlayerNames();
                    }
                });
    }

    private void displayPlayerNames() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        playerNames.clear();

        for (int i = 0; i < playerViews.length; ++i) {
            final int index = i;
            TextView tv = playerViews[i];

            if (i < playerIds.size()) {
                String uid = playerIds.get(i);
                db.collection("players").document(uid).get()
                        .addOnSuccessListener(doc -> {
                            String name = doc.exists() ? doc.getString("name") : "unknown";
                            while (playerNames.size() <= index) playerNames.add("");
                            playerNames.set(index, name);
                            tv.setText(name);
                        })
                        .addOnFailureListener(e -> tv.setText("(error)"));
            } else {
                tv.setText("xxx");
            }
        }
    }
}
