package com.example.poker;

import java.util.*;

public class Card implements Comparable<Card> {
    private int suit;
     // suit -- масть карты
     // 1 -- пики
     // 2 -- червы
     // 3 -- бубы
     // 4 -- крести(трефы)
    private int rank;
     // rank -- достоинство карты
     // 2-10 -- 2-10
     // валет -- 11
     // дама -- 12
     // король -- 13
     // туз -- 14

    // конструкторы
    Card(int suit, int rank) {
        this.suit = suit;
        this.rank = rank;
    }
    Card() {
        this(0, 0);
    }

    // геттеры
    public int getSuit() {
        return suit;
    }
    public int getRank() {
        return rank;
    }

    // компаратор(сравнивает только достоинства)
    public int compareTo(Card o) {
        return Integer.compare(rank, o.rank);
    }
}
