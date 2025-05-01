package com.example.poker;

import java.util.*;

public class PlayerInGame {
    static final int START_CHIPS = 900;
    private String playerId;
    private int chips;
    public List<Card> cards;
    // пока public тк мне кажется что удобнее работать с листом карт напрямую

    // конструкторы
    PlayerInGame(String playerId) {
        this.playerId = playerId;
        this.chips = START_CHIPS;
        cards = new ArrayList<Card>();
    }
    PlayerInGame() {
        this(null);
    }

    // геттеры сеттеры
    public String getPlayerId() {
        return playerId;
    }
    public int getChips() {
        return chips;
    }
    public void setChips(int new_chips) {
        this.chips = new_chips;
    }
}
