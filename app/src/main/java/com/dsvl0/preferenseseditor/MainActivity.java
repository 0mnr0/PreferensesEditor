package com.dsvl0.preferenseseditor;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.RoundedCorner;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.android.material.color.DynamicColors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class MainActivity extends AppCompatActivity {
    List<SystemAppInfo> apps = new ArrayList<>();
    ExecutorService executor = Executors.newSingleThreadExecutor();
    private float lastX = -1;
    private float lastY = -1;
    CardView AnimationElement;
    RecyclerView appList;
    ConstraintLayout MainContent;
    boolean isAnimating = false;
    boolean isRefreshing = false;
    SearchView searchView;
    String PackageFilter = "";

    AppCardAdapter adapter = new AppCardAdapter(this);



    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }


    public static List<SystemAppInfo> getSystemAppsWithRoot(Context context, int mode) {
        // MODES:
        // 1 - All Packages (User + System)
        // 2 - Only System
        // 3 - Only User
        List<SystemAppInfo> result = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo appInfo : apps) {
            boolean isSystemApp = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;

            if (
                    (mode == 1) || // All
                            (mode == 2 && isSystemApp) || // Only System
                            (mode == 3 && !isSystemApp) // Only User
            ) {
                String label = pm.getApplicationLabel(appInfo).toString();
                Drawable iconDrawable = pm.getApplicationIcon(appInfo);
                Bitmap icon = drawableToBitmap(iconDrawable);

                result.add(new SystemAppInfo(label, appInfo.packageName, icon));
            }
        }

        Collections.sort(result, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
        return result;
    }


    public void ShowLoadingProgress(Boolean state) {
        View LoadingElement = findViewById(R.id.LoadindIndicatorMain);
        LoadingElement.setVisibility(state ? ConstraintLayout.VISIBLE : ConstraintLayout.GONE);
        searchView.setEnabled(!state);
    }

    private void BuildAppList() {

        adapter.clearApps();
        appList.setAdapter(adapter);
        appList.setLayoutManager(new LinearLayoutManager(this));

        for (SystemAppInfo app : apps) {
            if (!app.getName().contains(PackageFilter)) {
                continue;
            }
            adapter.addApp(app);
        }


        adapter.setOnAppClickListener(appCard -> {
            if (isAnimating) { return; }
            isAnimating = true;
            AnimationElement.setVisibility(View.VISIBLE);
            AnimationElement.setAlpha(0.5f);
            AnimationElement.setScaleX(0.1f);
            AnimationElement.setScaleY(0.1f);
            AnimationElement.setCardBackgroundColor(ContextCompat.getColor(this, R.color.Material40));

            AnimationElement.post(() -> {
                int width = AnimationElement.getWidth();
                int height = AnimationElement.getHeight();
                float scale = (((float) getResources().getDisplayMetrics().widthPixels / width) + ((float) getResources().getDisplayMetrics().heightPixels / height) ) * 1.5f;
                float targetX = lastX - width / 2f;
                float targetY = lastY - height / 2f;

                AnimationElement.setX(targetX);
                AnimationElement.setY(targetY);

                AnimationElement.animate()
                        .alpha(1f)
                        .scaleX(scale)
                        .scaleY(scale)
                        .setDuration(400)
                        .start();
            });

            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), ContextCompat.getColor(this, R.color.Material40), ContextCompat.getColor(this, R.color.Material20));
            colorAnimation.setDuration(300);

            colorAnimation.addUpdateListener(animator -> {
                int animatedColor = (int) animator.getAnimatedValue();
                AnimationElement.setCardBackgroundColor(animatedColor);
            });

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ValueAnimator blurAnimator = ValueAnimator.ofFloat(0f, 20f);
                blurAnimator.setDuration(300);

                blurAnimator.addUpdateListener(animation -> {
                    float blurRadius = (float) animation.getAnimatedValue();
                    RenderEffect blurEffect = RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP);
                    MainContent.setRenderEffect(blurEffect);
                });

                blurAnimator.start();
            }

            colorAnimation.start();
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(MainActivity.this, Editor.class);
                intent.putExtra("packageName", appCard.getPackageName());
                intent.putExtra("appName", appCard.getName());
                try {
                    intent.putExtra("appIcon", appCard.getIcon());
                    startActivity(intent);
                } catch (Exception ignored) {
                    intent.removeExtra("appIcon");
                    startActivity(intent);
                }

                searchView.clearFocus();
                isAnimating = false;
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }, 350);
        });
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.ConstraintLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            return insets;
        });
        appList = findViewById(R.id.appList);
        MainContent = findViewById(R.id.ConstraintLayout);
        searchView = findViewById(R.id.search_view);
        ImageView NewAppUpdate = findViewById(R.id.NewAppUpdate);
        NewAppUpdate.setOnClickListener(v -> {
            try{
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/0mnr0/PreferensesEditor/releases"));
                startActivity(browserIntent);
            } catch (Exception ignored) {
                Toast.makeText(this, "Browser not found (github.com/0mnr0/PreferensesEditor)", Toast.LENGTH_SHORT).show();
            }
        });

        ShowLoadingProgress(true);
        new Handler().postDelayed(() -> executor.execute(() -> {
            isRefreshing = true;
            List<SystemAppInfo> fetchedApps = getSystemAppsWithRoot(this, 3);
            runOnUiThread(() -> {
                apps = fetchedApps;
                BuildAppList();
                ShowLoadingProgress(false);
                isRefreshing = false;
            });
        }), 0);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                PackageFilter = newText;
                BuildAppList();
                return false;
            }
        });

        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setColorSchemeResources(R.color.Material20, R.color.MaterialAdditional20);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (isRefreshing) {swipeRefreshLayout.setRefreshing(false); return;}
            isRefreshing = true;
            adapter.clearApps();
            ShowLoadingProgress(true);
            swipeRefreshLayout.setRefreshing(false);
            executor.execute(() -> {
                List<SystemAppInfo> fetchedApps = getSystemAppsWithRoot(this, 3);
                runOnUiThread(() -> new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    apps = fetchedApps;
                    BuildAppList();
                    ShowLoadingProgress(false);
                    isRefreshing = false;
                }, 100));
            });
        });


        AnimationElement = findViewById(R.id.AnimationElement);

        try {
            VersionFetcher fetcher = new VersionFetcher();
            fetcher.fetchVersion("https://raw.githubusercontent.com/0mnr0/PreferensesEditor/refs/heads/main/app/sampledata/actual.version.info", new VersionFetcher.VersionCallback() {
                @Override
                public void onResult(String result) {
                    try {
                        String currentVersion = MainActivity.this.getPackageManager().getPackageInfo(MainActivity.this.getPackageName(), 0).versionName;
                        if (result.equalsIgnoreCase(currentVersion)) return;

                        NewAppUpdate.setVisibility(View.VISIBLE);
                        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
                        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) searchView.getLayoutParams();
                        params.setMargins(0, 0, margin, 0);
                        searchView.setLayoutParams(params);

                        final boolean[] avatarTime = {false};
                        final Handler handler = new Handler();
                        Runnable task = new Runnable() {
                            @Override
                            public void run() {
                                NewAppUpdate.animate().alpha(0f).setDuration(200).start();
                                new Handler().postDelayed(() -> {
                                    Glide.with(MainActivity.this).load(avatarTime[0] ? "https://avatars.githubusercontent.com/u/99125366" : R.drawable.app_update).circleCrop().into(NewAppUpdate);
                                    final int paddingInDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
                                    final int padding = (avatarTime[0] ? 0 : paddingInDp);
                                    NewAppUpdate.setPadding(padding, padding, padding, padding);
                                    new Handler().postDelayed(() -> NewAppUpdate.animate().alpha(1f).setDuration(200).start(), 200);
                                    avatarTime[0] = !avatarTime[0];
                                    handler.postDelayed(this, 3200);
                                }, 200);
                            }
                        };

                        handler.post(task);
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.e("VersionChecker", e.toString());
                        Toast.makeText(MainActivity.this, "Failed to get app version :(", Toast.LENGTH_SHORT).show();
                    }

                }


                @Override
                public void onError(Exception e) {
                    Log.e("Version", e.toString());
                }
            });
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Failed to execute app version check :(", Toast.LENGTH_SHORT).show();
        }

    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            lastX = ev.getX();
            lastY = ev.getY();
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    // on back pressed
    public void onBackPressed() {
        if (searchView.hasFocus()) {
            searchView.clearFocus();
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        AnimationElement.setVisibility(View.GONE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MainContent.setRenderEffect(null);
        }
    }
}