package com.example.myapplication.enity;

import java.time.LocalDateTime;

public class Work {
    public int id;
    public String title;

    public double duration;
    public String timestamp;

    public Work() {

    }

    public Work(String title, double duration, String timestamp) {
        this.title = title;
        this.duration = duration;
        this.timestamp = timestamp;

    }

    @Override
    public String toString() {
        return "Work{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", duration=" + duration +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

}