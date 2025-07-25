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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class Editor extends AppCompatActivity {
    SwipeRefreshLayout settingsRefreshLayout, xmlRefreshLayout;
    final float GUIDELINE_PERCENT_OPENED = 0.25f;
    final float GUIDELINE_PERCENT_CLOSED = 0.025f;
    ArrayList<String> searchType = new ArrayList<>();
    String packageName, appName;
    Bitmap appIcon;
    List<String> sharedPrefsFiles = new ArrayList<>();
    RecyclerView SettingsList, XmlFilesList;
    XmlFileAdapter XmlAdapter;
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
    private FloatingActionButton fabMain;
    private ExtendedFloatingActionButton SaveFile, EditFullFile, CreateNewSetting;
    private boolean isFabMenuOpen = false;

    private ActivityResultLauncher<Intent> launcher;



    public List<String> getSharedPrefsFileNames() {
        List<String> sharedPrefsFiles = new ArrayList<>();
        String sharedPrefsPath = "/data/data/" + packageName + "/shared_prefs/";
        Process process;
        BufferedReader reader;
        try {
            process = Runtime.getRuntime().exec(new String[]{"su", "-c", "ls " + sharedPrefsPath});
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            Process finalProcess = process;
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    finalProcess.destroy();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

            // Чтение вывода
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.endsWith(".xml")) sharedPrefsFiles.add(line);
            }
            process.waitFor();


        } catch (Exception e) {
            e.printStackTrace();
            return sharedPrefsFiles;
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

                if (finalSymbolsCounter > 3000) {
                    PopupLayout.setVisibility(View.VISIBLE);
                    PopupText.setText(getString(R.string.TooLargeContent) + " (" + finalSymbolsCounter + ")");
                } else {
                    PopupLayout.setVisibility(View.GONE);
                }

                Log.d("finalData", String.valueOf(finalData.size())); // не может быть == 0
                //SettingsList.setAdapter(null);
                settingsAdapter = new SettingsAdapter(finalData);
                SettingsList.setAdapter(settingsAdapter);
                ShowLoadingIndicator(false);
            });
        }).start();
    }


    public void ShowFilePreferences() {
        ShowLoadingIndicator(true);
        settingsAdapter = null;
        SwitchTopElement(true);

        new Thread(() -> {
            final String content = SharedPrefsReader.readSharedPrefs(packageName, fileName); // Returning shared_preferences.xml file content like a String
            final List<SharedPrefsParser.Setting> parsedSettings;

            try {
                parsedSettings = SharedPrefsParser.parseSharedPrefsXml(content);
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.PreferenceDecodeFailed, Toast.LENGTH_SHORT).show();
                    Log.w("DecodeFileFailed", e);
                    SwitchTopElement(false);
                    ShowLoadingIndicator(false);
                });
                return;
            }

            runOnUiThread(() -> {
                settings = parsedSettings;
                RefreshInAppSettings();
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
        settingsRefreshLayout.setVisibility(blurred ? View.VISIBLE : View.GONE);
        xmlRefreshLayout.setVisibility(blurred ? View.GONE : View.VISIBLE);
        SwitchBottomNavVisibility(blurred ? View.VISIBLE : View.GONE, 300);
        if (blurred) {
            SettingsList.setAdapter(null);
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
        findViewById(R.id.NoFilesWasFound).setVisibility(sharedPrefsFiles.isEmpty() ? View.VISIBLE : View.GONE);
        if (sharedPrefsFiles.isEmpty()) { ShowLoadingIndicator(false); return; }

        for (int i = 0; i < sharedPrefsFiles.size(); i++) {
            XmlAdapter.addFile(sharedPrefsFiles.get(i));
        }

        ShowLoadingIndicator(false);
    }


    public void CreateXmlAdapter() {
        XmlAdapter = new XmlFileAdapter();
        XmlFilesList.setLayoutManager(new LinearLayoutManager(this));
        XmlFilesList.setAdapter(XmlAdapter);
        XmlAdapter.setOnItemClickListener(clickedText -> {
            fileName = clickedText;
            ShowFilePreferences();
        });
        UpdateSharedPrefs();
    }

    @SuppressLint({"MissingInflatedId", "SdCardPath"})
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
        SettingsList = findViewById(R.id.SettingsList);
        SettingsList.setLayoutManager(new LinearLayoutManager(this));
        XmlFilesList = findViewById(R.id.XmlFilesList);
        PopupLayout = findViewById(R.id.PopupLayout);
        PopupText = findViewById(R.id.PopupText);
        findViewById(R.id.ClosePopupImage).setOnClickListener(v -> PopupLayout.setVisibility(View.GONE));

        //Get Extra from this activity
        packageName = getIntent().getStringExtra("packageName");
        appName = getIntent().getStringExtra("appName");


        try {
            appImg = findViewById(R.id.appImg);
            blurImage = findViewById(R.id.blurAnimation);
            if (getIntent().getParcelableExtra("appIcon") != null) {
                appIcon = getIntent().getParcelableExtra("appIcon");
                Glide.with(this).load(appIcon).circleCrop().into(appImg);
                blurImage.setImageBitmap(appIcon);
            } else {
                appImg.setVisibility(View.GONE);
                blurImage.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Toast.makeText(this, "App Icon is corrupted", Toast.LENGTH_SHORT).show();
        }

        EditorAppName = findViewById(R.id.EditorAppName); EditorAppName.setText(appName);
        EditorAppPackage = findViewById(R.id.EditorAppPackage); EditorAppPackage.setText(packageName);
        fabMain = findViewById(R.id.fabMain);
        EditFullFile = findViewById(R.id.EditFullFile); EditFullFile.hide();
        CreateNewSetting = findViewById(R.id.CreateNewSetting); CreateNewSetting.hide();
        SaveFile = findViewById(R.id.SaveFile); SaveFile.hide();
        fabMain.setOnClickListener(v -> toggleFabMenu(false));
        hideFabMenu(true);
        //every 1 sec

        settingsRefreshLayout = findViewById(R.id.listRefresh); settingsRefreshLayout.setColorSchemeResources(R.color.Material20, R.color.MaterialAdditional20);
        settingsRefreshLayout.setOnRefreshListener(() -> {
            ShowFilePreferences();
            settingsRefreshLayout.setRefreshing(false);
        });

        xmlRefreshLayout = findViewById(R.id.xmlRefresh); xmlRefreshLayout.setColorSchemeResources(R.color.Material20, R.color.MaterialAdditional20);
        xmlRefreshLayout.setOnRefreshListener(() -> {
            PopupLayout.setVisibility(View.GONE);
            CreateXmlAdapter();
            xmlRefreshLayout.setRefreshing(false);
        });


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


        launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            if (data.getBooleanExtra("fileChanged", false)) {
                                ShowFilePreferences();
                            }
                        }
                    }
                });


        EditFullFile.hide();
        EditFullFile.setOnClickListener(v -> {
            isFabMenuOpen = true; toggleFabMenu(false);
            Intent intent = new Intent(this, FullFileEditor.class);
            intent.putExtra("packageName", packageName);
            intent.putExtra("appName", appName);
            intent.putExtra("workingFile", fileName);
            launcher.launch(intent);
        });

        SaveFile.setOnClickListener(v -> {
            final List<SettingItem> settings = settingsAdapter.ExportData();
            XmlCreator xmlCreator = new XmlCreator();
            for (SettingItem settingItem : settings) {
                if (settingItem.value != null && settingItem.value.getClass() == ArrayList.class) {
                    settingItem.value = new HashSet<>((ArrayList<String>) settingItem.value);
                }
                xmlCreator.add(settingItem.settingName, settingItem.settingType, settingItem.value);
            }
            String FinalXML = xmlCreator.getResult();
            RootFile.save("/data/data/" + packageName + "/shared_prefs/" + fileName, FinalXML);
            Toast.makeText(this, R.string.ChangesWasSaved, Toast.LENGTH_SHORT).show();
            isFabMenuOpen = true; toggleFabMenu(false);
        });

        CreateNewSetting.setOnClickListener(v -> {

            isFabMenuOpen = true; toggleFabMenu(false);
            LayoutInflater inflater = LayoutInflater.from(this);
            View dialogView = inflater.inflate(R.layout.dialog_with_spinner, null);
            Spinner spinner = dialogView.findViewById(R.id.dialogSpinner);
            TextInputEditText SetVarName = dialogView.findViewById(R.id.SetVarName);

            String[] options = {"String", "Boolean" ,"Int", "Float", "Long", "Set"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, options);
            spinner.setAdapter(adapter);


            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setView(dialogView)
                    .setTitle(R.string.CreateNewOption)
                    .setCancelable(false)
                    .setPositiveButton(R.string.NextStep, (dialog, which) -> {
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
                    .setNegativeButton(R.string.Cancel, (dialog, which) -> {});

            AlertDialog dialog = builder.create();
            dialog.show();
        });


        bottomNav.post(() -> {
            SwitchBottomNavVisibility(View.GONE, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                    boolean isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
                    KeyBoardActions(isKeyboardVisible);
                    return insets;
                });
            }
        });


    }


    public void KeyBoardActions(boolean isKeyBoardOpened) {
        if (isKeyBoardOpened) {
            isFabMenuOpen = true;
            toggleFabMenu(false);
        }
        if (SecondMenuOpened) {
            SwitchBottomNavVisibility(isKeyBoardOpened ? View.GONE : View.VISIBLE, 200);
        } else {
            SwitchBottomNavVisibility(View.GONE, 200);
        }
    }

    public void SwitchBottomNavVisibility(int mode, int duration){
        final boolean opened = mode == 0;
        bottomNav.animate().translationY(opened ? 5 : bottomNav.getHeight()).setDuration(duration);

        final int movementBy = (opened ? 0 : -bottomNav.getHeight());
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bottomNav.getLayoutParams();
        int startMargin = params.bottomMargin;

        ValueAnimator animator = ValueAnimator.ofInt(startMargin, movementBy);
        animator.setDuration(duration);
        animator.addUpdateListener(animation -> {
            params.bottomMargin = (int) animation.getAnimatedValue();
            bottomNav.setLayoutParams(params);
        });
        animator.start();

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


    @Override
    public void onBackPressed() {
        View rootView = getWindow().getDecorView().findViewById(android.R.id.content);

        if (SecondMenuOpened) {
            View Element = getCurrentFocus();
            if (Element != null && !rootView.isFocused()) {
                rootView.setFocusableInTouchMode(true);
                rootView.requestFocus();
            } else {
                SecondMenuOpened = false;
                SwitchTopElement(false);
                PopupLayout.setVisibility(View.GONE);
            }
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