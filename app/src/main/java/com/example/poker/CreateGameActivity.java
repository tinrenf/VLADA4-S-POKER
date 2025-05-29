package com.example.poker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import android.widget.EditText;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;

import android.util.Log;
import java.util.*;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.auth.FirebaseUser;

public class CreateGameActivity extends AppCompatActivity {

    private EditText gameNameEditText;
    private EditText bigBlindEditText;
    private Button createButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_game);

        gameNameEditText = findViewById(R.id.editTextGameName);
        bigBlindEditText = findViewById(R.id.editTextBigBlind);
        createButton = findViewById(R.id.buttonCreateGame);

        createButton.setOnClickListener(v -> {
            String gameName = gameNameEditText.getText().toString().trim();
            String blindStr = bigBlindEditText.getText().toString().trim();

            if (gameName.isEmpty() || blindStr.isEmpty()) {
                Toast.makeText(this, "Введите название и большой блайнд", Toast.LENGTH_SHORT).show();
                return;
            }

            int bigBlind = Integer.parseInt(blindStr);

            createGameInFirestore(gameName, bigBlind);
        });
    }

    private void createGameInFirestore(String gameName, int bigBlind) {
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
        game.put("name", gameName);
        game.put("bigBlind", bigBlind);

        db.collection("games")
                .add(game)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Игра создана", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(CreateGameActivity.this, GameActivity.class);
                    intent.putExtra("gameId", documentReference.getId());
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка при создании игры", Toast.LENGTH_SHORT).show();
                    Log.e("CreateGameActivity", "Ошибка: ", e);
                });
    }
}
