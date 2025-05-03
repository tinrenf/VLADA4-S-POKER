package com.example.poker;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;

import android.view.View;
import android.graphics.Color;
import android.graphics.Typeface;

import android.util.Log;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import android.content.Intent;

public class GameActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    int big_blind = 50;
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
    private Map<String, Integer> playerBets = new HashMap<>();
    private List<String> foldedPlayers = new ArrayList<>();
    private String currentUID;
    private TextView potView;

    private ImageView[] commViews;
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

        playerIds = getIntent().getStringArrayListExtra("playerIds");

        if (playerIds == null) {
            playerIds = new ArrayList<>();
        }

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

        commViews = new ImageView[]{
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

                        String smallBlindPlayer = playerIds.get(0);
                        String bigBlindPlayer = playerIds.get(1);

                        if (playerIds.size() >= 2) {
                            DocumentReference gameRef = db.collection("games").document(gameId);

                            cur_rate = big_blind;

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
                                    gameRef.update("currentBet", cur_rate);
                                }
                            });

                            Map<String, Object> updates = new HashMap<>();
                            Map<String, Integer> playerBets = new HashMap<>();

                            playerBets.put(smallBlindPlayer, small_blind);
                            playerBets.put(bigBlindPlayer, big_blind);

                            updates.put("playerBets", playerBets);
                            updates.put("pot", 0);
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

                        AtomicInteger remaining = new AtomicInteger(playerIds.size());

                        for (String uid : playerIds) {
                            db.collection("players").document(uid).get()
                                    .addOnSuccessListener(docSnapshot -> {
                                        if (docSnapshot.exists()) {
                                            Long money = docSnapshot.getLong("money");
                                            if (money != null) {
                                                chips.put(uid, money);
                                            } else {
                                                chips.put(uid, 5252L);
                                            }
                                        } else {
                                            chips.put(uid, 5252L);
                                        }

                                        if (remaining.decrementAndGet() == 0) {
                                            Object smallBlindValue = chips.get(smallBlindPlayer);
                                            Object bigBlindValue = chips.get(bigBlindPlayer);
                                            if (smallBlindValue instanceof Number && bigBlindValue instanceof Number) {
                                                int newSmall = ((Number) smallBlindValue).intValue() - small_blind;
                                                int newBig = ((Number) bigBlindValue).intValue() - big_blind;

                                                chips.put(smallBlindPlayer, newSmall);
                                                chips.put(bigBlindPlayer, newBig);
                                            }

                                            db.collection("games").document(gameId).update("chips", chips);
                                        }
                                    });
                        }

                        lastRaise = 0;

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("deck", deck);
                        updates.put("holeCards", holeCardStrings);
                        updates.put("gameStarted", true);
                        updates.put("currentBet", cur_rate);
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
            if (!currentUID.equals(currentPlayerID) ||
                    playersRaisedThisRound.contains(currentUID) ||
                    foldedPlayers.contains(currentPlayerID)) return;

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference gameRef = db.collection("games").document(gameId);

            int raiseAmount = cur_rate + big_blind;

            gameRef.get().addOnSuccessListener(docSnapshot -> {
                if (docSnapshot.exists()) {
                    Map<String, Long> chipsMap = (Map<String, Long>) docSnapshot.get("chips");
                    long currentChips = chipsMap.get(currentPlayerID);
                    Map<String, Object> rawMap = (Map<String, Object>) docSnapshot.get("playerBets");
                    Map<String, Integer> playerBets = new HashMap<>();
                    if (rawMap != null) {
                        for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                            playerBets.put(entry.getKey(), ((Number) entry.getValue()).intValue());
                        }
                    }
                    int prevBet = playerBets.getOrDefault(currentUID, 0);

                    if (currentChips - (raiseAmount - prevBet) >= 0) {
                        chipsMap.put(currentPlayerID, currentChips - (raiseAmount - prevBet));
                        playerBets = (Map<String, Integer>) docSnapshot.get("playerBets");
                        playerBets.put(currentUID, raiseAmount);
                        cur_rate = raiseAmount;

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("chips", chipsMap);
                        updates.put("playerBets", playerBets);
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
            if (!currentUID.equals(currentPlayerID) || foldedPlayers.contains(currentPlayerID)) return;

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference gameRef = db.collection("games").document(gameId);

            gameRef.get().addOnSuccessListener(docSnapshot -> {
                if (docSnapshot.exists()) {
                    Map<String, Object> rawMap = (Map<String, Object>) docSnapshot.get("playerBets");
                    Map<String, Integer> playerBets = new HashMap<>();
                    if (rawMap != null) {
                        for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                            playerBets.put(entry.getKey(), ((Number) entry.getValue()).intValue());
                        }
                    }
                    int prevBet = playerBets.getOrDefault(currentPlayerID, 0);
                    int toCall = cur_rate - prevBet;

                    if (toCall <= 0) {
                        proceedToNextPlayer();
                        return;
                    }

                    Map<String, Long> chipsMap = (Map<String, Long>) docSnapshot.get("chips");
                    long currentChips = chipsMap.get(currentPlayerID);

                    if (currentChips - toCall >= 0) {
                        chipsMap.put(currentPlayerID, currentChips - toCall);
                        playerBets.put(currentPlayerID, cur_rate);
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("chips", chipsMap);
                        updates.put("playerBets", playerBets);

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
            if (!currentUID.equals(currentPlayerID) || foldedPlayers.contains(currentPlayerID)) return;

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference gameRef = db.collection("games").document(gameId);

            gameRef.get().addOnSuccessListener(docSnapshot -> {
                if (docSnapshot.exists()) {
                    List<String> foldedList = (List<String>) docSnapshot.get("foldedPlayers");
                    if (foldedList == null) foldedList = new ArrayList<>();

                    if (!foldedList.contains(currentUID)) {
                        foldedList.add(currentUID);
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("foldedPlayers", foldedList);
                    updates.put("playerBets", playerBets);

                    gameRef.update(updates);

                    if (foldedList.size() == playerIds.size() - 1) {
                        for (int i = 0; i < playerIds.size(); ++i) {
                            if (!foldedList.contains(playerIds.get(i))) {
                                foldEndGame(playerIds.get(i));
                                break;
                            }
                        }
                    } else {
                        proceedToNextPlayer();
                    }
                }
            });
        });
    }

    private void foldEndGame(String winnerID) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference gameRef = db.collection("games").document(gameId);

        gameRef.get().addOnSuccessListener(docSnapshot -> {
            if (!docSnapshot.exists()) return;

            Long pot = docSnapshot.getLong("pot");
            if (pot == null) pot = 0L;

            Map<String, Long> chips = (Map<String, Long>) docSnapshot.get("chips");
            if (chips == null) chips = new HashMap<>();

            Map<String, Object> gameUpdates = new HashMap<>();

            for (Map.Entry<String, Long> e : chips.entrySet()) {
                String uid = e.getKey();
                Long moneyLong = e.getValue();
                int money = moneyLong != null ? moneyLong.intValue() : 0;
                if (uid.equals(winnerID))
                    money += pot;
                FirebaseFirestore.getInstance().collection("players")
                        .document(uid).update("money", money);
            }

            pot = 0L;
            gameUpdates.put("pot", pot);
            gameRef.update(gameUpdates);
            gameRef.update("resetGame", true);
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
                    commViews[i].setImageResource(getResources().getIdentifier(Card.toImage(communityCards.get(i)), "drawable", getPackageName()));
                } else {
                    commViews[i].setImageDrawable(null);
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

            betsUpdate();

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
                // Конец игры
                return;
            }

            gameRef.update(updates);
        });
    }
    private void betsUpdate() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference gameRef = db.collection("games").document(gameId);

        gameRef.get().addOnSuccessListener(docSnapshot -> {
            if (!docSnapshot.exists()) return;

            Map<String, Object> updates = new HashMap<>();

            Map<String, Object> rawPlayerBets = (Map<String, Object>) docSnapshot.get("playerBets");
            Map<String, Integer> playerBets = new HashMap<>();

            if (rawPlayerBets != null) {
                for (Map.Entry<String, Object> entry : rawPlayerBets.entrySet()) {
                    Object val = entry.getValue();
                    if (val instanceof Number) {
                        playerBets.put(entry.getKey(), ((Number) val).intValue());
                    }
                }
            }
            cur_rate = 0;
            pot = ((Long) docSnapshot.get("pot")).intValue();
            for (Object val : playerBets.values()) {
                if (val instanceof Number) {
                    pot += ((Number) val).intValue();
                }
            }
            playerBets.clear();
            updates.put("playerBets", playerBets);
            updates.put("pot", pot);
            updates.put("currentBet", cur_rate);

            gameRef.update(updates);
        });
    }

    private void proceedToNextPlayer() {
        DocumentReference gameRef = db.collection("games").document(gameId);

        gameRef.get().addOnSuccessListener(docSnapshot -> {
            if (!docSnapshot.exists()) return;

            int currentIndex = playerIds.indexOf(currentPlayerID);
            int nextIndex = (currentIndex + 1) % playerIds.size();

            List<String> foldedPlayers = (List<String>) docSnapshot.get("foldedPlayers");
            if (foldedPlayers == null) foldedPlayers = new ArrayList<>();

            int originalIndex = nextIndex;
            while (foldedPlayers.contains(playerIds.get(originalIndex))) {
                originalIndex = (originalIndex + 1) % playerIds.size();
            }
            nextIndex = originalIndex;

            if (nextIndex == lastRaise) {
                proceedToNextStage();
            }
            List<String> playerIds = (List<String>) docSnapshot.get("playerIds");
            String nextPlayerUid = playerIds.get(nextIndex);
            gameRef.update("currentPlayerID", nextPlayerUid);
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
                cur_rate = ((Long) docSnapshot.get("currentBet")).intValue();
            }

            if (docSnapshot.contains("foldedPlayers")) {
                List<String> list = (List<String>) docSnapshot.get("foldedPlayers");
                if (list != null) {
                    foldedPlayers.clear();
                    foldedPlayers.addAll(list);
                }
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
                    chipViews[i].setText("Chips: " + chips);  // TextView фишек
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
                        commViews[i].setImageResource(getResources().getIdentifier(Card.toImage(commCards.get(i)), "drawable", getPackageName()));
                    }
                }
            }

            Boolean reset = docSnapshot.getBoolean("resetGame");
            if (Boolean.TRUE.equals(reset)) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("resetGame", false);
                gameRef.update(updates);
                Intent intent = getIntent();
                intent.putExtra("gameId", gameId);
                intent.putStringArrayListExtra("playerIds", new ArrayList<>(playerIds));
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                finish();
                startActivity(intent);
            }

            /*updates.put("stage", "preflop");
            updates.put("lastRaise", null);
            updates.put("communtyCards", null);

            updates.put("chips", null);
            updates.put("currentBet", null);
            updates.put("currentPlayerID", null);
            updates.put("deck", null);
            updates.put("foldedPlayers", null);
            updates.put("gameStarted", null);
            updates.put("holeCards", null);
            updates.put("pot", null);*/
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
