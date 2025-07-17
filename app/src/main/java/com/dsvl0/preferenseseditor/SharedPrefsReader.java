package com.dsvl0.preferenseseditor;

import android.annotation.SuppressLint;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class SharedPrefsReader {

    public static String readSharedPrefs(String packageName, String fileName) {
        try {
            @SuppressLint("SdCardPath") String path = "/data/data/" + packageName + "/shared_prefs/" + fileName;

            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "cat \"" + path + "\""});

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }

            reader.close();
            process.waitFor();

            if (result.length() == 0) {
                return "No data returned. File may be empty or root access is not granted.";
            }

            return result.toString();

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

}

