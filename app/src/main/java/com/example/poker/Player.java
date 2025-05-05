package com.example.poker;

import java.util.*;
public class Player {
    public static final int START_MONEY = 10000;
    private String name;
    private int money;
    private String gameId;

    public Player() {
        this("Unnamed", START_MONEY);
    }
    public Player(String name) {
        this(name, START_MONEY);
    }
    public Player(String name, int money) {
        this.name = name;
        this.money = money;
    }

    public String getName() { return name; }
    public int getMoney() { return money; }
    public String getGameId() { return gameId; }

    public void setName(String name) { this.name = name; }
    public void setMoney(int money) { this.money = money; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    // какая-нибудь хуета
}