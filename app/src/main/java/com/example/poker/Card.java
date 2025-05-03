package com.example.poker;

import java.util.*;

public class Card implements Comparable<Card> {
    static public String toImage(String s) {
        String p = "c";
        if (s.length() == 3) {
            p += "10";
        } else {
            switch (s.charAt(0)) {
                case 'J':
                    p += "jack";
                    break;
                case 'Q':
                    p += "queen";
                    break;
                case 'K':
                    p += "king";
                    break;
                case 'A':
                    p += "ace";
                    break;
                default:
                    p += s.charAt(0);
            }
        }
        p += "_of_";
        switch (s.charAt(s.length() == 3 ? 2 : 1)) {
            case '♠':
                p += "spades";
                break;
            case '♥':
                p += "hearts";
                break;
            case '♦':
                p += "diamonds";
                break;
            case '♣':
                p += "clubs";
                break;
        }
        return p;
    }
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

    public String toString() {
        String r;
        switch (rank) {
            case 11:
                r = "J";
                break;
            case 12:
                r = "Q";
                break;
            case 13:
                r = "K";
                break;
            case 14:
                r = "A";
                break;
            default:
                r = String.valueOf(rank);
        }
        String s;
        switch (suit) {
            case 1:
                s = "♠";
                break;  // пики
            case 2:
                s = "♥";
                break;  // червы
            case 3:
                s = "♦";
                break;  // бубны
            case 4:
                s = "♣";
                break;  // трефы
            default:
                s = "?";
        }
        return r + s;
    }

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
