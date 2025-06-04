package com.example.poker;

import java.util.*;
import android.content.*;
import android.view.*;
import android.widget.*;

import android.os.Bundle;
import android.text.InputFilter;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.auth.FirebaseUser;


public class CreateGameActivity extends AppCompatActivity {

    private EditText gameNameEditText;
    private EditText bigBlindEditText;
    private Button createButton;
    private Button buttonIncrease;
    private Button buttonDecrease;
    private int bigBlind = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_game);

        gameNameEditText = findViewById(R.id.editTextGameName);
        bigBlindEditText = findViewById(R.id.editTextBigBlind);
        createButton = findViewById(R.id.buttonCreateGame);
        buttonIncrease = findViewById(R.id.buttonIncrease);
        buttonDecrease = findViewById(R.id.buttonDecrease);

        gameNameEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});

        bigBlindEditText.setText(String.valueOf(bigBlind));

        buttonIncrease.setOnClickListener(v -> {
            bigBlind += 50;
            bigBlindEditText.setText(String.valueOf(bigBlind));
        });

        buttonDecrease.setOnClickListener(v -> {
            if (bigBlind > 50) {
                bigBlind -= 50;
                bigBlindEditText.setText(String.valueOf(bigBlind));
            }
        });

        createButton.setOnClickListener(v -> {
            String gameName = gameNameEditText.getText().toString().trim();
            if (gameName.isEmpty()) {
                Toast.makeText(this, "Введите название игры", Toast.LENGTH_SHORT).show();
                return;
            }
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
                });
    }
}

