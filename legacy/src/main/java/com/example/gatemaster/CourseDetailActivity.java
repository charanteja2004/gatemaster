package com.example.gatemaster;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gatemaster.models.Course;
import com.example.gatemaster.models.CourseRepository;
import com.example.gatemaster.models.Topic;
import com.google.android.material.appbar.MaterialToolbar;

public class CourseDetailActivity extends AppCompatActivity {

    public static final String EXTRA_COURSE_ID = "course_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String courseId = getIntent().getStringExtra(EXTRA_COURSE_ID);
        Course course = CourseRepository.getCourseById(this, courseId);

        if (course != null) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(course.getTitle());
            }

            RecyclerView recyclerView = findViewById(R.id.recycler_topics);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));

            TopicAdapter adapter = new TopicAdapter(course.getTopics(), topic -> {
                // Launch dynamic content viewer for this topic
                Intent intent = new Intent(CourseDetailActivity.this, ContentViewerActivity.class);
                intent.putExtra(ContentViewerActivity.EXTRA_TOPIC, topic);
                intent.putExtra(ContentViewerActivity.EXTRA_COURSE_TITLE, course.getTitle());
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
            recyclerView.setAdapter(adapter);
        } else {
            Toast.makeText(this, "Course not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
