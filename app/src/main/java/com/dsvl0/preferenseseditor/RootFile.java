package com.dsvl0.preferenseseditor;

import java.io.*;

public class RootFile {
    public static String get(String path) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            InputStream is = process.getInputStream();

            os.writeBytes("cat \"" + path + "\"\n");
            os.writeBytes("exit\n");
            os.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }

            process.waitFor();
            return output.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean save(String path, String content) {
        try {
            File temp = File.createTempFile("rootsave", ".tmp");
            FileWriter writer = new FileWriter(temp);
            writer.write(content);
            writer.close();

            String cmd = "cp \"" + temp.getAbsolutePath() + "\" \"" + path + "\"\n" +
                    "chmod 644 \"" + path + "\"\n";

            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(cmd);
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();

            temp.delete();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean exists(String path) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            InputStream is = process.getInputStream();

            // Если файл существует, echo вернёт 1
            os.writeBytes("[ -f \"" + path + "\" ] && echo 1 || echo 0\n");
            os.writeBytes("exit\n");
            os.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String result = reader.readLine();

            process.waitFor();
            return "1".equals(result);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

