package com.dsvl0.preferenseseditor;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

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
        private boolean skipElement = false;

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
                    currentType = "string";
                    currentName = attributes.getValue("name");
                    stringValue = new StringBuilder();
                    inStringElement = true;
                    break;

                case "set":
                    inSetElement = true;
                    skipElement = true; // Пропускаем обработку set
                    break;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (inStringElement && !inSetElement) {
                stringValue.append(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            switch (qName) {
                case "string":
                    if (inStringElement && !inSetElement) {
                        addSetting(stringValue.toString());
                    }
                    inStringElement = false;
                    break;

                case "set":
                    inSetElement = false;
                    skipElement = false;
                    break;
            }
        }

        private void addSetting(String valueStr) {
            if (skipElement || currentName == null || currentType == null) return;

            try {
                Object value = parseValue(currentType, valueStr);
                settings.add(new Setting(currentName, currentType, value));
            } catch (Exception e) {
                // Обработка ошибок преобразования
            }

            currentType = null;
            currentName = null;
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

    // Класс Setting остается без изменений
    public static class Setting {
        public String settingName;
        public String type;
        public Object value;

        public Setting(String settingName, String type, Object value) {
            this.settingName = settingName;
            this.type = type;
            this.value = value;
        }

        @Override
        public String toString() {
            return String.format("{\"settingName\": \"%s\", \"type\": \"%s\", \"value\": %s}",
                    settingName, type, value instanceof String ? "\"" + value + "\"" : value);
        }
    }
}