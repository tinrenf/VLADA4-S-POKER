package com.example.poker;

import java.util.*;

public class Deck {
    private final List<Card> cards;

    /**
     * Инициализирует и собирает стандартную 52-карточную колоду.
     */
    public Deck() {
        cards = new ArrayList<>();
        // масти 1–4, ранги 2–14
        for (int suit = 1; suit <= 4; suit++) {
            for (int rank = 2; rank <= 14; rank++) {
                cards.add(new Card(suit, rank));
            }
        }
    }

    /**
     * Перемешивает колоду случайным образом.
     */
    public void shuffle() {
        Collections.shuffle(cards);
    }

    /**
     * Раздать одну карту сверху колоды.
     * @return карта
     * @throws IllegalStateException если колода пуста
     */
    public Card dealOne() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("Колода пуста");
        }
        return cards.remove(0);
    }

    /**
     * @return количество оставшихся карт в колоде
     */
    public int remaining() {
        return cards.size();
    }

    /**
     * Сбрасывает колоду к полному состоянию и перемешивает её.
     */
    public void resetAndShuffle() {
        cards.clear();
        for (int suit = 1; suit <= 4; suit++) {
            for (int rank = 2; rank <= 14; rank++) {
                cards.add(new Card(suit, rank));
            }
        }
        shuffle();
    }
}