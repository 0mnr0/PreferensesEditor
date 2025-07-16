package com.dsvl0.preferenseseditor;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class Editor extends AppCompatActivity {
    final float GUIDELINE_PERCENT_OPENED = 0.25f;
    final float GUIDELINE_PERCENT_CLOSED = 0.025f;
    ArrayList<String> searchType = new ArrayList<>();
    String packageName, appName;
    Bitmap appIcon;
    List<String> sharedPrefsFiles = new ArrayList<>();
    RecyclerView WorkingList;
    XmlFileAdapter adapter;
    TextView PopupText;
    ConstraintLayout PopupLayout, main;
    SettingsAdapter settingsAdapter;
    String fileName = null;
    BottomNavigationView bottomNav;
    boolean SecondMenuOpened = false;
    ImageView appImg, blurImage;
    TextView EditorAppName, EditorAppPackage;
    private volatile int refreshTaskId = 0;
    List<SharedPrefsParser.Setting> settings;
    private FloatingActionButton fabMain, SaveFile, EditFullFile, CreateNewSetting;
    private boolean isFabMenuOpen = false;




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
        if (settings == null) {
            return;
        }


        if (SecondMenuOpened) {
            ShowLoadingIndicator(true);
        }

        final int currentTaskId = ++refreshTaskId; // Инкремент ID задачи

        new Thread(() -> {
            long symbolsCounter = 0;
            List<SettingItem> data = new ArrayList<>();

            // Фильтрация и подсчет в фоне
            for (SharedPrefsParser.Setting setting : settings) {
                if (searchType.contains(setting.type) ||
                        searchType.contains("*") ||
                        searchType.isEmpty()) {

                    if ("string".equals(setting.type)) {
                        symbolsCounter += setting.value.toString().length();
                    }
                    data.add(new SettingItem(setting.settingName, setting.type, setting.value));
                }
            }

            final long finalSymbolsCounter = symbolsCounter;
            final List<SettingItem> finalData = data;

            runOnUiThread(() -> {
                if (isDestroyed() || currentTaskId != refreshTaskId) {
                    return;
                }

                // Обновление UI
                if (finalSymbolsCounter > 3000) {
                    PopupLayout.setVisibility(View.VISIBLE);
                    PopupText.setText(getString(R.string.TooLargeContent) + " (" + finalSymbolsCounter + ")");
                } else {
                    PopupLayout.setVisibility(View.GONE);
                }

                settingsAdapter = new SettingsAdapter(finalData);
                WorkingList.setAdapter(settingsAdapter);
                ShowLoadingIndicator(false);
            });
        }).start();
    }


    public void ShowFilePreferences() {
        adapter.clearFiles();
        SwitchTopElement(true);
        ShowLoadingIndicator(true);

        new Thread(() -> {
            final String content = SharedPrefsReader.readSharedPrefs(packageName, fileName); // Returning shared_preferences.xml file content like a String
            final List<SharedPrefsParser.Setting> parsedSettings;

            try {
                parsedSettings = SharedPrefsParser.parseSharedPrefsXml(content);
                Log.d("parsedSettings:", parsedSettings.toString());
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Failed to decode file :/", Toast.LENGTH_SHORT).show();
                    Log.w("DecodeFileFailed", e);
                    SwitchTopElement(false);
                    ShowLoadingIndicator(false);
                });
                return;
            }

            runOnUiThread(() -> {
                settings = parsedSettings;
                RefreshInAppSettings(); // Запустить обновление с новыми данными
            });
        }).start();
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
        toggleFabMenu(true);
        hideFabMenu(!blurred);
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
        main = findViewById(R.id.main);
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
        fabMain = findViewById(R.id.fabMain);
        EditFullFile = findViewById(R.id.EditFullFile);
        CreateNewSetting = findViewById(R.id.CreateNewSetting);
        SaveFile = findViewById(R.id.SaveFile);
        fabMain.setOnClickListener(v -> toggleFabMenu(false));
        hideFabMenu(true);


        CreateXmlAdapter();
        bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setOnItemSelectedListener(item -> {
            ArrayList<String> newSearchType = new ArrayList<>();
            int id = item.getItemId();
            if (id == R.id.bools) {
                newSearchType.add("boolean");
            } else if (id == R.id.ints) {
                newSearchType.add("int");
                newSearchType.add("long");
                newSearchType.add("float");
                newSearchType.add("double");
            } else if (id == R.id.alls) {
                newSearchType.add("*");
            } else if (id == R.id.strings_types) {
                newSearchType.add("string");
            } else {
                newSearchType.add("set");
            }
            hideKeyboard(this);
            if (!searchType.equals(newSearchType)) {
                searchType = newSearchType;
                RefreshInAppSettings();
            }

            if (isFabMenuOpen) toggleFabMenu(false);
            return true;
        });
        bottomNav.setSelectedItemId(R.id.alls);




        EditFullFile.hide();
        EditFullFile.setOnClickListener(v -> {
            Intent intent = new Intent(this, FullFileEditor.class);
            intent.putExtra("packageName", packageName);
            intent.putExtra("appName", appName);
            intent.putExtra("workingFile", fileName);
            startActivity(intent);
        });

        SaveFile.setOnClickListener(v -> {
            final List<SettingItem> settings = settingsAdapter.ExportData();
            XmlCreator xmlCreator = new XmlCreator();
            for (SettingItem settingItem : settings) {
                if (settingItem.value.getClass() == ArrayList.class) {
                    settingItem.value = new HashSet<>((ArrayList<String>) settingItem.value);
                }
                Log.d("Save("+settingItem.settingName+")", String.valueOf(settingItem.value.getClass()));
                xmlCreator.add(settingItem.settingName, settingItem.settingType, settingItem.value);
            }
            String FinalXML = xmlCreator.getResult();

            Log.v("FinalXML", FinalXML);
            RootFile.save("/data/data/" + packageName + "/shared_prefs/" + fileName, FinalXML);
        });

        CreateNewSetting.setOnClickListener(v -> {
            LayoutInflater inflater = LayoutInflater.from(this);
            View dialogView = inflater.inflate(R.layout.dialog_with_spinner, null);
            Spinner spinner = dialogView.findViewById(R.id.dialogSpinner);
            TextInputEditText SetVarName = dialogView.findViewById(R.id.SetVarName);

            String[] options = {"String", "Boolean" ,"Int", "Float", "Long", "Set"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, options);
            spinner.setAdapter(adapter);


            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setView(dialogView)
                    .setTitle("Variable Creation")
                    .setCancelable(false)
                    .setPositiveButton("Далее", (dialog, which) -> {
                        String selected = spinner.getSelectedItem().toString().toLowerCase();
                        Object value = null;
                        switch (selected) {
                            case "string":
                                value = "";
                                break;
                            case "boolean":
                                value = false;
                                break;
                            case "integer":
                            case "long":
                            case "float":
                                value = 0;
                                break;
                            case "set":
                                value = new HashSet<String>();
                                break;
                        }
                        String varName = Objects.requireNonNull(SetVarName.getText()).toString();
                        settingsAdapter.AddSetting(varName, selected, value);
                    })
                    .setNegativeButton("Отмена", (dialog, which) -> {});

            AlertDialog dialog = builder.create();
            dialog.show();



        });

        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.listRefresh);
        swipeRefreshLayout.setColorSchemeResources(R.color.Material20, R.color.MaterialAdditional20);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (SecondMenuOpened) {
                ShowFilePreferences();
            } else {
                PopupLayout.setVisibility(View.GONE);
                CreateXmlAdapter();
            }
            swipeRefreshLayout.setRefreshing(false);
        });


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                boolean isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
                KeyBoardActions(isKeyboardVisible);
                return insets;
            });
        }
    }


    public void KeyBoardActions(boolean isKeyBoardOpened) {
        if (isKeyBoardOpened) {
            isFabMenuOpen = true;
            toggleFabMenu(false);
        }
        bottomNav.setVisibility(isKeyBoardOpened ? View.GONE : View.VISIBLE);
    }




    private void toggleFabMenu(boolean forceClose) {
        final int time = (forceClose ? 0 : 200);
        if (forceClose) {
            isFabMenuOpen = true;
        }
        if (!forceClose) {
            fabMain.animate().rotation(isFabMenuOpen ? 0f : 45f).setDuration(time).start();
        } else {
            fabMain.animate().rotation(0f).setDuration(time).start();
        }
        if (isFabMenuOpen) {
            EditFullFile.hide();
            CreateNewSetting.hide();
            SaveFile.hide();
        } else {
            EditFullFile.show();
            CreateNewSetting.show();
            SaveFile.show();
        }
        isFabMenuOpen = !isFabMenuOpen;
    }

    private void hideFabMenu(boolean status) {
        if (status) {
            fabMain.hide();
        } else {
            fabMain.show();
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }


    boolean WasSomethingFocused = false;
    private void clearFocusFromAll(ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof ViewGroup) {
                clearFocusFromAll((ViewGroup) child);
            } else {
                if (child.hasFocus()) {WasSomethingFocused = true;}
                child.clearFocus();
            }
        }
    }



    @Override
    public void onBackPressed() {

        if (!SecondMenuOpened) {
            WasSomethingFocused = false;
            clearFocusFromAll(main);
            if (WasSomethingFocused) {
                return;
            }
        }

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