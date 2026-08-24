package com.example.gatemaster;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;

import com.example.cources_cse.Algorithems;
import com.example.cources_cse.Aptitude;
import com.example.cources_cse.DataStructures;
import com.example.cources_cse.DigitalLogics;
import com.example.cources_cse.EnggMath;
import com.example.cources_cse.MockTest;
import com.example.cources_cse.OperatingSystem;
import com.example.cources_cse.PreviousPapers;
import com.example.cources_cse.ShortNotes;
import com.example.cources_cse.TheoryOfComputation;

public class MainActivity extends AppCompatActivity {

    Intent intent;
    private GridView gridView;
    private Button btnCourses, btnMainSubject, btnRevision, btnMockTest;

    private String[] courses = { "Previous Papers", "Crash Course", "Question Bank", "Mock Test", "Short Notes",
            "Aptitude", "Mathematics", "Operating System", "Theory Of Computation" };
    private int[] courseImages = { R.drawable.gate, R.drawable.crashcouse, R.drawable.qb, R.drawable.mocktest,
            R.drawable.shortnotes, R.drawable.aptitude, R.drawable.mathematics, R.drawable.os, R.drawable.toc }; // Add
                                                                                                                 // your
                                                                                                                 // image
                                                                                                                 // resources
                                                                                                                 // here

    private String[] mainSubjects = { "Data Structure", "Algorithms", "Digital Logic", "Operating System",
            "Theory Of Computation", "DataBase", "Mathematics" };
    private int[] mainSubjectImages = { R.drawable.ds, R.drawable.icon, R.drawable.de, R.drawable.os, R.drawable.toc,
            R.drawable.db, R.drawable.mathematics }; // Add your image resources here

    private String[] revisions = { "Revision" };
    private int[] revisionImages = { R.drawable.shortnotes }; // Add your image resources here

    private String[] mockTests = { "Mock Test" };
    private int[] mockTestImages = { R.drawable.gate }; // Add your image resources here

    private String[] currentData;
    private int[] currentImages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gridView = findViewById(R.id.grid_view);
        btnCourses = findViewById(R.id.btn_courses);
        btnMainSubject = findViewById(R.id.btn_main_subject);
        btnRevision = findViewById(R.id.btn_revision);
        btnMockTest = findViewById(R.id.btn_mock_test);

        btnCourses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentData = courses;
                currentImages = courseImages;
                btnCourses.setTextColor(Color.BLACK);
                setGridAdapter(currentData, currentImages);
            }
        });

        btnMainSubject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentData = mainSubjects;
                currentImages = mainSubjectImages;
                btnMainSubject.setTextColor(Color.BLACK);
                setGridAdapter(currentData, currentImages);
            }
        });

        btnRevision.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentData = revisions;
                currentImages = revisionImages;
                btnRevision.setTextColor(Color.BLACK);
                setGridAdapter(currentData, currentImages);
            }
        });

        btnMockTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentData = mockTests;
                currentImages = mockTestImages;
                btnMockTest.setTextColor(Color.BLACK);
                setGridAdapter(currentData, currentImages);
            }
        });

        // Set initial data
        currentData = courses;
        currentImages = courseImages;
        setGridAdapter(currentData, currentImages);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = (String) parent.getItemAtPosition(position);
                Intent intent;

                if (item.equals("Mock Test")) {
                    intent = new Intent(MainActivity.this, MockTest.class);
                    startActivity(intent);
                    return;
                } else if (item.equals("Crash Course")) {
                    intent = new Intent(MainActivity.this, crashCourse.class);
                    startActivity(intent);
                    return;
                }

                String courseId = null;
                switch (item) {
                    case "Aptitude":
                        courseId = "aptitude";
                        break;
                    case "Previous Papers":
                        courseId = "previousPapers";
                        break;
                    case "Short Notes":
                        courseId = "shortnotes";
                        break;
                    case "Revision":
                        courseId = "shortnotes";
                        break;
                    case "Question Bank":
                        courseId = "aptitude";
                        break; // Temp fallback
                    case "Operating System":
                        courseId = "os";
                        break;
                    case "Theory Of Computation":
                        courseId = "toc";
                        break;
                    case "Data Structure":
                        courseId = "ds";
                        break;
                    case "Algorithms":
                        courseId = "algo";
                        break;
                    case "Digital Logic":
                        courseId = "dl";
                        break;
                    case "DataBase":
                        courseId = "dbms";
                        break;
                    default:
                        // For missing ones like Mathematics, maybe they don't have HTML assets. We can
                        // silently fail or toast.
                        break;
                }

                if (courseId != null) {
                    intent = new Intent(MainActivity.this, CourseDetailActivity.class);
                    intent.putExtra(CourseDetailActivity.EXTRA_COURSE_ID, courseId);
                    startActivity(intent);
                }
            }
        });
    }

    private void setGridAdapter(String[] data, int[] images) {
        CustomGridAdapter adapter = new CustomGridAdapter(this, data, images);
        gridView.setAdapter(adapter);
    }
    // @Override
    // protected void onCreate(Bundle savedInstanceState) {
    // super.onCreate(savedInstanceState);
    // setContentView(R.layout.activity_main);
    //
    // }

    public void cources_CSE(View view) {
        intent = new Intent(this, Cources_cse.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    public void cources_AI(View view) {
        intent = new Intent(this, Cources_ai.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

}