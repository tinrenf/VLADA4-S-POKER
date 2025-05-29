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
import com.google.firebase.firestore.FieldValue;

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
    private int round = 0;
    private String creatorID;
    private String currentPlayerID;
    private String lastRaise = creatorID;

    private TextView[] chipViews;
    private List<String> playersRaisedThisRound = new ArrayList<>();
    private List<String> joinAfterStart = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toast.makeText(this, "GameActivity onCreate", Toast.LENGTH_SHORT).show();
        Log.d("DEBUG", "GameActivity onCreate, gameId=" + getIntent().getStringExtra("gameId"));
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

        db.collection("games").document(gameId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                List<String> ids = (List<String>) documentSnapshot.get("playerIds");
                if (ids != null) {
                    playerIds = new ArrayList<>(ids);
                }
                Long bigBlindLong = documentSnapshot.getLong("bigBlind");
                if (bigBlindLong != null) {
                    big_blind = bigBlindLong.intValue();
                    small_blind = big_blind / 2;
                }
            }
        });

        startButton = findViewById(R.id.start_button);
        Button callButton = findViewById(R.id.button_call);
        Button raiseButton = findViewById(R.id.button_raise);
        Button foldButton = findViewById(R.id.button_fold);/**КНОПКИ**/

        loadGameDetails(gameId);

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
                        }

                        Map<String, List<Card>> holeCards = PreFlop.deal(playerIds);

                        // Показываем карты у игроков
                        for (int i = 0; i < playerIds.size() && i < holeCardViews1.length; i++) {
                            List<Card> hand = holeCards.get(playerIds.get(i));
                            holeCardViews1[i].setImageResource(getResources().getIdentifier(Card.toImage(hand.get(0).toString()), "drawable", getPackageName()));
                            holeCardViews2[i].setImageResource(getResources().getIdentifier(Card.toImage(hand.get(1).toString()), "drawable", getPackageName()));
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
                        for(int s: suits) {
                            for (int r = 2; r <= 14; r++) {
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
                                                chips.put(uid, 2525L);
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
                        updates.put("lastRaise", currentUID);
                        updates.put("playersRaisedThisRound", playersRaisedThisRound);
                        gameRef.update(updates);

                        updatePotView();
                        proceedToNextPlayer(false);
                    } else {
                        Toast.makeText(this, "Not enough chips to raise", Toast.LENGTH_SHORT).show();
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
                        proceedToNextPlayer(false);
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
                        proceedToNextPlayer(false);
                    } else {
                        Toast.makeText(this, "Not enough chips to call", Toast.LENGTH_SHORT).show();
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

    private long evaluateHandStrength(List<String> cards7) { //оценка силы комбинации
        List<Integer> ranks = new ArrayList<>();
        List<Character> suits = new ArrayList<>();

        for (String c : cards7) {
            if (c == null || c.length() < 2) continue;
            c = c.trim();
            String rankPart = c.substring(0, c.length() - 1);
            String suitChar = c.substring(c.length() - 1);

            int rank;
            switch (rankPart) {
                case "A":  rank = 14; break;
                case "K":  rank = 13; break;
                case "Q":  rank = 12; break;
                case "J":  rank = 11; break;
                default:
                    try {
                        rank = Integer.parseInt(rankPart);
                    } catch (NumberFormatException e) {
                        continue;
                    }
            }
            if (rank < 2 || rank > 14) continue;
            char suit = suitChar.charAt(0);
            if ("♠♥♦♣".indexOf(suit) == -1) continue;

            ranks.add(rank);
            suits.add(suit);
        }
        if (ranks.size() != 7) {
            throw new IllegalStateException(
                    "Получено " + ranks.size() + " карт: " + cards7);
        }
        Map<Integer, Integer> cnt = new HashMap<>();
        Map<Character, List<Integer>> bySuit = new HashMap<>();
        for (int i = 0; i < ranks.size(); i++) {
            int r = ranks.get(i);
            char s = suits.get(i);
            cnt.put(r, cnt.getOrDefault(r, 0) + 1);
            bySuit.computeIfAbsent(s, k -> new ArrayList<>()).add(r);
        }
        // 1) sf
        for (List<Integer> sameSuitRanks : bySuit.values()) {
            if (sameSuitRanks.size() >= 5) {
                long sf = findStraightScore(sameSuitRanks);
                if (sf > 0) {
                    return (8L << 20) | sf;
                }
            }
        }
        // 2)ранги кикеров
        List<Integer> uniq = new ArrayList<>(cnt.keySet());
        uniq.sort((a, b) -> {
            int c = cnt.get(b).compareTo(cnt.get(a));
            return c != 0 ? c : b.compareTo(a);
        });

        int four = -1, three = -1;
        List<Integer> pairs = new ArrayList<>();
        for (int r : uniq) {
            int f = cnt.get(r);
            if (f == 4) four = r;
            else if (f == 3) three = three < 0 ? r : three;
            else if (f == 2) pairs.add(r);
        }
        if (four >= 0) {
            //3)каре + кикер
            int kicker = 0;
            for (int rnk : uniq) {
                if (rnk != four) {
                    kicker = rnk;
                    break;
                }
            }
            return (7L<<20) | ((long)four<<16) | ((long)kicker<<12);
        }
        if (three >= 0 && !pairs.isEmpty()) {
            // 4)fh
            int pair = pairs.get(0);
            return (6L << 20) | ((long) three << 16) | ((long) pair << 12);
        }
        // 5) флеш
        for (List<Integer> sameSuitRanks : bySuit.values()) {
            if (sameSuitRanks.size() >= 5) {
                sameSuitRanks.sort(Comparator.reverseOrder());
                long score = 0;
                for (int i = 0; i < 5; i++) score |= ((long) sameSuitRanks.get(i) << (16 - 4 * i));
                return (5L << 20) | score;
            }
        }
        // 6) стрит
        long st = findStraightScore(ranks);
        if (st > 0) return (4L << 20) | st;
        // 7) сет + 2 кикера
        if (three >= 0) {
            List<Integer> kickers = new ArrayList<>();
            for (int r : uniq) if (r != three) kickers.add(r);
            return (3L << 20)
                    | ((long) three << 16)
                    | ((long) kickers.get(0) << 12)
                    | ((long) kickers.get(1) << 8);
        }
        // 8) 2 пары + кикер
        if (pairs.size() >= 2) {
            int hi = pairs.get(0), lo = pairs.get(1);
            int kicker = uniq.stream().filter(r -> r != hi && r != lo).findFirst().orElse(0);
            return (2L << 20)
                    | ((long) hi << 16)
                    | ((long) lo << 12)
                    | ((long) kicker << 8);
        }
        // 9) пара + 3 кикера
        if (pairs.size() == 1) {
            int pr = pairs.get(0);
            List<Integer> kickers = new ArrayList<>();
            for (int r : uniq) if (r != pr) kickers.add(r);
            return (1L << 20)
                    | ((long) pr << 16)
                    | ((long) kickers.get(0) << 12)
                    | ((long) kickers.get(1) << 8)
                    | ((long) kickers.get(2) << 4);
        }
        // 11) старшая карта
        List<Integer> hc = new ArrayList<>(uniq);
        hc.sort(Comparator.reverseOrder());
        long code = 0;
        for (int i = 0; i < 5; i++) code |= ((long) hc.get(i) << (16 - 4 * i));
        return (0L << 20) | code;
    }

    private long findStraightScore(List<Integer> ranks) {
        Set<Integer> u = new HashSet<>(ranks);
        //A‑2‑3‑4‑5
        if (u.contains(14)) u.add(1);
        List<Integer> ur = new ArrayList<>(u);
        Collections.sort(ur);
        int consec = 1, bestHigh = 0;
        for (int i = 1; i < ur.size(); i++) {
            if (ur.get(i) == ur.get(i - 1) + 1) {
                consec++;
                if (consec >= 5) bestHigh = ur.get(i);
            } else consec = 1;
        }
        if (bestHigh > 0) {
            long s = 0;
            for (int k = 0; k < 5; k++) {
                s |= ((long) (bestHigh - k) << (16 - 4 * k));
            }
            return s;
        }
        return 0;
    }

    private void findWinnerUid() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference gameRef = db.collection("games").document(gameId);

        gameRef.get().addOnSuccessListener(docSnapshot -> {
            if (!docSnapshot.exists()) return;

            List<String> communityCards = (List<String>) docSnapshot.get("communityCards");
            Map<String, List<String>> holeCards = (Map<String, List<String>>) docSnapshot.get("holeCards");
            String winner = creatorID;
            long best = -1;
            for (String uid : playerIds) {
                if (foldedPlayers.contains(uid)) continue;
                List<String> seven = new ArrayList<>();
                seven.addAll(holeCards.get(uid));
                seven.addAll(communityCards);
                long sc = evaluateHandStrength(seven);
                if (sc > best) {
                    best = sc;
                    winner = uid;
                }
            }
            onWinnerDetermined(winner);
        });
    }

    private void onWinnerDetermined(String winner) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference gameRef = db.collection("games").document(gameId);

        gameRef.get().addOnSuccessListener(docSnapshot -> {
            if (!docSnapshot.exists()) return;

            Map<String, Object> gameUpdates = new HashMap<>();

            Long pot = docSnapshot.getLong("pot");
            if (pot == null) pot = 0L;

            final Map<String, Long> chips = new HashMap<>();
            Map<String, Long> fromDb = (Map<String, Long>) docSnapshot.get("chips");
            if (fromDb != null) {
                chips.putAll(fromDb);
            }

            Long newWinnerMoney = pot;
            for (Map.Entry<String, Long> e : chips.entrySet()) {
                String uid = e.getKey();
                Long moneyLong = e.getValue();
                int money = moneyLong != null ? moneyLong.intValue() : 0;
                if (uid.equals(winner)) {
                    money += pot.intValue();
                    newWinnerMoney = money * 1L;
                }
                FirebaseFirestore.getInstance().collection("players")
                        .document(uid).update("money", money);
            }

            final int award = pot.intValue();

            db.collection("players").document(winner).get()
                    .addOnSuccessListener(doc -> {
                        String winnerName = doc.getString("name");
                        gameRef.update(
                                "winnerUid", winnerName,
                                "award", award,
                                "winnerDisplayed", false
                        );
                    });



            chips.put(winner, newWinnerMoney);
            pot = 0L;
            gameUpdates.put("pot", pot);
            gameUpdates.put("chips", chips);
            gameRef.update(gameUpdates);

            DocumentReference oldGameRef = db.collection("games").document(gameId);
            oldGameRef.get().addOnSuccessListener(doc -> {
                if (!doc.exists()) return;

                List<String> players = (List<String>) doc.get("playerIds");
                Map<String,Object> newGame = new HashMap<>();
                newGame.put("creatorID", creatorID);
                newGame.put("playerIds", players);
                newGame.put("maxPlayers", 5);
                newGame.put("status", "waiting");
                newGame.put("timestamp", FieldValue.serverTimestamp());
                newGame.put("chips", doc.get("chips"));
                newGame.put("bigBlind", big_blind);

                db.collection("games")
                        .add(newGame)
                        .addOnSuccessListener(newGameRef -> {
                            String newGameId = newGameRef.getId();
                            oldGameRef.update("newGameId", newGameId);
                        });
            });
        });
    }

    private void foldEndGame(String winnerID) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference gameRef = db.collection("games").document(gameId);

        gameRef.get().addOnSuccessListener(docSnapshot -> {
            if (!docSnapshot.exists()) return;

            Map<String, Object> gameUpdates = new HashMap<>();

            Long pot = docSnapshot.getLong("pot");
            if (pot == null) pot = 0L;

            Map<String, Long> chips = (Map<String, Long>) docSnapshot.get("chips");
            if (chips == null) chips = new HashMap<>();

            Map<String, Object> pb = (Map<String, Object>) docSnapshot.get("playerBets");
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
            oldGameRef.get().addOnSuccessListener(doc -> {
                if (!doc.exists()) return;

                List<String> players = (List<String>) doc.get("playerIds");
                Map<String,Object> newGame = new HashMap<>();
                newGame.put("creatorID", creatorID);
                newGame.put("playerIds", players);
                newGame.put("maxPlayers", 5);
                newGame.put("status", "waiting");
                newGame.put("timestamp", FieldValue.serverTimestamp());
                newGame.put("chips", doc.get("chips"));
                newGame.put("bigBlind", big_blind);

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
                findWinnerUid();
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

    private void proceedToNextPlayer(boolean fuckFold) {
        DocumentReference gameRef = db.collection("games").document(gameId);

        gameRef.get().addOnSuccessListener(docSnapshot -> {
            if (!docSnapshot.exists()) return;

            List<String> originalPlayerIds = (List<String>) docSnapshot.get("playerIds");
            if (originalPlayerIds == null || originalPlayerIds.isEmpty()) return;

            String currentPlayerID = docSnapshot.getString("currentPlayerID");
            String lastRaiseUid = docSnapshot.getString("lastRaise");

            if (currentPlayerID == null) return;

            int currentIndex = originalPlayerIds.indexOf(currentPlayerID);
            int nextIndex = (currentIndex + 1) % originalPlayerIds.size();

            List<String> foldedPlayers = (List<String>) docSnapshot.get("foldedPlayers");
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
                playerIds = reorderPlayers(playerIds, currentUID);
                displayPlayerNames();

                if (creatorID != null && creatorID.equals(currentUID) && (gameStarted == null || !gameStarted)) {
                    startButton.setEnabled(true);
                }
            } else {
                playerIds.clear();
            }

            if (docSnapshot.contains("holeCards")) {
                Map<String, List<String>> holeCardMap = (Map<String, List<String>>) docSnapshot.get("holeCards");

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

            String stage = docSnapshot.getString("stage");
            if (stage != null) {
                updateStage(stage);
            }

            Object d = docSnapshot.get("deck");
            if (d instanceof List) {
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
                String lastRaiseTM = docSnapshot.getString("lastRaise");
                if (lastRaiseTM != null) {
                    lastRaise = lastRaiseTM;
                }
            }

            if (docSnapshot.contains("chips")) {
                Map<String, Long> chipMap = (Map<String, Long>) docSnapshot.get("chips");

                for (int i = 0; i < playerIds.size() && i < chipViews.length; i++) {
                    String uid = playerIds.get(i);
                    long chips = chipMap.getOrDefault(uid, 0L);
                    chipViews[i].setText("Chips: " + chips);
                }
            }

            if (docSnapshot.contains("joinAfterStart")) {
                List<String> list = (List<String>) docSnapshot.get("foldedPlayers");
                if (list != null) {
                    joinAfterStart.clear();
                    joinAfterStart.addAll(list);
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

            String newGameId = docSnapshot.getString("newGameId");
            if (newGameId != null && !newGameId.isEmpty()) {
                gameRef.update("newGameId", FieldValue.delete());
                Intent intent = new Intent(GameActivity.this, GameActivity.class);
                intent.putExtra("gameId", newGameId);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }

            Boolean disp = docSnapshot.getBoolean("winnerDisplayed");
            String winner = docSnapshot.getString("winnerUid");
            Long award = docSnapshot.getLong("award");
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
                                tv.setAlpha(0.5f);
                                betViews[index].setAlpha(0.5f);
                                chipViews[index].setAlpha(0.5f);
                            } else {
                                tv.setAlpha(1f);
                                betViews[index].setAlpha(1f);
                                chipViews[index].setAlpha(1f);
                            }

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
                tv.setText(".");
            }
        }
    }
}