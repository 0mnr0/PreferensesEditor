package com.dsvl0.preferenseseditor;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.loadingindicator.LoadingIndicator;

public class FullFileEditor extends AppCompatActivity {
    String packageName;
    String appName;
    String fileName;
    TextView FileWorkingOn, AppFullEditName;
    LoadingIndicator LoadingIndicator;
    ImageView SaveAction;
    EditText EditTextFile;

    public void LoadIndicator(boolean show) {
        EditTextFile.setEnabled(!show);
        LoadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_full_file_editor);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        LoadingIndicator = findViewById(R.id.LoadIndicator);
        FileWorkingOn = findViewById(R.id.FileWorkingOn);
        AppFullEditName = findViewById(R.id.AppFullEditName);
        EditTextFile = findViewById(R.id.EditTextFile);
        SaveAction = findViewById(R.id.SaveAction);

        Intent intent = getIntent();
        packageName = intent.getStringExtra("packageName");
        appName = intent.getStringExtra("appName");
        fileName = intent.getStringExtra("workingFile");
        FileWorkingOn.setText(fileName);
        AppFullEditName.setText(appName);

        EditTextFile.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                EditTextFile.clearFocus();
            }
            return false;
        });




        LoadIndicator(true);
        new Thread(() -> {
            String content = RootFile.get("/data/data/" + packageName + "/shared_prefs/" + fileName);
            runOnUiThread(() -> {
                if (content == null) {return;}
                EditTextFile.setText(content);
                EditTextFile.setHorizontallyScrolling(true);
                EditTextFile.setMovementMethod(new ScrollingMovementMethod());
                LoadIndicator(false);
            });
        }).start();



        SaveAction.setOnClickListener(v -> {
            LoadIndicator(true);
            new Thread(() -> {
                RootFile.save("/data/data/" + packageName + "/shared_prefs/" + fileName, EditTextFile.getText().toString());
                runOnUiThread(() -> {
                    Toast.makeText(FullFileEditor.this, "Сохранено!", Toast.LENGTH_SHORT).show();
                    LoadIndicator(false);
                    finish();
                });
            }).start();
        });

    }


    @Override
    public void onBackPressed() {
        if (EditTextFile.hasFocus()) {
            EditTextFile.clearFocus();
            return;
        }
        super.onBackPressed();
    }
}