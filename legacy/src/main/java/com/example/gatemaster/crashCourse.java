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
import com.example.crash.cc_Algorithems;
import com.example.crash.cc_Cao;
import com.example.crash.cc_CompilerDesign;
import com.example.crash.cc_ComputerNetworks;
import com.example.crash.cc_DataStructures;
import com.example.crash.cc_DatabaseManagement;
import com.example.crash.cc_DigitalLogics;
import com.example.crash.cc_EnggMath;
import com.example.crash.cc_OperatingSystem;
import com.example.crash.cc_TheoryOfComputation;

public class crashCourse extends AppCompatActivity {

    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash_course);
    }
    public void onItemClick(View view) {
        String tag = (String) view.getTag();
        // Depending on the tag, navigate to the corresponding activity
        switch (tag) {
            case "maths":
                startActivity(new Intent(this, cc_EnggMath.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
            case "os":
                startActivity(new Intent(this, cc_OperatingSystem.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
            case "toc":
                startActivity(new Intent(this, cc_TheoryOfComputation.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
            case "dbms":
                startActivity(new Intent(this, cc_DatabaseManagement.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
            case "cn":
                startActivity(new Intent(this, cc_ComputerNetworks.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
            case "ds":
                startActivity(new Intent(this, cc_DataStructures.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
            case "cd":
                startActivity(new Intent(this, cc_CompilerDesign.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
            case "Algo":
                startActivity(new Intent(this, cc_Algorithems.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
            case "dl":
                startActivity(new Intent(this, cc_DigitalLogics.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
            case "cao":
                startActivity(new Intent(this, cc_Cao.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;
            // Add cases for other items as needed
        }
    }
}