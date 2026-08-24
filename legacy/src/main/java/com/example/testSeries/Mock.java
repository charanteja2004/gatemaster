package com.example.testSeries;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.gatemaster.R;
import com.example.notes.algorithms;

public class Mock extends AppCompatActivity {
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
    public  void goback(View view){
        onBackPressed();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mock);
    }
    Intent intent ;
    public void onItemClick(View view) {
        String tag = (String) view.getTag();
        // Depending on the tag, navigate to the corresponding activity
        switch (tag) {
            case "cse":
                startActivity(new Intent(this, algorithms.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
            case "m1":
                intent=new Intent(this, Mocktest1.class);
                intent.putExtra("mock1","mock1.json");
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
            case "m2":
                intent=new Intent(Mock.this, algorithms.class);
                intent.putExtra("algo","testseries/question.html");
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
            case "m3":
                intent=new Intent(Mock.this, Mocktest1.class);
                intent.putExtra("mock3",3);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
        }
    }
}