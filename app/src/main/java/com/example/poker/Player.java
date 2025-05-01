package com.example.poker;

import java.util.*;
public class Player {
    static final int START_MONEY = 1488; // базовое количество денег, выдаваемых игроку, как только он зарегестрировался
    private String name;
    private int money;

    // конструкторы
    Player(String name, int money) {
        this.name = name;
        this.money = money;
    }
    Player() {
        this("Unnamed", START_MONEY);
    }

    Player(String name) {
        this(name, START_MONEY);
    }

    // геттеры сеттеры
    public String getName() {
        return name;
    }
    public int getMoney() {
        return money;
    }

    public void setMoney(int new_money) {
        this.money = new_money;
    }

    // какая-нибудь хуета
}
