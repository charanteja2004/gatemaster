package com.example.testSeries;
public class Question {
    private final String question;
    private final String question_number;
    private final String[] options;
    private final int correctAnswerIndex;


    public Question(String question_number,String question, String[] options, int correctAnswerIndex) {
        this.question_number = question_number;
        this.question = question;
        this.options = options;
        this.correctAnswerIndex = correctAnswerIndex;
//        this.marks = marks;
    }

    public String getQuestion() {
        return question;
    }
    public String question_number() {
        return question_number;
    }

    public String[] getOptions() {
        return options;
    }

    public int getCorrectAnswerIndex() {
        return correctAnswerIndex;
    }

}
