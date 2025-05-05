package com.example.poker;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

import java.util.*;

public class GameAdapter extends RecyclerView.Adapter<GameAdapter.GameViewHolder> {

    private final List<Game> games;
    private final Context context;
    private final String currentUserUid;

    public GameAdapter(List<Game> games) {
        this.games = games;
        this.context = null;
        this.currentUserUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @NonNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.game_list_item, parent, false);
        return new GameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        Game game = games.get(position);
        String creatorID = game.getcreatorID();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("players")
                .document(creatorID)
                .get()
                .addOnSuccessListener(doc -> {
                    holder.gameInfo.setText("Host: " + doc.getString("name"));
                });
        holder.joinButton.setOnClickListener(v -> {
            String uid = currentUserUid;
            DocumentReference gameRef = FirebaseFirestore.getInstance()
                    .collection("games")
                    .document(game.getId());

            if (game.getPlayerIds().contains(currentUserUid)) {
                Toast.makeText(v.getContext(), "In game", Toast.LENGTH_SHORT).show();
                return;
            }

            if (game.getPlayerIds().size() >= game.getMaxPlayers()) {
                Toast.makeText(v.getContext(), "max players", Toast.LENGTH_SHORT).show();
                return;
            }

            gameRef.get().addOnSuccessListener(doc -> {
                if (!doc.exists()) return;

                Map<String,Object> updates = new HashMap<>();
                updates.put("playerIds", FieldValue.arrayUnion(uid));

                String status = doc.getString("status");
                if ("started".equals(status)) {
                    updates.put("foldedPlayers", FieldValue.arrayUnion(uid));
                    updates.put("joinAfterStart", FieldValue.arrayUnion(uid));
                }

                gameRef.update(updates)
                        .addOnSuccessListener(aVoid -> {
                            Intent intent = new Intent(v.getContext(), GameActivity.class);
                            intent.putExtra("gameId", game.getId());
                            v.getContext().startActivity(intent);
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(v.getContext(), "You are a mistake: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
            });
        });
    }

    @Override
    public int getItemCount() {
        return games.size();
    }

    static class GameViewHolder extends RecyclerView.ViewHolder {
        TextView gameInfo;
        Button joinButton;

        GameViewHolder(@NonNull View itemView) {
            super(itemView);
            gameInfo = itemView.findViewById(R.id.game_info);
            joinButton = itemView.findViewById(R.id.btn_join);
        }
    }
}
