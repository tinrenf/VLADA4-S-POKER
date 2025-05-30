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
    TextView blindsInfo;

    public GameAdapter(List<Game> games, Context context) {
        this.games = games;
        this.context = context;
        this.currentUserUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @Override
    public GameViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.game_list_item, parent, false);
        return new GameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(GameViewHolder holder, int position) {
        Game game = games.get(position);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        holder.gameName.setText(game.getName());

        int count = game.getPlayerIds().size();
        holder.playerCount.setText("Players: " + count + "/" + game.getMaxPlayers());

        db.collection("games")
                .document(game.getId())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String hostName = doc.getString("creatorID");
                        Long bigBlind = doc.getLong("bigBlind");

                        db.collection("players")
                                .document(hostName)
                                .get()
                                .addOnSuccessListener(playerDoc -> {
                                    holder.hostName.setText("Host: " + playerDoc.getString("name"));
                                });

                        if (bigBlind != null) {
                            holder.blindsInfo.setText("Blinds: " + bigBlind + "/" + (bigBlind / 2));
                        } else {
                            holder.blindsInfo.setText("Blinds: unknown");
                        }
                    }
                });

        List<String> ids = game.getPlayerIds();
        if (ids.isEmpty()) {
            holder.playerNames.setText("Players: none");
        } else {
            List<String> names = new ArrayList<>();
            for (String uid : ids) {
                db.collection("players").document(uid).get()
                        .addOnSuccessListener(doc -> {
                            String name = doc.getString("name");
                            if (name != null) {
                                names.add(name);
                            }
                            if (names.size() == ids.size()) {
                                holder.playerNames.setText("Players: " + String.join(", ", names));
                            }
                        });
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (game.getPlayerIds().contains(currentUserUid)) {
                goToGameActivity(game.getId());
                return;
            }

            if (game.getPlayerIds().size() >= game.getMaxPlayers()) {
                Toast.makeText(context, "Игроков уже 5", Toast.LENGTH_SHORT).show();
                return;
            }

            DocumentReference gameRef = db.collection("games").document(game.getId());
            gameRef.get().addOnSuccessListener(doc -> {
                if (!doc.exists()) return;

                Map<String, Object> updates = new HashMap<>();
                updates.put("playerIds", FieldValue.arrayUnion(currentUserUid));

                String status = doc.getString("status");
                if ("started".equals(status)) {
                    updates.put("foldedPlayers", FieldValue.arrayUnion(currentUserUid));
                    updates.put("joinAfterStart", FieldValue.arrayUnion(currentUserUid));
                }

                gameRef.update(updates)
                        .addOnSuccessListener(aVoid -> goToGameActivity(game.getId()))
                        .addOnFailureListener(e ->
                                Toast.makeText(context, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
            });
        });
    }

    private void goToGameActivity(String gameId) {
        Intent intent = new Intent(context, GameActivity.class);
        intent.putExtra("gameId", gameId);
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return games.size();
    }
    static class GameViewHolder extends RecyclerView.ViewHolder {
        TextView gameName, hostName, playerCount, playerNames, blindsInfo;
        GameViewHolder(View itemView) {
            super(itemView);
            gameName = itemView.findViewById(R.id.game_name);
            hostName = itemView.findViewById(R.id.host_name);
            playerCount = itemView.findViewById(R.id.player_count);
            playerNames = itemView.findViewById(R.id.player_names);
            blindsInfo = itemView.findViewById(R.id.blinds_info);
        }
    }
}
