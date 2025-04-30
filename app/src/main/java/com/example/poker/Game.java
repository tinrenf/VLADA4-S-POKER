package com.example.poker;

import com.google.firebase.Timestamp;

// Публичный класс-модель для Firestore
public class Game {
    private String id;
    private String creatorId;
    private Timestamp timestamp;

    // Пустой конструктор обязательно нужен для deserialization
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

    public Timestamp getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
