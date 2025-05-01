package com.example.poker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.*;

public class RegisterActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private EditText name, email, password;
    private Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        name = findViewById(R.id.name);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> {
            String inName = name.getText().toString().trim();
            String inEmail = email.getText().toString().trim();
            String inPassword = password.getText().toString().trim();
            if (inName.isEmpty() || inEmail.isEmpty() || inPassword.length() < 6) {
                Toast.makeText(this, "Enter your name, email and password >=6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(inEmail, inPassword).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        String uid = user.getUid();
                        String emailRegistered = user.getEmail();
                        Log.d("REGISTER", "User registered. UID = " + uid + ", Email = " + emailRegistered);

                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("email", emailRegistered);
                        userData.put("password", password);
                        db.collection("users").document(uid).set(userData);
                    }

                    Toast.makeText(this, "Successful registration", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Player pl = new Player(mAuth.getCurrentUser().getUid(), inName);
            db.collection("players").add(pl).addOnSuccessListener(documentReference -> {
                Log.d("CreatePlayer", "Player created with ID: " + documentReference.getId());
            }).addOnFailureListener(e -> {
                Log.w("CreatePlayer", "Error adding player", e);
                Toast.makeText(this, "Error creating player", Toast.LENGTH_SHORT).show();
            });
        });
    }
}
