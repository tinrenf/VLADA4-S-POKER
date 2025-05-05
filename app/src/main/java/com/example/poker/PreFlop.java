package com.example.poker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PreFlop {//Он не бесполезен, честно
    public static Map<String, List<Card>> deal(List<String> playerIds) {
        Deck deck = new Deck();
        deck.shuffle();

        Map<String, List<Card>> hands = new LinkedHashMap<>();
        for (String uid : playerIds) {
            List<Card> hand = new ArrayList<>();
            hand.add(deck.dealOne());
            hand.add(deck.dealOne());
            hands.put(uid, hand);
        }
        return hands;
    }
}
