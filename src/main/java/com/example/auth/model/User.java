package com.example.auth.model;

public class User {
    private String id;
    private String email;
    private String name;
    private String picture;
    private boolean emailVerified;

    
    public User() {}

    
    public User(String id, String email, String name, String picture, boolean emailVerified) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.picture = picture;
        this.emailVerified = emailVerified;
    }

    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", picture='" + picture + '\'' +
                ", emailVerified=" + emailVerified +
                '}';
    }
}

