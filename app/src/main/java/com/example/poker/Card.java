package com.example.poker;

import java.util.*;

public class Card implements Comparable<Card> {
    // это надо чтобы текстуры приделывать
    static public String toImage(String s) {
        if (s.equals("🂠")) {
            return "card_back";
        }
        String p = "c", v = "2";
        if (s.length() == 3) {
            p += "10";
            v = "";
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
                    v = "";
                    break;
                default:
                    p += s.charAt(0);
                    v = "";
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
        return p + v;
    }
    private int suit;
    // 1 -- пики
    // 2 -- червы
    // 3 -- бубы
    // 4 -- крести
    // Но это все если храним как адекватные люди
    private int rank;
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
                break;  // крести
            default:
                s = " ";
        }
        return r + s;
    }

    Card(int suit, int rank) {
        this.suit = suit;
        this.rank = rank;
    }

    Card() {
        this(0, 0);
    }

    public int compareTo(Card o) {
        return Integer.compare(rank, o.rank);
    }
}
