package com.example.gatemaster;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.cources_cse.Algorithems;
import com.example.cources_cse.Aptitude;
import com.example.cources_cse.Cao;
import com.example.cources_cse.CompilerDesign;
import com.example.cources_cse.ComputerNetworks;
import com.example.cources_cse.DataStructures;
import com.example.cources_cse.DatabaseManagement;
import com.example.cources_cse.DigitalLogics;
import com.example.cources_cse.EnggMath;
import com.example.cources_cse.MockTest;
import com.example.cources_cse.OperatingSystem;
import com.example.cources_cse.ShortNotes;
import com.example.cources_cse.TheoryOfComputation;

public class Cources_cse extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cources);
    }
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
    public void onItemClick(View view) {
        String tag = (String) view.getTag();
        // Depending on the tag, navigate to the corresponding activity
        switch (tag) {
            case "aptitude":
                startActivity(new Intent(this, Aptitude.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
            case "maths":
                startActivity(new Intent(this, EnggMath.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
            case "os":
                startActivity(new Intent(this, OperatingSystem.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
            case "toc":
                startActivity(new Intent(this, TheoryOfComputation.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
            case "dbms":
                startActivity(new Intent(this, DatabaseManagement.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
            case "cn":
                startActivity(new Intent(this, ComputerNetworks.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
            case "ds":
                startActivity(new Intent(this, DataStructures.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
            case "cd":
                startActivity(new Intent(this, CompilerDesign.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
            case "Algo":
                startActivity(new Intent(this, Algorithems.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
            case "dl":
                startActivity(new Intent(this, DigitalLogics.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
            case "Shortnotes":
                startActivity(new Intent(this, ShortNotes.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
            case "Mocktest":
                startActivity(new Intent(this, MockTest.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
            case "cao":
                startActivity(new Intent(this, Cao.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
            // Add cases for other items as needed
        }
    }
}