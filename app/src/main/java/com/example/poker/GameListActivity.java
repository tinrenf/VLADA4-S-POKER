package com.example.poker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.*;

public class GameListActivity extends AppCompatActivity {

    private static final String TAG = "GameListActivity";

    private FirebaseFirestore db;
    private CollectionReference gamesRef;
    private FirebaseAuth auth;

    private RecyclerView recyclerView;
    private GameAdapter adapter;
    private List<Game> gameList;

    private Button createGameButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_list);

        // Инициализация Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        gamesRef = db.collection("games");

        // UI элементы
        recyclerView = findViewById(R.id.game_list_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        gameList = new ArrayList<>();
        adapter = new GameAdapter(gameList);
        recyclerView.setAdapter(adapter);

        createGameButton = findViewById(R.id.create_game_button);
        createGameButton.setOnClickListener(v -> createNewGame());

        listenForGames();
    }

    private void listenForGames() {
        gamesRef.orderBy("timestamp", Query.Direction.DESCENDING).addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            Toast.makeText(GameListActivity.this,
                                    "Error loading games: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        gameList.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Game game = doc.toObject(Game.class);
                            if (game != null) {
                                game.setId(doc.getId());
                                gameList.add(game);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void createNewGame() {
        String userId = auth.getCurrentUser().getUid();
        Map<String,Object> gameData = new HashMap<>();
        gameData.put("creatorId", userId);
        gameData.put("timestamp", FieldValue.serverTimestamp());
        //можно добавлять другие параметры

        gamesRef
                .add(gameData)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this,
                            "Game created",
                            Toast.LENGTH_SHORT).show();
                    // опционально: сразу открыть эту игру
                    Intent intent = new Intent(GameListActivity.this, GameActivity.class);
                    intent.putExtra("gameId", docRef.getId());
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating game", e);
                    Toast.makeText(this,
                            "Failed to create game: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}
