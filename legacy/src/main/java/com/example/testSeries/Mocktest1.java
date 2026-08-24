package com.example.testSeries;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import android.content.SharedPreferences;

import com.example.gatemaster.R;


public class Mocktest1 extends AppCompatActivity {

    private TextView questionTextView;
    private TextView questionNo;
    private RadioGroup optionsRadioGroup;
    private List<Question> questionList;
    private int currentQuestionIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mocktest1);

        questionTextView = findViewById(R.id.questionText);
        questionNo = findViewById(R.id.questionNo);
        optionsRadioGroup = findViewById(R.id.optionsRadioGroup);
        Button nextButton = findViewById(R.id.nextButton);

        questionList = loadQuestionsFromAssets();

        if (questionList.isEmpty()) {
            Toast.makeText(this, "No questions found", Toast.LENGTH_LONG).show();
            return;
        }

        loadProgress();
        showNextQuestion();
//        Log.d("myT", questionList.toString());
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAnswer();

                if (currentQuestionIndex < questionList.size()) {
                    showNextQuestion();

                } else {
                    showResult();
                }
                saveProgress();
            }
        });
    }

    private List<Question> loadQuestionsFromAssets() {
        List<Question> questions = new ArrayList<>();
        String jsonStr = getIntent().getStringExtra("mock1");
        String jsonContent = loadJSONFromAsset(jsonStr);

        if (jsonStr == null) {
            return questions; // Return an empty list if JSON could not be loaded
        }

        try {
            JSONObject jsonObj = new JSONObject(jsonContent);
            JSONArray jsonArray = jsonObj.getJSONArray("questions");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String question = obj.getString("question");
                String question_number = obj.getString("question_number");
                JSONArray optionsArray = obj.getJSONArray("options");
                String[] options = new String[optionsArray.length()];
                for (int j = 0; j < optionsArray.length(); j++) {
                    options[j] = optionsArray.getString(j);
                }
                int correctAnswerIndex = obj.getInt("correct_option");
                questions.add(new Question(question_number,question, options, correctAnswerIndex));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return questions;
    }

    private String loadJSONFromAsset(String filename) {
        String json = null;
        try {
            InputStream is = getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    private void showNextQuestion() {
        optionsRadioGroup.clearCheck();
        Question currentQuestion = questionList.get(currentQuestionIndex);
        questionTextView.setText(currentQuestion.getQuestion());
        questionNo.setText(currentQuestion.question_number());

        optionsRadioGroup.removeAllViews();
        for (int i = 0; i < currentQuestion.getOptions().length; i++) {
            RadioButton radioButton = new RadioButton(this);
            radioButton.setText(currentQuestion.getOptions()[i]);
            radioButton.setId(i+1);
            optionsRadioGroup.addView(radioButton);
        }
    }

    private void checkAnswer() {
        int selectedOptionId = optionsRadioGroup.getCheckedRadioButtonId();
        if (selectedOptionId == -1) {
            Toast.makeText(this, "Please select an answer", Toast.LENGTH_SHORT).show();
        } else {

            Question currentQuestion = questionList.get(currentQuestionIndex);
            if (selectedOptionId == currentQuestion.getCorrectAnswerIndex()) {
                currentQuestionIndex++;
                // Correct answer selected
                Toast.makeText(this, "correct", Toast.LENGTH_LONG).show();
            } else {
                // Incorrect answer selected
                Toast.makeText(this, "incorrect", Toast.LENGTH_LONG).show();


            }
        }
    }

    private void showResult() {
        Toast.makeText(this, "You have completed the exam", Toast.LENGTH_LONG).show();
        clearProgress();
    }

    private void saveProgress() {
        SharedPreferences sharedPreferences = getSharedPreferences("ExamProgress", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("currentQuestionIndex", currentQuestionIndex);
        editor.apply();
    }

    private void loadProgress() {
        SharedPreferences sharedPreferences = getSharedPreferences("ExamProgress", MODE_PRIVATE);
        currentQuestionIndex = sharedPreferences.getInt("currentQuestionIndex", 0);
    }

    private void clearProgress() {
        SharedPreferences sharedPreferences = getSharedPreferences("ExamProgress", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
}
