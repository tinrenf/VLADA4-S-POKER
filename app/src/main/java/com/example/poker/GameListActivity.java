package com.example.poker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;

import java.util.*;

public class GameListActivity extends AppCompatActivity {

    private static final String TAG = "GameListActivity";

    private FirebaseFirestore db;
    private CollectionReference gamesRef;

    private RecyclerView recyclerView;
    private GameAdapter adapter;
    private List<Game> gameList;

    private Button createGameButton, BackButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_list);

        //Firebase штучки
        db = FirebaseFirestore.getInstance();
        gamesRef = db.collection("games");

        // UI элементы
        recyclerView = findViewById(R.id.game_list_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        gameList = new ArrayList<>();
        adapter = new GameAdapter(gameList, this);
        recyclerView.setAdapter(adapter);

        createGameButton = findViewById(R.id.create_game_button);
        createGameButton.setOnClickListener(v -> {
            Intent intent = new Intent(GameListActivity.this, CreateGameActivity.class);
            startActivity(intent);
        });

        BackButton = findViewById(R.id.BackFLbutton);
        BackButton.setOnClickListener(v -> {
            Intent intent = new Intent(GameListActivity.this, MainActivity.class);
            startActivity(intent);
        });

        listenForGames();
    }

    private void listenForGames() {
        gamesRef.orderBy("timestamp", Query.Direction.DESCENDING).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot s, FirebaseFirestoreException e) {
                gameList.clear();

                for (DocumentSnapshot doc : s.getDocuments()) {
                    Game game = doc.toObject(Game.class);
                    if (game != null && doc.contains("timestamp")) {
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

        List<String> playerIds = new ArrayList<>();
        playerIds.add(currentUser.getUid());
        game.put("creatorID", currentUser.getUid());
        game.put("playerIds", playerIds);

        game.put("maxPlayers", 5);
        game.put("status", "waiting");
        game.put("timestamp", FieldValue.serverTimestamp());

        db.collection("games")
                .add(game)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Game created", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(GameListActivity.this, GameActivity.class);
                    intent.putExtra("gameId", documentReference.getId());
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error creating game", Toast.LENGTH_SHORT).show();
                });
    }
}
