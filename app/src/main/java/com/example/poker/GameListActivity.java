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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.auth.FirebaseUser;

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

        //Firebase штучки
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        gamesRef = db.collection("games");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String uid = currentUser.getUid();
            String email = currentUser.getEmail();
            Log.d("FIREBASE_USER", "Пользователь вошёл: UID = " + uid + ", Email = " + email);
        } else {
            Log.d("FIREBASE_USER", "Пользователь НЕ вошёл в систему");
        }

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
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> game = new HashMap<>();
        game.put("hostId", currentUser.getUid());

        List<String> playerIds = new ArrayList<>();
        playerIds.add(currentUser.getUid());
        game.put("playerIds", playerIds);

        game.put("maxPlayers", 5);
        game.put("status", "waiting");

        db.collection("games")
                .add(game)
                .addOnSuccessListener(documentReference -> {
                    Log.d("CreateGame", "Game created with ID: " + documentReference.getId());
                    Toast.makeText(this, "Game created", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(GameListActivity.this, GameActivity.class);//Переход в GameActivity
                    intent.putExtra("gameId", documentReference.getId()); // передаем ID игры
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Log.w("CreateGame", "Error adding game", e);
                    Toast.makeText(this, "Error creating game", Toast.LENGTH_SHORT).show();
                });
    }
}
