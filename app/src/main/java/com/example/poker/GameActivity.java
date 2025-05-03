package com.example.poker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;

import android.view.View;
import android.graphics.Color;
import android.graphics.Typeface;

import android.util.Log;
import java.util.*;

public class GameActivity extends AppCompatActivity {
    private TextView playerInfo;
    private TextView player1, player2, player3, player4, player5;
    private static final String TAG = "GameActivity";
    private FirebaseFirestore db;
    int big_blind = 100;
    int small_blind = big_blind / 2;
    int cur_rate = big_blind;
    private TextView[] holeCardViews;
    private TextView[] playerViews;
    private TextView[] betViews;
    private List<String> playerIds = new ArrayList<>();
    private Button startButton, nextRound;
    private List<String> playerNames = new ArrayList<>();
    private String gameId;

    private int pot = 0;
    private int currentBet = big_blind;
    private Map<String, Integer> playerBets = new HashMap<>();
    private Set<String> foldedPlayers = new HashSet<>();
    private boolean hasActed = false;
    private String currentUID;
    private TextView potView;

    private TextView[] commViews;
    private List<String> deck;
    private int round = 0;
    private String creatorID;
    private String currentPlayerID;
    private int lastRaise = -1;

    private TextView[] chipViews;
    private List<String> playersRaisedThisRound = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toast.makeText(this, "GameActivity onCreate", Toast.LENGTH_SHORT).show();
        Log.d("DEBUG", "GameActivity onCreate, gameId=" + getIntent().getStringExtra("gameId"));
        setContentView(R.layout.activity_game);

        gameId = getIntent().getStringExtra("gameId");
        currentUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        potView = findViewById(R.id.pot_text);

        playerInfo = findViewById(R.id.player_info);
        playerViews = new TextView[]{findViewById(R.id.player5),
                findViewById(R.id.player1_name),
                findViewById(R.id.player2_name),
                findViewById(R.id.player3_name),
                findViewById(R.id.player4_name)
        };

        holeCardViews = new TextView[]{
                findViewById(R.id.cards_player5),
                findViewById(R.id.cards_player1),
                findViewById(R.id.cards_player2),
                findViewById(R.id.cards_player3),
                findViewById(R.id.cards_player4)
        };

        betViews = new TextView[]{
                findViewById(R.id.bet_player5),
                findViewById(R.id.bet_player1),
                findViewById(R.id.bet_player2),
                findViewById(R.id.bet_player3),
                findViewById(R.id.bet_player4)
        };

        commViews = new TextView[]{
                findViewById(R.id.card_comm1),
                findViewById(R.id.card_comm2),
                findViewById(R.id.card_comm3),
                findViewById(R.id.card_comm4),
                findViewById(R.id.card_comm5)
        };

        chipViews = new TextView[]{
                findViewById(R.id.player5_chips),
                findViewById(R.id.player1_chips),
                findViewById(R.id.player2_chips),
                findViewById(R.id.player3_chips),
                findViewById(R.id.player4_chips)
        };

        db = FirebaseFirestore.getInstance();

        startButton = findViewById(R.id.start_button);
        Button callButton = findViewById(R.id.button_call);
        Button raiseButton = findViewById(R.id.button_raise);
        Button foldButton = findViewById(R.id.button_fold);
        nextRound = findViewById(R.id.next_round);/**КНОПКИ**/

        loadGameDetails(gameId);

        nextRound.setOnClickListener(v -> {
            if(currentUID.equals(creatorID)){
                int newRound = round + 1;
                if(newRound <= 3){
                    db.collection("games").document(gameId)
                            .update("round", newRound);
                }
            }
        });

