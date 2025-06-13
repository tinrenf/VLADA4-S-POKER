package com.example.poker;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

public class GameResult {
        private final String gameId;
        private final String creatorID;
        private final List<String> playerIds;
        private final List<String> foldedPlayers;
        private final String gameName;
        private final int pot;
        public GameResult(String gameId, String creatorID, List<String> playerIds, List<String> foldedPlayers, String gameName, int pot) {
            this.gameId = gameId;
            this.creatorID = creatorID;
            this.playerIds = playerIds;
            this.foldedPlayers = foldedPlayers;
            this.gameName = gameName;
            this.pot = pot;
        }
    public void findWinnerUid() {
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

    private void onWinnerDetermined(String winner) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference gameRef = db.collection("games").document(gameId);

        gameRef.get().addOnSuccessListener(docSnapshot -> {
            if (!docSnapshot.exists()) return;

            Map<String, Object> gameUpdates = new HashMap<>();

            final Map<String, Long> chips = new HashMap<>();
            Map<String, Long> fromDb = (Map<String, Long>) docSnapshot.get("chips");
            if (fromDb != null) {
                chips.putAll(fromDb);
            }

            int newWinnerMoney = pot;
            for (Map.Entry<String, Long> e : chips.entrySet()) {
                String uid = e.getKey();
                Long moneyLong = e.getValue();
                int money = moneyLong != null ? moneyLong.intValue() : 0;
                if (uid.equals(winner)) {
                    newWinnerMoney += money;
                    money = newWinnerMoney;
                }
                FirebaseFirestore.getInstance().collection("players")
                        .document(uid).update("money", money);
            }

            final int award = pot;

            db.collection("players").document(winner).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String winnerName = doc.getString("name");
                            Map<String, Object> winnerEntry = new HashMap<>();
                            winnerEntry.put("winnerUid", winnerName);
                            winnerEntry.put("award", award);
                            gameRef.update("winners", FieldValue.arrayUnion(winnerEntry));
                            gameRef.update("winnerDisplayed", false);
                        }
                    });


            chips.put(winner, 1L * newWinnerMoney);
            gameUpdates.put("chips", chips);
            gameRef.update(gameUpdates);
        });
    }
}
