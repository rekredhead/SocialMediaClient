package com.example.socialmediaclient;

import java.util.Date;
import java.util.UUID;

public class Message {
    private UUID id;
    private String text;
    private String imageLocation;
    private Date datePosted;

    public Message() {
        this(UUID.randomUUID());
    }

    public Message(UUID id) {
        this.id = id;
        this.datePosted = new Date();
    }

    public UUID getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getImageLocation() {
        return imageLocation;
    }

    public void setImageLocation(String imageLocation) {
        this.imageLocation = imageLocation;
    }

    public Date getDatePosted() {
        return datePosted;
    }

    public void setDatePosted(Date datePosted) {
        this.datePosted = datePosted;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false; // o has to be non-null and is a Message class
        Message msg = (Message) o;
        return id.equals(msg.getId());
    }
}
