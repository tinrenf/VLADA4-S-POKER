package com.example.poker;

import com.google.firebase.Timestamp;
import java.util.*;

public class Game { //Эта штука для датабазы firebase
    private String id;
    private String creatorId;
    private List<String> playerIds;
    private int maxPlayers;
    private Timestamp timestamp;

    public Game(String id, String creatorId, List<String> playerIds, int maxPlayers, Timestamp timestamp) {
        this.id = id;
        this.creatorId = creatorId;
        this.playerIds = playerIds;
        this.maxPlayers = maxPlayers;
        this.timestamp = timestamp;
    }

    public Game() { }

    // Геттеры и сеттеры
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
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

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
