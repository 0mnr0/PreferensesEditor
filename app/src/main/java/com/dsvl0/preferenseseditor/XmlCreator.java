package com.dsvl0.preferenseseditor;

import java.util.*;
import java.io.StringWriter;

public class XmlCreator {

    private final List<String> entries = new ArrayList<>();

    public void add(String name, String type, Object value) {
        if (name == null || type == null || value == null) return;

        switch (type.toLowerCase()) {
            case "int":
            case "integer":
                entries.add("<int name=\"" + escape(name) + "\" value=\"" + value + "\"/>");
                break;
            case "long":
                entries.add("<long name=\"" + escape(name) + "\" value=\"" + value + "\"/>");
                break;
            case "float":
                entries.add("<float name=\"" + escape(name) + "\" value=\"" + value + "\"/>");
                break;
            case "boolean":
                entries.add("<boolean name=\"" + escape(name) + "\" value=\"" + value + "\"/>");
                break;
            case "string":
                entries.add("<string name=\"" + escape(name) + "\">" + escape(value != null ? value.toString() : "") + "</string>");
                break;
            case "set":
                if (value instanceof Set) {
                    @SuppressWarnings("unchecked")
                    Set<String> set = (Set<String>) value;
                    entries.add(buildStringSet(name, set));
                } else {
                    entries.add("<set name=\"" + escape(name) + "\"/>");
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    public String getResult() {
        StringWriter sw = new StringWriter();
        sw.append("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n");
        sw.append("<map>\n");
        for (String entry : entries) {
            sw.append("    ").append(entry).append("\n");
        }
        sw.append("</map>");
        return sw.toString();
    }

    private String buildStringSet(String name, Set<String> set) {
        if (set == null || set.isEmpty()) {
            return "<set name=\"" + escape(name) + "\"/>";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<set name=\"" + escape(name) + "\">\n");
        for (String item : set) {
            sb.append("        <string>").append(escape(item)).append("</string>\n");
        }
        sb.append("    </set>");
        return sb.toString();
    }

    private String escape(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}

