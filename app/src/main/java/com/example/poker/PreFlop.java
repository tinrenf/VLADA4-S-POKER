package com.example.poker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Класс для выполнения фазы pre-flop: тасовка колоды и раздача по 2 карты каждому игроку.
 */
public class PreFlop {
    /**
     * Раздать по 2 карты каждому игроку.
     * @param playerIds список UID игроков (в порядке позиций за столом)
     * @return карта каждому UID: список из двух карт
     */
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
