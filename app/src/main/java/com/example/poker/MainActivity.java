package com.example.poker;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseUser;

import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Button logoutButton = findViewById(R.id.logout_button);

        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            Toast.makeText(MainActivity.this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
        });

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        Button playButton = findViewById(R.id.play_button);

        db = FirebaseFirestore.getInstance();

        if (currentUser != null) {
            String uid = currentUser.getUid();
            db.collection("players").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        TextView playerName = findViewById(R.id.playerName);
                        TextView playerChips = findViewById(R.id.playerChips);
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            Long chips = documentSnapshot.getLong("money");

                            playerName.setText(name);
                            playerChips.setText("Chips: " + chips);
                        } else {
                            playerName.setText("Player not found");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
                    });
        }//Инфа о пользователе

        if (currentUser == null) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else {
            playButton.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, GameListActivity.class);
                startActivity(intent);
            });
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}