package com.example.poker;

import java.util.*;

public class Deck {
    private final List<Card> cards;
    public Deck() {
        cards = new ArrayList<>();
        for (int suit = 1; suit <= 4; suit++) {
            for (int rank = 2; rank <= 14; rank++) {
                cards.add(new Card(suit, rank));
            }
        }
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }
    public Card dealOne() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("Deck is empty");
        }
        return cards.remove(0);
    }
    public void Shuffle() {
        cards.clear();
        for (int suit = 1; suit <= 4; suit++) {
            for (int rank = 2; rank <= 14; rank++) {
                cards.add(new Card(suit, rank));
            }
        }
        shuffle();
    }
}