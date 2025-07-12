package com.dsvl0.preferenseseditor;

import android.graphics.Bitmap;

public class SystemAppInfo {
    private final String name;
    private final String packageName;
    private final Bitmap icon;

    public SystemAppInfo(String name, String packageName, Bitmap icon) {
        this.name = name;
        this.packageName = packageName;
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public String getPackageName() {
        return packageName;
    }

    public Bitmap getIcon() {
        return icon;
    }

}

