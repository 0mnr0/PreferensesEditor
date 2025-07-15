package com.dsvl0.preferenseseditor;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
public class SharedPrefsParser {

    public static List<Setting> parseSharedPrefsXml(String xmlString) throws Exception {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            SharedPrefsHandler handler = new SharedPrefsHandler();
            saxParser.parse(new InputSource(new StringReader(xmlString)), handler);

            return handler.getSettings();
        } catch (SAXException e) {
            throw new Exception("SAX parsing error", e);
        }
    }

    private static class SharedPrefsHandler extends DefaultHandler {
        private final List<Setting> settings = new ArrayList<>();
        private String currentType;
        private String currentName;
        private StringBuilder stringValue;
        private boolean inStringElement = false;
        private boolean inSetElement = false;
        private List<String> currentSet;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            switch (qName) {
                case "int":
                case "long":
                case "float":
                case "double":
                case "boolean":
                    currentType = qName;
                    currentName = attributes.getValue("name");
                    String value = attributes.getValue("value");
                    addSetting(value);
                    break;

                case "string":
                    stringValue = new StringBuilder();
                    inStringElement = true;

                    // Устанавливаем тип только для обычных строк (не внутри сета)
                    if (!inSetElement) {
                        currentType = "string";
                        currentName = attributes.getValue("name");
                    }
                    break;

                case "set":
                    currentType = "set";
                    currentName = attributes.getValue("name");
                    inSetElement = true;
                    currentSet = new ArrayList<>();
                    break;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (inStringElement) {
                stringValue.append(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            switch (qName) {
                case "string":
                    if (inStringElement) {
                        if (inSetElement) {
                            // Добавляем строку в текущий сет
                            currentSet.add(stringValue.toString());
                        } else {
                            // Обрабатываем как обычную строку
                            addSetting(stringValue.toString());
                        }
                        inStringElement = false;
                        stringValue = null;
                    }
                    break;

                case "set":
                    if (inSetElement) {
                        // Сохраняем собранный сет (даже пустой)
                        addSetting(new HashSet<>(currentSet));
                        inSetElement = false;
                        currentSet = null;
                    }
                    break;
            }
        }

        private void addSetting(Object value) {
            if (currentName == null || currentType == null) return;

            try {
                Object finalValue;
                if ("set".equals(currentType)) {
                    finalValue = value;
                } else {
                    finalValue = parseValue(currentType, (String) value);
                }
                settings.add(new Setting(currentName, currentType, finalValue));
            } catch (Exception e) {
                // Логируем ошибку для отладки
                System.err.println("Error parsing setting: " + currentName);
                e.printStackTrace();
            } finally {
                // Всегда сбрасываем состояние после обработки
                currentType = null;
                currentName = null;
            }
        }

        private Object parseValue(String type, String valueStr) {
            switch (type) {
                case "int": return Integer.parseInt(valueStr);
                case "long": return Long.parseLong(valueStr);
                case "boolean": return Boolean.parseBoolean(valueStr);
                case "float": return Float.parseFloat(valueStr);
                case "double": return Double.parseDouble(valueStr);
                case "string": return valueStr;
                default: return valueStr;
            }
        }

        public List<Setting> getSettings() {
            return settings;
        }
    }

    public static class Setting {
        public String settingName;
        public String type;
        public Object value;

        public Setting(String settingName, String type, Object value) {
            this.settingName = settingName;
            this.type = type;
            this.value = value;
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @NonNull
        @Override
        public String toString() {
            String valueOutput;
            if (value instanceof String) {
                valueOutput = "\"" + value + "\"";
            }
            else if (value instanceof Set) {
                Set<?> set = (Set<?>) value;
                valueOutput = set.stream()
                        .map(obj -> "\"" + obj + "\"")
                        .collect(Collectors.joining(", ", "[", "]"));
            }
            else {
                valueOutput = String.valueOf(value);
            }

            return String.format(
                    "{\"settingName\": \"%s\", \"type\": \"%s\", \"value\": %s}",
                    settingName, type, valueOutput
            );
        }
    }
}