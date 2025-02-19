package com.example.freshtrack;

public class FirebaseModel {
    private String title;
    private String content;

    // Default constructor required for Firebase
    public FirebaseModel() {
    }

    // Parameterized constructor to initialize title and content
    public FirebaseModel(String title, String content) {
        this.title = title;
        this.content = content;
    }

    // Getter method for title
    public String getTitle() {
        return title;
    }

    // Setter method for title
    public void setTitle(String title) {
        this.title = title;
    }

    // Getter method for content
    public String getContent() {
        return content;
    }

    // Setter method for content
    public void setContent(String content) {
        this.content = content;
    }
}
