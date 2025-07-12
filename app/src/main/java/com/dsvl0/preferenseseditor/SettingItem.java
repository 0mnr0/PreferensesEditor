package com.dsvl0.preferenseseditor;

public class SettingItem {
    public String settingName;
    public String settingType; // например "boolean", "string"
    public Object value;

    public SettingItem(String settingName, String settingType, Object value) {
        this.settingName = settingName;
        this.settingType = settingType;
        this.value = value;
    }
}

