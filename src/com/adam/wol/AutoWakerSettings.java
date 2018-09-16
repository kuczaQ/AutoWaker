package com.adam.wol;

import com.adam.util.JsonHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

public class AutoWakerSettings {
    private static final String DEFAULT_CONFIG_FILE_NAME = "config.json";
    private static final String ENCODING = "UTF-8";
    private static final String DEFAULT_CONFIG =
                    "{\n" +
                    "    \"devices\": {\n" +
                    "        \n" +
                    "    },\n" +
                    "    \"mappings\": {\n" +
                    "        \n" +
                    "    }\n" +
                    "}";

    ///////////////////////
    //  LOCAL VARIABLES  //
    ///////////////////////

    private String configFileName;
    private JSONObject config;


    ////////////////////
    //  CONSTRUCTORS  //
    ////////////////////

    public AutoWakerSettings() {
        this.configFileName = DEFAULT_CONFIG_FILE_NAME;
    }

    public AutoWakerSettings(String configFileName) {
        if (configFileName == null)
            throw new IllegalArgumentException("configFileName is null!");
        if (configFileName.isEmpty())
            throw new IllegalArgumentException("configFileName is empty!");

        this.configFileName = configFileName;
    }

    ///////////////
    //  METHODS  //
    ///////////////

    public void load() {
        try (BufferedReader br = new BufferedReader(new FileReader(new File("./" + configFileName)))) {
            String json = JsonHelper.readerToString(br);
            config = new JSONObject(json);
        } catch (Exception e) {
            config = new JSONObject(DEFAULT_CONFIG);
        }
    }

    public void save() {
        try (PrintWriter writer = new PrintWriter(configFileName, ENCODING)){
            String save = JsonHelper.mapToJsonObject(config.toMap());

            writer.println(save);
        } catch (Exception e) {
            e.printStackTrace();
            AutoWaker.printErr("Cannot create the config file! Make sure you have sufficient rights.");
        }
    }

    public JSONObject getMappings() {
        return config.getJSONObject("mappings");
    }

    public JSONObject getDevices() {
        return config.getJSONObject("devices");
    }

    public void addMapping(String trigger, String target) {
        JSONArray targets;

        try {
            targets = getMappings().getJSONArray(trigger);

            for (Object o : targets) {
                if (!(o instanceof String))
                    throw new ClassCastException(o + " is not a String!");

                String s = (String) o;

                if (s.equals(target))
                    return;
            }
        } catch (JSONException e) {
            targets = new JSONArray();
        }

        targets.put(target);
        getMappings().put(trigger, targets);
    }

    public void addDeviceIp(String mac, String ip) {
        JSONObject device = getDevice(mac);
        device.put("ip", ip);
        getDevices().put(mac, device);
//        getMappings().put(trigger, targets);
    }

    public JSONObject getDevice(String mac) {
        try {
            return getDevices().getJSONObject(mac);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }
    //////////////////////
    //  STATIC METHODS  //
    //////////////////////

    public static JSONObject createDeviceBundle(String ip) {
        return new JSONObject("{\n" +
                "    \"ip\":\"" + ip + "\"\n" +
                "}");
    }
}