        startButton.setOnClickListener(v -> {
            db.collection("games").document(gameId).get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.contains("gameStarted") && Boolean.TRUE.equals(snapshot.get("gameStarted"))) {
                            Log.d("Game", "Игра уже началась");
                            return;
                        }

                        if (playerIds.size() >= 2) {
                            String smallBlindPlayer = playerIds.get(0);
                            String bigBlindPlayer = playerIds.get(1);

                            DocumentReference gameRef = db.collection("games").document(gameId);

                            currentBet = big_blind;

                            gameRef.get().addOnSuccessListener(docSnapshot -> {
                                if (docSnapshot.exists()) {
                                    List<String> playerIds = (List<String>) docSnapshot.get("playerIds");
                                    if (playerIds != null && !playerIds.isEmpty()) {
                                        if (playerIds.size() == 2) {
                                            currentPlayerID = playerIds.get(0);
                                        } else if (playerIds.size() > 2) {
                                            currentPlayerID = playerIds.get(2);
                                        }
                                    }

                                    gameRef.update("currentPlayerID", currentPlayerID);
                                    gameRef.update("currentBet", currentBet);
                                    gameRef.update("pot", small_blind + big_blind);
                                }
                            });

                            Map<String, Object> updates = new HashMap<>();
                            Map<String, Integer> playerBets = new HashMap<>();

                            playerBets.put(smallBlindPlayer, small_blind);
                            playerBets.put(bigBlindPlayer, big_blind);

                            updates.put("playerBets", playerBets);
                            updates.put("pot", small_blind + big_blind);
                            updates.put("stage", "preflop");

                            db.collection("games").document(gameId).update(updates);
                        }

                        Map<String, List<Card>> holeCards = PreFlop.deal(playerIds);

                        // Показываем карты у хоста
                        for (int i = 0; i < playerIds.size() && i < holeCardViews.length; i++) {
                            List<Card> hand = holeCards.get(playerIds.get(i));
                            holeCardViews[i].setText(hand.get(0).toString() + "  " + hand.get(1).toString());
                        }

                        // Сохраняем карты в Firestore
                        Map<String, Object> holeCardStrings = new HashMap<>();
                        for (Map.Entry<String, List<Card>> entry : holeCards.entrySet()) {
                            String uid = entry.getKey();
                            List<Card> hand = entry.getValue();
                            List<String> stringHand = Arrays.asList(hand.get(0).toString(), hand.get(1).toString());
                            holeCardStrings.put(uid, stringHand);
                        }

                        deck = new ArrayList<>();
                        int[] suits = {1,2,3,4};
                        for(int s: suits){
                            for(int r=2; r<=14; r++){
                                Card curCard = new Card(s, r);
                                deck.add(curCard.toString());
                            }
                        }
                        Collections.shuffle(deck);

                        for(List<Card> hand : holeCards.values()){
                            deck.remove(hand.get(0).toString());
                            deck.remove(hand.get(1).toString());
                        }

                        Map<String, Object> chips = new HashMap<>();
                        for (String uid : playerIds) {
                            chips.put(uid, 1000);
                        }

                        lastRaise = 1;

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("deck", deck);
                        updates.put("holeCards", holeCardStrings);
                        updates.put("gameStarted", true);
                        updates.put("currentBet", currentBet);
                        updates.put("chips", chips);
                        updates.put("lastRaise", lastRaise);

                        db.collection("games").document(gameId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> Log.d("Game", "Игра началась"))
                                .addOnFailureListener(e -> Log.w("Game", "Ошибка старта игры", e));

                        startButton.setVisibility(View.GONE);
                    });
        });//Когда нажали на кнопку старт

        raiseButton.setOnClickListener(v -> {
            if (!currentUID.equals(currentPlayerID) || playersRaisedThisRound.contains(currentUID)) return;

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference gameRef = db.collection("games").document(gameId);

            int raiseAmount = cur_rate * 2;

            gameRef.get().addOnSuccessListener(docSnapshot -> {
                if (docSnapshot.exists()) {
                    Map<String, Long> chipsMap = (Map<String, Long>) docSnapshot.get("chips");
                    long currentChips = chipsMap.get(currentPlayerID);

                    if (currentChips - raiseAmount >= 0) {
                        chipsMap.put(currentPlayerID, currentChips - raiseAmount);
                        int prevBet = playerBets.getOrDefault(currentUID, 0);
                        playerBets = (Map<String, Integer>) docSnapshot.get("playerBets");
                        playerBets.put(currentUID, prevBet + raiseAmount);
                        pot += raiseAmount;
                        cur_rate = prevBet + raiseAmount;

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("chips", chipsMap);
                        updates.put("playerBets", playerBets);
                        updates.put("pot", pot);
                        updates.put("currentBet", cur_rate);
                        updates.put("lastRaise", playerIds.indexOf(currentUID));
                        updates.put("playersRaisedThisRound", playersRaisedThisRound);
                        gameRef.update(updates);

                        updatePotView();
                        proceedToNextPlayer();
                    } else {
                        Toast.makeText(this, "Не хватает фишек для Raise", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });

        callButton.setOnClickListener(v -> {
            if (!currentUID.equals(currentPlayerID)) return;

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference gameRef = db.collection("games").document(gameId);

            int prevBet = playerBets.getOrDefault(currentUID, 0);
            int toCall = cur_rate - prevBet;

            if (toCall <= 0) {
                proceedToNextPlayer();
                return;
            }

            gameRef.get().addOnSuccessListener(docSnapshot -> {
                if (docSnapshot.exists()) {
                    Map<String, Long> chipsMap = (Map<String, Long>) docSnapshot.get("chips");
                    long currentChips = chipsMap.get(currentPlayerID);

                    if (currentChips - toCall >= 0) {
                        chipsMap.put(currentPlayerID, currentChips - toCall);
                        playerBets = (Map<String, Integer>) docSnapshot.get("playerBets");
                        playerBets.put(currentUID, cur_rate);
                        pot += toCall;
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("chips", chipsMap);
                        updates.put("playerBets", playerBets);
                        updates.put("pot", pot);

                        gameRef.update(updates);
                        updatePotView();
                        proceedToNextPlayer();
                    } else {
                        Toast.makeText(this, "Не хватает фишек для Call", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });

        foldButton.setOnClickListener(v -> {
            if (!currentUID.equals(currentPlayerID)) return;

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference gameRef = db.collection("games").document(gameId);

            gameRef.get().addOnSuccessListener(docSnapshot -> {
                if (docSnapshot.exists()) {
                    Map<String, Boolean> foldedMap = (Map<String, Boolean>) docSnapshot.get("foldedPlayers");
                    if (foldedMap == null)
                        foldedMap = new HashMap<>();
                    foldedMap.put(currentUID, true);
                    playerBets.put(currentUID, 0);
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("foldedPlayers", foldedMap);
                    updates.put("playerBets", playerBets);

                    gameRef.update(updates);
                    holeCardViews[0].setText("Folded");

                    proceedToNextPlayer();
                }
            });
        });
    }

    private void updateStage(String stage) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference gameRef = db.collection("games").document(gameId);

        gameRef.get().addOnSuccessListener(docSnapshot -> {
            if (!docSnapshot.exists()) return;
            List<String> communityCards = (List<String>) docSnapshot.get("communityCards");
            if (communityCards == null) return;

            for (int i = 0; i < commViews.length; i++) {
                if (i < communityCards.size()) {
                    commViews[i].setText(communityCards.get(i));
                } else {
                    commViews[i].setText(" ");
                }
            }
        });
    }
    private void proceedToNextStage() {
        playersRaisedThisRound.clear();
        db.collection("games").document(gameId).update("playersRaisedThisRound", playersRaisedThisRound);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference gameRef = db.collection("games").document(gameId);

        gameRef.get().addOnSuccessListener(docSnapshot -> {
            if (!docSnapshot.exists()) return;

            String stage = docSnapshot.getString("stage");
            List<String> deck = (List<String>) docSnapshot.get("deck");

            if (deck == null || deck.size() < 5) {
                gameRef.update("stage", "VVV");
                return;
            }

            Map<String, Object> updates = new HashMap<>();

            playerBets.clear();
            updates.put("playerBets", playerBets);

            if ("preflop".equals(stage)) {
                updates.put("stage", "flop");
                updates.put("communityCards", deck.subList(0, 3));
            } else if ("flop".equals(stage)) {
                updates.put("stage", "turn");
                updates.put("communityCards", deck.subList(0, 4));
            } else if ("turn".equals(stage)) {
                updates.put("stage", "river");
                updates.put("communityCards", deck.subList(0, 5));
            } else {
                // Конец игры — по желанию можно добавить showDown
                return;
            }

            gameRef.update(updates);
        });
    }
    private void proceedToNextPlayer() {
        DocumentReference gameRef = db.collection("games").document(gameId);
        int currentIndex = playerIds.indexOf(currentPlayerID);
        int nextIndex = (currentIndex + 1) % playerIds.size();

        gameRef.get().addOnSuccessListener(docSnapshot -> {
            //lastRaise = (int)docSnapshot.get("lastRaise");
            if (nextIndex == lastRaise) {
                //gameRef.update("stage", "DDD");
                //return;
                proceedToNextStage();
                //return;
            }
            if (docSnapshot.exists()) {
                List<String> playerIds = (List<String>) docSnapshot.get("playerIds");

                String nextPlayerUid = playerIds.get(nextIndex);
                gameRef.update("currentPlayerID", nextPlayerUid);
            }
        });
    }

    private void updatePotView() {
        potView.setText("Pot: " + pot);
    }

    private void loadGameDetails(String gameId) {
        DocumentReference gameRef = db.collection("games").document(gameId);

        gameRef.addSnapshotListener((docSnapshot, e) -> {
            if (e != null || docSnapshot == null || !docSnapshot.exists()) {
                return;
            }

            Game game = docSnapshot.toObject(Game.class);
            creatorID = docSnapshot.getString("creatorID");
            String currentUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String currentTurnUid = docSnapshot.getString("currentPlayerID");

            currentPlayerID = currentTurnUid;

            Boolean gameStarted = docSnapshot.getBoolean("gameStarted");
            if (gameStarted != null && gameStarted) {
                startButton.setVisibility(View.GONE);
            }

            if (creatorID != null && creatorID.equals(currentUID) && (gameStarted == null || !gameStarted)) {
                startButton.setEnabled(!playerIds.isEmpty());
                startButton.setVisibility(View.VISIBLE);
            } else {
                startButton.setVisibility(View.GONE);
            }

            if (game != null && game.getPlayerIds() != null) {
                playerIds.clear();
                playerIds.addAll(game.getPlayerIds());
                startButton.setEnabled(playerIds.size() > 1);
                displayPlayerNames();

                if (creatorID != null && creatorID.equals(currentUID) && (gameStarted == null || !gameStarted)) {
                    startButton.setEnabled(true);
                }
            } else {
                playerIds.clear();
            }

            if (docSnapshot.contains("holeCards")) {
                Map<String, List<String>> holeCardMap = (Map<String, List<String>>) docSnapshot.get("holeCards");

                for (int i = 0; i < playerIds.size() && i < holeCardViews.length; i++) {
                    String uid = playerIds.get(i);
                    if (holeCardMap.containsKey(uid)) {
                        List<String> cards = holeCardMap.get(uid);

                        if (uid.equals(currentUID)) {
                            holeCardViews[i].setText(cards.get(0) + "  " + cards.get(1));
                        } else {
                            holeCardViews[i].setText("🂠  🂠");
                        }
                    }
                }
            }

            String stage = docSnapshot.getString("stage");
            if (stage != null) {
                updateStage(stage);
            }

            Object d = docSnapshot.get("deck");
            if (d instanceof List) {
                //noinspection unchecked
                deck = (List<String>) d;
            } else {
                deck = new ArrayList<>();
            }

            if (docSnapshot.contains("currentBet")) {
                currentBet = ((Long) docSnapshot.get("currentBet")).intValue();
            }

            if (docSnapshot.contains("foldedPlayers")) {
                List<String> list = (List<String>) docSnapshot.get("foldedPlayers");
                foldedPlayers.clear();
                foldedPlayers.addAll(list);
            }

            if (docSnapshot.contains("pot")) {
                pot = ((Long) docSnapshot.get("pot")).intValue();
                updatePotView();
            }

            if (docSnapshot.contains("playerBets")) {
                Map<String, Object> firestoreBets = (Map<String, Object>) docSnapshot.get("playerBets");

                for (int i = 0; i < playerIds.size() && i < betViews.length; i++) {
                    String uid = playerIds.get(i);
                    Object betObj = firestoreBets.getOrDefault(uid, 0);
                    int bet = (betObj instanceof Number) ? ((Number) betObj).intValue() : 0;

                    if (betViews[i] != null) {
                        betViews[i].setText("Bet: " + bet);
                    }
                }
            }

            if (docSnapshot.contains("lastRaise")) {
                Long lastRaiseLong = docSnapshot.getLong("lastRaise");
                if (lastRaiseLong != null) {
                    lastRaise = lastRaiseLong.intValue();
                }
            }

            if (docSnapshot.contains("chips")) {
                Map<String, Long> chipMap = (Map<String, Long>) docSnapshot.get("chips");

                for (int i = 0; i < playerIds.size() && i < chipViews.length; i++) {
                    String uid = playerIds.get(i);
                    long chips = chipMap.getOrDefault(uid, 0L);
                    chipViews[i].setText("Chips: " + chips);  // TextView под ставкой
                }
            }

            if (docSnapshot.contains("playersRaisedThisRound")) {
                List<String> raisedList = (List<String>) docSnapshot.get("playersRaisedThisRound");
                if (raisedList != null) {
                    playersRaisedThisRound.clear();
                    playersRaisedThisRound.addAll(raisedList);
                }
            }
            if (docSnapshot.contains("communityCards")) {
                List<String> commCards = (List<String>) docSnapshot.get("communityCards");
                if (commCards != null) {
                    for (int i = 0; i < commCards.size(); i++) {
                        commViews[i].setText(commCards.get(i));
                    }
                }
            }
        });
    }

    private void displayPlayerNames() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        playerNames.clear();

        for (int i = 0; i < playerViews.length; ++i) {
            final int index = i;
            TextView tv = playerViews[i];

            if (i < playerIds.size()) {
                String uid = playerIds.get(i);
                db.collection("players").document(uid).get()
                        .addOnSuccessListener(doc -> {
                            String name = doc.exists() ? doc.getString("name") : "unknown";
                            while (playerNames.size() <= index) playerNames.add("");
                            playerNames.set(index, name);
                            tv.setText(name);

                            if (uid.equals(currentPlayerID)) {
                                tv.setTextColor(Color.RED);
                                tv.setTypeface(null, Typeface.BOLD);
                            } else {
                                tv.setTextColor(Color.BLACK);
                                tv.setTypeface(null, Typeface.NORMAL);
                            }
                        })
                        .addOnFailureListener(e -> tv.setText("(error)"));
            } else {
                tv.setText("xxx");
            }
        }
    }
}
