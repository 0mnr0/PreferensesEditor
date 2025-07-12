package com.dsvl0.preferenseseditor;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class SharedPrefsReader {

    public static String readSharedPrefs(String packageName, String fileName) {
        try {
            String path = "/data/data/" + packageName + "/shared_prefs/" + fileName;

            // Чтение файла напрямую через `su` и `cat`
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "cat \"" + path + "\""});

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }

            reader.close();
            process.waitFor();

            // Проверка: если пусто — возможно файл не прочитался
            if (result.length() == 0) {
                return "Файл пуст или не удалось прочитать. Возможно, root-запрос отклонён.";
            }

            return result.toString();

        } catch (Exception e) {
            return "Ошибка: " + e.getMessage();
        }
    }

}

