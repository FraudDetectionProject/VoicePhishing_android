package com.example.voicephising;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        setTitle("결과 확인");

        Intent get = getIntent();

        TextView ResultView = findViewById(R.id.showResult);
        TextView ProbView = findViewById(R.id.showProb);
        TextView StringView = findViewById(R.id.SpeachToText);
        Button getBack = findViewById(R.id.toBackbtn);
        Button checkText = findViewById(R.id.checkTextbtn);

        int fraudcode = get.getIntExtra("isFraud", 0);
        if(fraudcode==0) ResultView.setText("정상입니다.");
        else if(fraudcode==1) ResultView.setText("보이스피싱 위험이 있습니다.");

        String Prob = String.format("%.5f", get.getDoubleExtra("Prob", 0.0));
        ProbView.setText("신뢰도 : "+Prob);

        StringView.setText(get.getStringExtra("Text"));
        StringView.setMovementMethod(new ScrollingMovementMethod());

        checkText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkText.setVisibility(View.GONE);
                StringView.setVisibility(View.VISIBLE);
            }
        });

        getBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
