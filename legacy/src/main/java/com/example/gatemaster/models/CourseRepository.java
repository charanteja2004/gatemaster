package com.example.gatemaster.models;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class CourseRepository {

    private static final String JSON_FILE = "courses.json";
    private static List<Course> cachedCourses;

    public static List<Course> getCourses(Context context) {
        if (cachedCourses != null) {
            return cachedCourses;
        }

        cachedCourses = new ArrayList<>();
        try {
            InputStream is = context.getAssets().open(JSON_FILE);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonString = new String(buffer, "UTF-8");

            JSONObject root = new JSONObject(jsonString);
            JSONArray coursesArray = root.getJSONArray("courses");

            for (int i = 0; i < coursesArray.length(); i++) {
                JSONObject courseObj = coursesArray.getJSONObject(i);
                String courseId = courseObj.getString("id");
                String courseTitle = courseObj.getString("title");

                JSONArray topicsArray = courseObj.getJSONArray("topics");
                List<Topic> topicsList = new ArrayList<>();

                for (int j = 0; j < topicsArray.length(); j++) {
                    JSONObject topicObj = topicsArray.getJSONObject(j);
                    topicsList.add(new Topic(
                            topicObj.getString("id"),
                            topicObj.getString("title"),
                            topicObj.getString("contentPath"),
                            topicObj.getString("pdfPath")));
                }

                cachedCourses.add(new Course(courseId, courseTitle, topicsList));
            }
        } catch (Exception e) {
            Log.e("CourseRepository", "Error reading courses.json", e);
        }

        return cachedCourses;
    }

    public static Course getCourseById(Context context, String courseId) {
        List<Course> allCourses = getCourses(context);
        for (Course course : allCourses) {
            if (course.getId().equals(courseId)) {
                return course;
            }
        }
        return null;
    }
}
