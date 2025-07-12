package com.dsvl0.preferenseseditor;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.loadingindicator.LoadingIndicator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Editor extends AppCompatActivity {
    final float GUIDELINE_PERCENT_OPENED = 0.25f;
    final float GUIDELINE_PERCENT_CLOSED = 0.025f;
    ArrayList<String> searchType;
    String packageName, appName;
    Bitmap appIcon;
    List<String> sharedPrefsFiles = new ArrayList<>();
    RecyclerView WorkingList;
    XmlFileAdapter adapter;
    TextView PopupText;
    ConstraintLayout PopupLayout;
    SettingsAdapter settingsAdapter;
    String fileName = null;
    String fileContent = null;
    BottomNavigationView bottomNav;
    boolean SecondMenuOpened = false;
    ImageView appImg, blurImage;
    TextView EditorAppName, EditorAppPackage;

    List<SharedPrefsParser.Setting> settings;




    public List<String> getSharedPrefsFileNames() {
        List<String> sharedPrefsFiles = new ArrayList<>();
        String sharedPrefsPath = "/data/data/" + packageName + "/shared_prefs/";

        try {
            Process process = Runtime.getRuntime().exec(new String[] {"su", "-c", "ls " + sharedPrefsPath});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.endsWith(".xml")) {
                    sharedPrefsFiles.add(line);
                }
            }

            reader.close();
            process.waitFor();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return sharedPrefsFiles;
    }


    @SuppressLint("SetTextI18n")
    public void RefreshInAppSettings() {
        if (settings == null) {return;}
        // searchType = ["boolean", "string", "int", "float", "long", "set", "*"];
        long symbolsCounter = 0;
        List<SettingItem> data = new ArrayList<>();
        for (SharedPrefsParser.Setting setting : settings) {
            Log.d("searchType: ", String.valueOf(searchType));
            if (searchType.contains(setting.type) || searchType.contains("*") || searchType.isEmpty()) {
                if (setting.type.equals("string")){
                    symbolsCounter+=setting.value.toString().length();
                }
                data.add(new SettingItem(setting.settingName, setting.type, setting.value));
            }
        }
        if (symbolsCounter > 3000) {
            PopupLayout.setVisibility(View.VISIBLE);
            PopupText.setText(getString(R.string.TooLargeContent) + " (" + symbolsCounter + ")" );
        }
        final int delay = symbolsCounter > 3000 ? 1000 : 0;

        new Handler().postDelayed(() -> {
            settingsAdapter = new SettingsAdapter(data);
            WorkingList.setAdapter(settingsAdapter);
            ShowLoadingIndicator(false);
        }, delay);
    }

    public void ShowFilePreferences() {
        adapter.clearFiles();
        SwitchTopElement(true);
        fileContent = SharedPrefsReader.readSharedPrefs(packageName, fileName);

        try {
            settings = SharedPrefsParser.parseSharedPrefsXml(fileContent); } catch (Exception e) {
            Toast.makeText(this, "Failed to decode file", Toast.LENGTH_SHORT).show();
            SwitchTopElement(false);
            return;
        }

        RefreshInAppSettings();
    }

    public void ShowLoadingIndicator(boolean show) {
        View loadingIndicator = findViewById(R.id.LoadingIndicator);
        loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void SwitchTopElement(boolean blurred) {
        SecondMenuOpened = blurred;
        Guideline line = findViewById(R.id.guideline2);
        ValueAnimator animator;
        ShowLoadingIndicator(blurred);
        if (blurred) {
            WorkingList.setAdapter(null);
            EditorAppPackage.setText(fileName);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                blurImage.setRenderEffect(null);
                ValueAnimator blurAnimator = ValueAnimator.ofFloat(0f, 16f);

                blurAnimator.setDuration(300);
                blurAnimator.addUpdateListener(animation -> {
                    float blurRadius = (float) animation.getAnimatedValue();
                    RenderEffect blurEffect = RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP);
                    blurImage.setRenderEffect(blurEffect);
                });

                blurAnimator.start();
            }
            blurImage.setScaleX(1f);
            blurImage.setScaleY(1f);
            blurImage.setAlpha(0f);
            blurImage.animate()
                    .alpha(0.25f)
                    .scaleX(12f)
                    .scaleY(12f)
                    .setDuration(400)
                    .start();

            appImg.animate()
                    .alpha(0f)
                    .scaleX(2f)
                    .scaleY(2f)
                    .setDuration(400)
                    .start();


            animator = ValueAnimator.ofFloat(GUIDELINE_PERCENT_OPENED, GUIDELINE_PERCENT_CLOSED); animator.setDuration(400);
            bottomNav.setVisibility(View.VISIBLE);
        } else {

            CreateXmlAdapter();
            EditorAppPackage.setText(fileName);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                blurImage.setRenderEffect(null);
                ValueAnimator blurAnimator = ValueAnimator.ofFloat(16f, 0f);

                blurAnimator.setDuration(300);
                blurAnimator.addUpdateListener(animation -> {
                    float blurRadius = (float) animation.getAnimatedValue();
                    RenderEffect blurEffect = RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP);
                    blurImage.setRenderEffect(blurEffect);
                });
                blurAnimator.start();
            }

            appImg.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(400)
                    .start();
            blurImage.animate()
                    .alpha(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(400)
                    .start();
            bottomNav.setVisibility(View.GONE);
            EditorAppPackage.setText(packageName);
            animator = ValueAnimator.ofFloat(GUIDELINE_PERCENT_CLOSED, GUIDELINE_PERCENT_OPENED); animator.setDuration(400);

        }

        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) line.getLayoutParams(); params.guidePercent = value;
            line.setLayoutParams(params);
        }); animator.start();


    }





    public void UpdateSharedPrefs() {
        sharedPrefsFiles = getSharedPrefsFileNames();
        if (sharedPrefsFiles.isEmpty()) { Toast.makeText(this, "Root отсутствует или приложение ничего не сохранило", Toast.LENGTH_LONG).show();}

        for (int i = 0; i < sharedPrefsFiles.size(); i++) {
            adapter.addFile(sharedPrefsFiles.get(i));
        }

        ShowLoadingIndicator(false);
    }


    public void CreateXmlAdapter() {
        adapter = new XmlFileAdapter();
        WorkingList.setLayoutManager(new LinearLayoutManager(this));
        WorkingList.setAdapter(adapter);
        adapter.setOnItemClickListener(clickedText -> {
            fileName = clickedText;
            ShowFilePreferences();
        });
        UpdateSharedPrefs();
    }

    @SuppressLint({"MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_editor);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            return insets;
        }); ShowLoadingIndicator(true);
        WorkingList = findViewById(R.id.SettingsList);
        PopupLayout = findViewById(R.id.PopupLayout);
        PopupText = findViewById(R.id.PopupText);
        findViewById(R.id.ClosePopupImage).setOnClickListener(v -> PopupLayout.setVisibility(View.GONE));


        //Get Extra from this activity
        packageName = getIntent().getStringExtra("packageName");
        appName = getIntent().getStringExtra("appName");


        appImg = findViewById(R.id.appImg); blurImage = findViewById(R.id.blurAnimation);
        if (getIntent().getParcelableExtra("appIcon") != null) {
            appIcon = getIntent().getParcelableExtra("appIcon");
            appImg.setImageBitmap(appIcon);
            blurImage.setImageBitmap(appIcon);
        } else {
            appImg.setVisibility(View.GONE);
            blurImage.setVisibility(View.GONE);
        }

        EditorAppName = findViewById(R.id.EditorAppName); EditorAppName.setText(appName);
        EditorAppPackage = findViewById(R.id.EditorAppPackage); EditorAppPackage.setText(packageName);

        CreateXmlAdapter();
        bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setOnItemSelectedListener(item -> {
            searchType = new ArrayList<>();
            int id = item.getItemId();
            if (id == R.id.bools) {
                searchType.add("boolean");
            } else if (id == R.id.ints) {
                searchType.add("int");
                searchType.add("long");
                searchType.add("float");
                searchType.add("double");
            } else if (id == R.id.alls) {
                searchType.add("*");
            } else if (id == R.id.strings_types) {
                searchType.add("string");
            } else {
                searchType.add("set");
            }
            hideKeyboard(this);
            RefreshInAppSettings();
            return true;
        });
        bottomNav.setSelectedItemId(R.id.alls);
    }




        @Override
    protected void onDestroy() {
        super.onDestroy();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {
        if (SecondMenuOpened) {
            SecondMenuOpened = false;
            SwitchTopElement(false);
            PopupLayout.setVisibility(View.GONE);
            return;
        }

        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    public void hideKeyboard(Activity activity) {
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


}