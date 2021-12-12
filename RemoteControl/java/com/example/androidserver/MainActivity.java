package com.example.androidserver;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import android.widget.Button;
import android.widget.TextView;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {
    TextView t1;
    TextView t2;
    TextView t3;
    Button but1;
    Button but2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 直接切换到NFC处理界面
        Intent intent = new Intent(MainActivity.this, ReadTagActivity.class);
        startActivity(intent);
    }
}