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

import java.util.List;

public class GameAdapter extends RecyclerView.Adapter<GameAdapter.GameViewHolder> {

    private final List<Game> games;
    private final Context context;
    private final String currentUserUid;

    public GameAdapter(List<Game> games) {
        this.games = games;
        this.context = null;  // fix in onCreateViewHolder
        this.currentUserUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @NonNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.game_list_item, parent, false);
        return new GameViewHolder(view);
    }//Не ебу что делает

    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        Game game = games.get(position);
        holder.gameInfo.setText("Game: " + game.getId() + "\nHost: " + game.getCreatorId());

        holder.joinButton.setOnClickListener(v -> {
            if (game.getPlayerIds().contains(currentUserUid)) {
                Toast.makeText(v.getContext(), "Ты уже играешь долбаеб, крашнуть не получится. Иди лучше пососи", Toast.LENGTH_SHORT).show();
                return;
            }

            if (game.getPlayerIds().size() >= game.getMaxPlayers()) {
                Toast.makeText(v.getContext(), "I fucked your mum", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseFirestore.getInstance()
                    .collection("games")
                    .document(game.getId())
                    .update("playerIds", FieldValue.arrayUnion(currentUserUid))
                    .addOnSuccessListener(unused -> {
                        Intent intent = new Intent(v.getContext(), GameActivity.class);
                        intent.putExtra("gameId", game.getId());
                        v.getContext().startActivity(intent);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(v.getContext(), "Ошибка подключения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
