package com.adam.util;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;

@SuppressWarnings("squid:CommentedOutCodeLine")
public class JsonHelper {
    private JsonHelper() {}

    public static String readerToString(Reader reader) throws IOException {
        StringBuilder jsonData = new StringBuilder();
        String line;


        try (BufferedReader br = new BufferedReader(reader)) {
            while ((line = br.readLine()) != null) {
                jsonData.append(line + "\n");
            }
        }

        return jsonData.toString();
    }

    public static String mapToJsonObject(Map<String, Object> map) {
        StringBuilder res = new StringBuilder();

        res.append("{\n");

        for (Map.Entry<String, Object> entry : map.entrySet()) {

            String valOut = "",
                   valKey = entry.getKey();
            Object val = entry.getValue();
            boolean isString = false;

            if (val instanceof List) {
                List<?> list = (List<?>) val;

                StringBuilder builder = new StringBuilder("[\n\t\t");

                for (Object o : list) {
                    builder.append("\"" + o.toString() + "\",\n\t\t");
                }

                builder.replace(builder.length()-4, builder.length(), "\n\t]");
                valOut = builder.toString();

            } else if (val instanceof Map) {
                valOut = mapToJsonObject((Map<String, Object>) val);

                String[] lines = valOut.split("\n");

                StringBuilder tempOut = new StringBuilder(lines[0] + "\n");

                for (int a = 1; a < lines.length; a++)
                    tempOut.append("\t" + lines[a] + "\n");

                tempOut.replace(tempOut.length()-1, tempOut.length(), "");
                valOut = tempOut.toString();

            } else {
                isString = true;
                valOut = val.toString();
            }

            if (isString)
                valOut = "\"" + valOut + "\"";

            res.append("\t\"" + valKey + "\": "
                    + valOut + ",\n");
        }

        if (res.length() > 2)
            res.replace(res.length()-2, res.length(), "\n}");
        else
            res.append("}");

        return res.toString();
    }

}