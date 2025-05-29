package com.example.poker;

import com.google.firebase.Timestamp;
import java.util.*;

public class Game { //Эта штука для firebase, когда в начале создаем игру
    private String id;
    private String creatorID;
    private List<String> playerIds;
    private int maxPlayers;
    private Timestamp timestamp;
    private String name;
    private long bigBlind;

    public Game(String name, String id, String creatorID, List<String> playerIds, int maxPlayers, Timestamp timestamp) {
        this.id = id;
        this.creatorID = creatorID;
        this.playerIds = playerIds;
        this.maxPlayers = maxPlayers;
        this.timestamp = timestamp;
        this.name = name;
    }

    public Game() { }

    public long getBigBlind() {
        return bigBlind;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getcreatorID() {
        return creatorID;
    }

    public void setcreatorID(String creatorID) {
        this.creatorID = creatorID;
    }

    public List<String> getPlayerIds() {
        return playerIds;
    }

    public void setPlayerIds(List<String> playerIds) {
        this.playerIds = playerIds;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }
}
