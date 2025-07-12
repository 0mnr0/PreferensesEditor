package com.dsvl0.preferenseseditor;


import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;
import org.xml.sax.InputSource;



public class SharedPrefsParser {

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

    public static List<Setting> parseSharedPrefsXml(String xmlString) throws Exception {
        List<Setting> result = new ArrayList<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        InputSource is = new InputSource(new StringReader(xmlString));
        Document doc = builder.parse(is);
        doc.getDocumentElement().normalize();

        Element root = doc.getDocumentElement(); // <map>

        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) node;
                String type = el.getTagName();  // например int, boolean, long, string и т.д.
                String name = el.getAttribute("name");
                String valueStr = null;

                // У разных тегов атрибуты с данными могут называться по-разному:
                // - int, boolean, long, float, double: value
                // - string: текст внутри тега (child text node)
                // - set: содержит множество строк (особый случай)
                if (type.equals("string")) {
                    valueStr = el.getTextContent();
                } else if (type.equals("set")) {
                    // для set можно собрать все string внутри в List<String>,
                    // но для упрощения сейчас пропустим
                    continue;
                } else {
                    valueStr = el.getAttribute("value");
                }

                Object value = null;
                switch (type) {
                    case "int":
                        value = Integer.parseInt(valueStr);
                        break;
                    case "long":
                        value = Long.parseLong(valueStr);
                        break;
                    case "boolean":
                        value = Boolean.parseBoolean(valueStr);
                        break;
                    case "float":
                        value = Float.parseFloat(valueStr);
                        break;
                    case "double":
                        value = Double.parseDouble(valueStr);
                        break;
                    case "string":
                        value = valueStr;
                        break;
                    default:
                        // другие типы можно добавить
                        value = valueStr;
                        break;
                }

                result.add(new Setting(name, type, value));
            }
        }

        return result;
    }

    public static void main(String[] args) throws Exception {
        String xml = "<map>\n" +
                "    <int name=\"request_in_session_count\" value=\"30\" />\n" +
                "    <boolean name=\"gad_idless\" value=\"false\" />\n" +
                "    <long name=\"first_ad_req_time_ms\" value=\"1745580018100\" />\n" +
                "    <long name=\"app_settings_last_update_ms\" value=\"1742277724848\" />\n" +
                "    <long name=\"app_last_background_time_ms\" value=\"1745580805265\" />\n" +
                "</map>";

        List<Setting> settings = parseSharedPrefsXml(xml);
        System.out.println("[");
        for (int i = 0; i < settings.size(); i++) {
            System.out.print(settings.get(i).toString());
            if (i < settings.size() - 1) System.out.print(", ");
        }
        System.out.println("]");
    }
}
