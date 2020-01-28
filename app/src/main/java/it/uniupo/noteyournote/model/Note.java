package it.uniupo.noteyournote.model;

import com.google.firebase.firestore.Blob;

public class Note {

    private String id;
    private String title;
    private String description;
    private Blob image;
    private Blob audio;
    private String location;
    private double latitude;
    private double longitude;

    public Note() {}

    public Note(String id, String title, String description, Blob image, Blob audio, String location, double latitude, double longitude) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.image = image;
        this.audio = audio;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Blob getImage() {
        return image;
    }

    public Blob getAudio() {
        return audio;
    }

    public String getLocation() {
        return location;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
