package com.example.poker;

import java.util.*;
import android.content.*;
import android.view.*;
import android.widget.*;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;

import android.graphics.Color;
import android.graphics.Typeface;
import java.util.concurrent.atomic.AtomicInteger;
import com.google.firebase.firestore.FieldValue;
import androidx.appcompat.app.AlertDialog;
import android.annotation.SuppressLint;

public class GameActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    int big_blind = 0;
    int small_blind = 0;
    int cur_rate = 0;
    private ImageView[] holeCardViews1, holeCardViews2;
    private TextView[] playerViews;
    private TextView[] betViews;
    private List<String> playerIds = new ArrayList<>();
    private Button startButton;
    private List<String> playerNames = new ArrayList<>();
    private String gameId;

    private int pot = 0;
    private Map<String, Integer> playerBets = new HashMap<>();
    private List<String> foldedPlayers = new ArrayList<>();
    private String currentUID;
    private TextView potView;

    private ImageView[] commViews;
    private List<String> deck;
    private String creatorID;
    private String currentPlayerID;
    private String lastRaise = creatorID;

    private TextView[] chipViews;
    private List<String> playersRaisedThisRound = new ArrayList<>();
    private List<String> joinAfterStart = new ArrayList<>();
    private String gameName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        gameId = getIntent().getStringExtra("gameId");
        currentUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        potView = findViewById(R.id.pot_text);

        playerViews = new TextView[]{findViewById(R.id.player5),
                findViewById(R.id.player1_name),
                findViewById(R.id.player2_name),
                findViewById(R.id.player3_name),
                findViewById(R.id.player4_name)
        };

        holeCardViews1 = new ImageView[]{
                findViewById(R.id.card1_player5),
                findViewById(R.id.card1_player1),
                findViewById(R.id.card1_player2),
                findViewById(R.id.card1_player3),
                findViewById(R.id.card1_player4)
        };

        holeCardViews2 = new ImageView[]{
                findViewById(R.id.card2_player5),
                findViewById(R.id.card2_player1),
                findViewById(R.id.card2_player2),
                findViewById(R.id.card2_player3),
                findViewById(R.id.card2_player4)
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

        db.collection("games").document(gameId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                List<String> ids = (List<String>) doc.get("playerIds");
                if (ids != null) {
                    playerIds = new ArrayList<>(ids);
                }
                Long curBigBlind = doc.getLong("bigBlind");
                if (curBigBlind != null) {
                    big_blind = curBigBlind.intValue();
                    small_blind = big_blind / 2;
                }
                gameName = doc.getString("name");
            }
        });

        startButton = findViewById(R.id.start_button);
        Button callButton = findViewById(R.id.button_call);
        Button raiseButton = findViewById(R.id.button_raise);
        Button foldButton = findViewById(R.id.button_fold);

        loadGameDetails(gameId);

        startButton.setOnClickListener(v -> {
            db.collection("games").document(gameId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.contains("gameStarted") && Boolean.TRUE.equals(doc.get("gameStarted"))) {
                            return;
                        }

                        if (playerIds.size() <= 1) {
                            Toast.makeText(getApplicationContext(), "Нужно минимум 2 игрока для старта игры", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String smallBlindPlayer = playerIds.get(0);
                        String bigBlindPlayer = playerIds.get(1);

                        DocumentReference gameRef = db.collection("games").document(gameId);

                        cur_rate = big_blind;

                        //основные штуки
                        gameRef.get().addOnSuccessListener(docSnapshot -> {
                            if (docSnapshot.exists()) {
                                List<String> playerIds = (List<String>) docSnapshot.get("playerIds");
                                if (playerIds != null && !playerIds.isEmpty()) {
                                    if (playerIds.size() == 2) {
                                        currentPlayerID = playerIds.get(0);
                                        lastRaise = currentPlayerID;
                                    } else if (playerIds.size() > 2) {
                                        currentPlayerID = playerIds.get(2);
                                        lastRaise = currentPlayerID;
                                    }
                                }

                                Map<String, Object> updates = new HashMap<>();
                                Map<String, Integer> playerBets = new HashMap<>();

                                playerBets.put(smallBlindPlayer, small_blind);
                                playerBets.put(bigBlindPlayer, big_blind);

                                updates.put("playerBets", playerBets);
                                updates.put("status", "started");
                                updates.put("currentPlayerID", currentPlayerID);
                                updates.put("currentBet", cur_rate);
                                updates.put("lastRaise", lastRaise);
                                updates.put("pot", 0);
                                updates.put("stage", "preflop");

                                gameRef.update(updates);
                            }
                        });

                        Map<String, List<Card>> holeCards = PreFlop.deal(playerIds);

                        // Карты у игроков на руках
                        for (int i = 0; i < playerIds.size() && i < holeCardViews1.length; i++) {
                            List<Card> hand = holeCards.get(playerIds.get(i));
                            holeCardViews1[i].setImageResource(getResources().getIdentifier(Card.toImage(hand.get(0).toString()), "drawable", getPackageName()));
                            holeCardViews2[i].setImageResource(getResources().getIdentifier(Card.toImage(hand.get(1).toString()), "drawable", getPackageName()));
                        }

                        Map<String, Object> holeCardStrings = new HashMap<>();
                        for (Map.Entry<String, List<Card>> entry : holeCards.entrySet()) {
                            String uid = entry.getKey();
                            List<Card> hand = entry.getValue();
                            List<String> stringHand = Arrays.asList(hand.get(0).toString(), hand.get(1).toString());
                            holeCardStrings.put(uid, stringHand);
                        }

                        deck = new ArrayList<>();
                        int[] suits = {1, 2, 3, 4};
                        for (int s : suits) {
                            for (int r = 2; r <= 14; r++) {
                                Card curCard = new Card(s, r);
                                deck.add(curCard.toString());
                            }
                        }
                        Collections.shuffle(deck);

                        for (List<Card> hand : holeCards.values()) {
                            deck.remove(hand.get(0).toString());
                            deck.remove(hand.get(1).toString());
                        }

                        Map<String, Object> chips = new HashMap<>();
                        AtomicInteger remaining = new AtomicInteger(playerIds.size());
                        for (String uid : playerIds) {
                            db.collection("players").document(uid).get()
                                    .addOnSuccessListener(doc1 -> {
                                        if (doc1.exists()) {
                                            Long money = doc1.getLong("money");
                                            if (money != null) {
                                                chips.put(uid, money);
                                            } else {
                                                chips.put(uid, 2525L);
                                            }
                                        } else {
                                            chips.put(uid, 5252L);
                                        }

                                        if (remaining.decrementAndGet() == 0) {
                                            Object smallBlind = chips.get(smallBlindPlayer);
                                            Object bigBlind = chips.get(bigBlindPlayer);
                                            if (smallBlind instanceof Number && bigBlind instanceof Number) {
                                                int newSmall = ((Number) smallBlind).intValue() - small_blind;
                                                int newBig = ((Number) bigBlind).intValue() - big_blind;
                                                chips.put(smallBlindPlayer, newSmall);
                                                chips.put(bigBlindPlayer, newBig);
                                            }
                                            db.collection("games").document(gameId).update("chips", chips);
                                        }
                                    });
                        }

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("deck", deck);
                        updates.put("holeCards", holeCardStrings);
                        updates.put("gameStarted", true);
                        updates.put("currentBet", cur_rate);
                        updates.put("chips", chips);

                        db.collection("games").document(gameId).update(updates);
                        startButton.setVisibility(View.GONE);
                    });
        });//Когда нажали на кнопку старт

        raiseButton.setOnClickListener(v -> {
            if (!currentUID.equals(currentPlayerID) ||
                    playersRaisedThisRound.contains(currentUID) ||
                    foldedPlayers.contains(currentPlayerID)) return;

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference gameRef = db.collection("games").document(gameId);

            gameRef.get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Map<String, Long> chipsMap = (Map<String, Long>) doc.get("chips");
                    long curChips = chipsMap.get(currentPlayerID);
                    Map<String, Object> rawMap = (Map<String, Object>) doc.get("playerBets");
                    Map<String, Integer> playerBets = new HashMap<>();
                    if (rawMap != null) {
                        for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                            playerBets.put(entry.getKey(), ((Number) entry.getValue()).intValue());
                        }
                    }
                    int prevBet = playerBets.getOrDefault(currentUID, 0);
                    int minRaise = cur_rate + big_blind;
                    int maxRaise = prevBet + (int)curChips;

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Choose raise amount");

                    LinearLayout layout = new LinearLayout(this);
                    layout.setOrientation(LinearLayout.VERTICAL);
                    layout.setPadding(50, 40, 50, 10);

                    SeekBar seekBar = new SeekBar(this);
                    seekBar.setMax(maxRaise - minRaise);
                    seekBar.setProgress(0);

                    TextView valueText = new TextView(this);
                    valueText.setText("Raise: " + minRaise);
                    valueText.setPadding(0, 20, 0, 20);

                    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            valueText.setText("Raise: " + (minRaise + progress));
                        }
                        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                    });

                    layout.addView(valueText);
                    layout.addView(seekBar);
                    builder.setView(layout);

                    builder.setPositiveButton("Raise", (dialog, which) -> {
                        int raiseAmount = minRaise + seekBar.getProgress();

                        if (curChips - (raiseAmount - prevBet) >= 0) {
                            chipsMap.put(currentPlayerID, curChips - (raiseAmount - prevBet));
                            playerBets.put(currentUID, raiseAmount);

                            Map<String, Object> updates = new HashMap<>();
                            updates.put("chips", chipsMap);
                            updates.put("playerBets", playerBets);
                            updates.put("currentBet", raiseAmount);
                            updates.put("lastRaise", currentUID);
                            updates.put("playersRaisedThisRound", playersRaisedThisRound);
                            gameRef.update(updates);

                            updatePotView();
                            proceedToNextPlayer(false);
                        } else {
                            Toast.makeText(this, "Not enough chips to raise", Toast.LENGTH_SHORT).show();
                        }
                    });
                    builder.setNegativeButton("Cancel", null);
                    builder.show();
                }
            });
        });

        callButton.setOnClickListener(v -> {
            if (!currentUID.equals(currentPlayerID) || foldedPlayers.contains(currentPlayerID))
                return;

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference gameRef = db.collection("games").document(gameId);

            gameRef.get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Map<String, Object> rawMap = (Map<String, Object>) doc.get("playerBets");
                    Map<String, Integer> playerBets = new HashMap<>();
                    if (rawMap != null) {
                        for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                            playerBets.put(entry.getKey(), ((Number) entry.getValue()).intValue());
                        }
                    }
                    int prevBet = playerBets.getOrDefault(currentPlayerID, 0);
                    int toCall = cur_rate - prevBet;

                    if (toCall <= 0) {
                        proceedToNextPlayer(false);
                        return;
                    }

                    Map<String, Long> chipsMap = (Map<String, Long>) doc.get("chips");
                    long currentChips = chipsMap.get(currentPlayerID);

                    if (currentChips - toCall >= 0) {
                        chipsMap.put(currentPlayerID, currentChips - toCall);
                        playerBets.put(currentPlayerID, cur_rate);
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("chips", chipsMap);
                        updates.put("playerBets", playerBets);

                        gameRef.update(updates);
                        updatePotView();
                        proceedToNextPlayer(false);
                    } else {
                        Toast.makeText(this, "Not enough chips to call", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });

        foldButton.setOnClickListener(v -> {
            if (!currentUID.equals(currentPlayerID) || foldedPlayers.contains(currentPlayerID))
                return;

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference gameRef = db.collection("games").document(gameId);

            gameRef.get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    List<String> foldedList = (List<String>) doc.get("foldedPlayers");
                    if (foldedList == null) foldedList = new ArrayList<>();

                    boolean fuckFold = false;
                    if (!foldedList.contains(currentUID)) {
                        foldedList.add(currentUID);
                        if (currentUID.equals(lastRaise)) {
                            fuckFold = true;
                        }
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("foldedPlayers", foldedList);

                    gameRef.update(updates);

                    if (foldedList.size() == playerIds.size() - 1) {
                        for (int i = 0; i < playerIds.size(); ++i) {
                            if (!foldedList.contains(playerIds.get(i))) {
                                foldEndGame(playerIds.get(i));
                                break;
                            }
                        }
                    } else {
                        proceedToNextPlayer(fuckFold);
                    }
                }
            });
        });
    }
    @SuppressLint("MissingSuperCall")
    public void onBackPressed() {
        showLeaveConfirmationDialog();
    }
    private void showLeaveConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Game")
                .setMessage("Are you sure you want to leave the game?")
                .setPositiveButton("Leave", (dialog, which) -> leaveGame())
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void leaveGame() {
        String currentUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DocumentReference gameRef = FirebaseFirestore.getInstance().collection("games").document(gameId);

        gameRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                List<String> playerIds = new ArrayList<>((List<String>) doc.get("playerIds"));
                String currentPlayerID = doc.getString("currentPlayerID");
                String lastRaise = doc.getString("lastRaise");

                if (playerIds != null && playerIds.contains(currentUID)) {
                    if (currentUID.equals(currentPlayerID)) {
                        proceedToNextPlayer(false);
                    }
                    playerIds.remove(currentUID);
                    if (currentUID.equals(lastRaise)) {
                        List<String> originalPlayerIds = (List<String>) doc.get("playerIds");
                        int leavingIndex = originalPlayerIds.indexOf(currentUID);

                        int prevIndex = leavingIndex - 1;
                        if (prevIndex < 0 && playerIds.size() > 0) prevIndex = playerIds.size() - 1;

                        if (playerIds.size() > 0) {
                            lastRaise = playerIds.get(prevIndex);
                        } else {
                            lastRaise = null;
                        }
                        if (currentUID.equals(currentPlayerID))
                            proceedToNextStage();
                    }

                    Long pot = doc.contains("pot") ? doc.getLong("pot") : 0L;
                    Map<String, Long> playerBets = (Map<String, Long>) doc.get("playerBets");
                    Long leavingBet = (playerBets != null && playerBets.containsKey(currentUID)) ? playerBets.get(currentUID) : 0L;

                    if (leavingBet != null) {
                        pot += leavingBet;
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("pot", pot);
                    updates.put("playerIds", playerIds);
                    updates.put("holeCards." + currentUID, FieldValue.delete());
                    updates.put("playerBets." + currentUID, FieldValue.delete());
                    updates.put("lastRaise", lastRaise);

                    if (playerIds.size() == 1) {
                        String winnerUid = playerIds.get(0);
                    }//случай победы когда все вышли

                    gameRef.update(updates)
                            .addOnSuccessListener(aVoid -> {
                                for (ImageView card : holeCardViews1) {
                                    card.setImageDrawable(null);
                                }
                                for (ImageView card : holeCardViews2) {
                                    card.setImageDrawable(null);
                                }
                                finish();
                            });
                }
            }
        });
    }

    private void foldEndGame(String winnerID) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference gameRef = db.collection("games").document(gameId);

        gameRef.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;

            Map<String, Object> gameUpdates = new HashMap<>();

            Long pot = doc.getLong("pot");
            if (pot == null) pot = 0L;

            Map<String, Long> chips = (Map<String, Long>) doc.get("chips");
            if (chips == null) chips = new HashMap<>();

            Map<String, Object> pb = (Map<String, Object>) doc.get("playerBets");
            if (pb == null) pb = new HashMap<>();

            Long newWinnerMoney = pot;
            for (Object value : pb.values()) {
                if (value instanceof Number) {
                    newWinnerMoney += ((Number) value).longValue();
                }
            }

            for (Map.Entry<String, Long> e : chips.entrySet()) {
                String uid = e.getKey();
                Long moneyLong = e.getValue();
                int money = moneyLong != null ? moneyLong.intValue() : 0;
                if (uid.equals(winnerID)) {
                    newWinnerMoney += money;
                    money = newWinnerMoney.intValue();
                }
                FirebaseFirestore.getInstance().collection("players")
                        .document(uid).update("money", money);
            }

            chips.put(winnerID, newWinnerMoney);
            pot = 0L;
            gameUpdates.put("pot", pot);
            gameUpdates.put("chips", chips);
            gameRef.update(gameUpdates);

            DocumentReference oldGameRef = db.collection("games").document(gameId);
            oldGameRef.get().addOnSuccessListener(doc1 -> {
                if (!doc1.exists()) return;

                List<String> players = (List<String>) doc1.get("playerIds");
                Map<String, Object> newGame = new HashMap<>();
                newGame.put("creatorID", creatorID);
                newGame.put("playerIds", players);
                newGame.put("maxPlayers", 5);
                newGame.put("status", "waiting");
                newGame.put("timestamp", FieldValue.serverTimestamp());
                newGame.put("chips", doc1.get("chips"));
                newGame.put("bigBlind", big_blind);
                newGame.put("name", gameName);

                db.collection("games")
                        .add(newGame)
                        .addOnSuccessListener(newGameRef -> {
                            String newGameId = newGameRef.getId();
                            oldGameRef.update("newGameId", newGameId);
                        });
            });
        });
    }

    private void updateStage(String stage) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference gameRef = db.collection("games").document(gameId);

        gameRef.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;
            List<String> communityCards = (List<String>) doc.get("communityCards");
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

        gameRef.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;

            String stage = doc.getString("stage");
            List<String> deck = (List<String>) doc.get("deck");

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
                GameResult gameResult = new GameResult(gameId, creatorID, playerIds, foldedPlayers, gameName, big_blind);
                gameResult.findWinnerUid();
                return;
            }

            gameRef.update(updates);
        });
    }

    private void betsUpdate() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference gameRef = db.collection("games").document(gameId);

        gameRef.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;

            Map<String, Object> updates = new HashMap<>();

            Map<String, Object> rawPlayerBets = (Map<String, Object>) doc.get("playerBets");
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
            pot = ((Long) doc.get("pot")).intValue();
            for (Object val : playerBets.values()) {
                if (val instanceof Number) {
                    pot += ((Number)val).intValue();
                }
            }
            playerBets.clear();
            updates.put("playerBets", playerBets);
            updates.put("pot", pot);
            updates.put("currentBet", cur_rate);

            gameRef.update(updates);
        });
    }

    private void proceedToNextPlayer(boolean fuckFold) {
        DocumentReference gameRef = db.collection("games").document(gameId);

        gameRef.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;

            List<String> originalPlayerIds = (List<String>) doc.get("playerIds");
            if (originalPlayerIds == null || originalPlayerIds.isEmpty()) return;

            String currentPlayerID = doc.getString("currentPlayerID");
            String lastRaiseUid = doc.getString("lastRaise");

            if (currentPlayerID == null) return;

            int currentIndex = originalPlayerIds.indexOf(currentPlayerID);
            int nextIndex = (currentIndex + 1) % originalPlayerIds.size();

            List<String> foldedPlayers = (List<String>) doc.get("foldedPlayers");
            if (foldedPlayers == null) foldedPlayers = new ArrayList<>();

            //след не скинувший карты. Менять, если меняем порядок игроков
            int loopStart = nextIndex;
            while (foldedPlayers.contains(originalPlayerIds.get(nextIndex))) {
                nextIndex = (nextIndex + 1) % originalPlayerIds.size();
                if (nextIndex == loopStart) {
                    proceedToNextStage();
                    return;
                }
            }

            String nextPlayerUid = originalPlayerIds.get(nextIndex);
            if (nextPlayerUid.equals(lastRaiseUid)) {
                proceedToNextStage();
            }

            if (fuckFold) {
                int lrid = playerIds.indexOf(lastRaise);
                lrid = (lrid + 1) % (playerIds.size());
                lastRaise = playerIds.get(lrid);
            }

            gameRef.update("lastRaise", lastRaise);
            gameRef.update("currentPlayerID", nextPlayerUid);
        });
    }

    private void updatePotView() {
        potView.setText("Pot: " + pot);
    }

    private void loadGameDetails(String gameId) {
        DocumentReference gameRef = db.collection("games").document(gameId);

        gameRef.addSnapshotListener((doc, e) -> {
            if (e != null || doc == null || !doc.exists()) {
                return;
            }

            Game game = doc.toObject(Game.class);
            creatorID = doc.getString("creatorID");
            String currentUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String currentTurnUid = doc.getString("currentPlayerID");

            currentPlayerID = currentTurnUid;

            Boolean gameStarted = doc.getBoolean("gameStarted");
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
                playerIds = reorderPlayers(playerIds, currentUID);
                displayPlayerNames();

                if (creatorID != null && creatorID.equals(currentUID) && (gameStarted == null || !gameStarted)) {
                    startButton.setEnabled(true);
                }
            } else {
                playerIds.clear();
            }

            if (doc.contains("holeCards")) {
                Map<String, List<String>> holeCardMap = (Map<String, List<String>>) doc.get("holeCards");

                for (int i = 0; i < playerIds.size() && i < holeCardViews1.length; i++) {
                    String uid = playerIds.get(i);
                    if (holeCardMap.containsKey(uid)) {
                        List<String> cards = holeCardMap.get(uid);

                        if (uid.equals(currentUID)) {
                            holeCardViews1[i].setImageResource(getResources().getIdentifier(Card.toImage(cards.get(0)), "drawable", getPackageName()));
                            holeCardViews2[i].setImageResource(getResources().getIdentifier(Card.toImage(cards.get(1)), "drawable", getPackageName()));
                        } else {
                            holeCardViews1[i].setImageResource(getResources().getIdentifier(Card.toImage("🂠"), "drawable", getPackageName()));
                            holeCardViews2[i].setImageResource(getResources().getIdentifier(Card.toImage("🂠"), "drawable", getPackageName()));
                        }
                    }
                }
            }

            String stage = doc.getString("stage");
            if (stage != null) {
                updateStage(stage);
            }

            Object d = doc.get("deck");
            if (d instanceof List) {
                deck = (List<String>) d;
            } else {
                deck = new ArrayList<>();
            }

            if (doc.contains("currentBet")) {
                Long currentBet = doc.getLong("currentBet");
                cur_rate = currentBet.intValue();
                Button raiseButton = findViewById(R.id.button_raise);
                Button callButton = findViewById(R.id.button_call);

                if (currentBet != null && currentBet > 0) {
                    raiseButton.setText("Raise");
                    callButton.setText("Call");
                } else {
                    raiseButton.setText("Bet");
                    callButton.setText("Check");
                }
            }

            if (doc.contains("foldedPlayers")) {
                List<String> list = (List<String>) doc.get("foldedPlayers");
                if (list != null) {
                    foldedPlayers.clear();
                    foldedPlayers.addAll(list);
                }
            }

            if (doc.contains("pot")) {
                pot = ((Long) doc.get("pot")).intValue();
                updatePotView();
            }

            if (doc.contains("playerBets")) {
                Map<String, Object> firestoreBets = (Map<String, Object>) doc.get("playerBets");

                for (int i = 0; i < playerIds.size() && i < betViews.length; i++) {
                    String uid = playerIds.get(i);
                    Object betObj = firestoreBets.getOrDefault(uid, 0);
                    int bet = (betObj instanceof Number) ? ((Number) betObj).intValue() : 0;

                    if (betViews[i] != null) {
                        betViews[i].setText("Bet: " + bet);
                    }
                }
            }

            if (doc.contains("lastRaise")) {
                String lastRaiseTM = doc.getString("lastRaise");
                if (lastRaiseTM != null) {
                    lastRaise = lastRaiseTM;
                }
            }

            if (doc.contains("chips")) {
                Map<String, Long> chipMap = (Map<String, Long>) doc.get("chips");

                for (int i = 0; i < playerIds.size() && i < chipViews.length; i++) {
                    String uid = playerIds.get(i);
                    long chips = chipMap.getOrDefault(uid, 0L);
                    chipViews[i].setText("Chips: " + chips);
                }
            }

            if (doc.contains("joinAfterStart")) {
                List<String> list = (List<String>) doc.get("foldedPlayers");
                if (list != null) {
                    joinAfterStart.clear();
                    joinAfterStart.addAll(list);
                }
            }

            if (doc.contains("playersRaisedThisRound")) {
                List<String> raisedList = (List<String>) doc.get("playersRaisedThisRound");
                if (raisedList != null) {
                    playersRaisedThisRound.clear();
                    playersRaisedThisRound.addAll(raisedList);
                }
            }
            if (doc.contains("communityCards")) {
                List<String> commCards = (List<String>) doc.get("communityCards");
                if (commCards != null) {
                    for (int i = 0; i < commCards.size(); i++) {
                        commViews[i].setImageResource(getResources().getIdentifier(Card.toImage(commCards.get(i)), "drawable", getPackageName()));
                    }
                }
            }

            String newGameId = doc.getString("newGameId");
            if (newGameId != null && !newGameId.isEmpty()) {
                String oldGameID = gameId;
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                if (currentUID.equals(creatorID)) {
                    DocumentReference oldGameRef = db.collection("games").document(oldGameID);

                    oldGameRef.update("newGameId", FieldValue.delete())
                            .addOnSuccessListener(aVoid -> {
                                oldGameRef.delete().addOnSuccessListener(aVoid2 -> {
                                    Intent intent = new Intent(GameActivity.this, GameActivity.class);
                                    intent.putExtra("gameId", newGameId);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finish();
                                });
                            });
                } else {
                    Intent intent = new Intent(GameActivity.this, GameActivity.class);
                    intent.putExtra("gameId", newGameId);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            }

            Boolean disp = doc.getBoolean("winnerDisplayed");
            String winner = doc.getString("winnerUid");
            Long award = doc.getLong("award");
            if (Boolean.FALSE.equals(disp) && winner != null && award != null) {
                gameRef.update("winnerDisplayed", true);
                String text = "Winner - " + winner + "\n+ " + award + " chips";
                Toast.makeText(GameActivity.this, text, Toast.LENGTH_LONG).show();
            }
        });
    }

    private List<String> reorderPlayers(List<String> originalList, String currentUserId) {
        int size = originalList.size();
        int index = originalList.indexOf(currentUserId);
        if (index < 0) {
            return new ArrayList<>(originalList);
        }
        List<String> reordered = new ArrayList<>(size);
        for (int offset = 0; offset < size; offset++) {
            int i = (index + offset) % size;
            reordered.add(originalList.get(i));
        }
        return reordered;
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

                            if (foldedPlayers.contains(uid)) {
                                tv.setAlpha(0.4f);
                                betViews[index].setAlpha(0.4f);
                                chipViews[index].setAlpha(0.4f);
                            } else {
                                tv.setAlpha(1f);
                                betViews[index].setAlpha(1f);
                                chipViews[index].setAlpha(1f);
                            }

                            if (uid.equals(currentPlayerID)) {
                                tv.setTextColor(getResources().getColor(R.color.x, null));
                                tv.setTypeface(null, Typeface.BOLD);
                            } else {
                                tv.setTextColor(Color.BLACK);
                                tv.setTypeface(null, Typeface.NORMAL);
                            }
                        })
                        .addOnFailureListener(e -> tv.setText("(error)"));
            } else {
                tv.setText(".");
            }
        }
    }
}