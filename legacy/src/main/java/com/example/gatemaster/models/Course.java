package com.example.gatemaster.models;

import java.io.Serializable;
import java.util.List;

public class Course implements Serializable {
    private String id;
    private String title;
    private List<Topic> topics;

    public Course(String id, String title, List<Topic> topics) {
        this.id = id;
        this.title = title;
        this.topics = topics;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public List<Topic> getTopics() {
        return topics;
    }
}
