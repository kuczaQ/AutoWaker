package com.adam.wol;


import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import adam.util.JsonHelper;


@SuppressWarnings("squid:S106")
public class AutoWaker {
    public static class Bundle {
        byte[] mac;
        String stringMAC;

        public Bundle(byte[] mac, String stringMAC) {
            this.mac = mac;
            this.stringMAC = stringMAC;
        }
    }

    private static final String START_MSG =    "  ___        _        _    _       _               _____ _____ _____ _____ \n" +
            " / _ \\      | |      | |  | |     | |             / __  |  _  |  _  |  _  |\n" +
            "/ /_\\ \\_   _| |_ ___ | |  | | __ _| | _____ _ __  `' / /| |/' | |/' | |/' |\n" +
            "|  _  | | | | __/ _ \\| |/\\| |/ _` | |/ / _ | '__|   / / |  /| |  /| |  /| |\n" +
            "| | | | |_| | || (_) \\  /\\  | (_| |   |  __| |    ./ /__\\ |_/ \\ |_/ \\ |_/ /\n" +
            "\\_| |_/\\__,_|\\__\\___/ \\/  \\/ \\__,_|_|\\_\\___|_|    \\_____/\\___/ \\___/ \\___/ \n\n";

    private static final String APP_TITLE = "--- AutoWaker 2000 ---";

    private static final String HELP_MSG =
                    "-start [delay (seconds)]             starts the Waker | default delay = 5s\n" +
                    "-set                                 sets trigger/target device (mac separators: '-' or ':')\n" +
                    "-list                                list all current mappings\n" +
                    "-l                                   same as -list";

    private static final String CONFIG_FILE_NAME = "config.json";

    private static final String DEFAULT_CONFIG =
            "{\n" +
            "    \"devices\": {\n" +
            "        \n" +
            "    },\n" +
            "    \"mappings\": {\n" +
            "        \n" +
            "    }\n" +
            "}";

    private static final String DEFAULT_DEBUG_CONFIG =
            "{\n" +
            "    \"devices\": {\n" +
            "         " +
            "    },\n" +
            "    \"mappings\": {\n" +
            "        \"2c-30-33-29-7d-ef\": \"44-8a-5b-d5-f1-85\",\n" +
                    "\"oj\": {" +
                        "\"ico\": \"teraz\"" +
                    "}" +
                "}, \"test\": \"hej\"" +
            "}";

    private static final String ENCODING = "UTF-8";

    // Static variables
    private static JSONObject config;

    private static ArgStream argStream;

    private static String triggerMACString = null;

    private static InetAddress targetIP;

    static {
        try {
            targetIP = InetAddress.getByName("10.0.0.44");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private static byte[] triggerMAC = WakeOnLan.getMacBytes("2c-30-33-29-7d-ef"),
                          targetMAC = WakeOnLan.getMacBytes("44-8a-5b-d5-f1-85");


    ////////////
    //  MAIN  //
    ////////////

    public static void main(String[] args) throws IOException {
        loadConfig();

        if (args.length == 0) {
            println(START_MSG);
            println(HELP_MSG);
            //runArpA();
            saveConfig();
            System.exit(0);
        }

        argStream = new ArgStream(args);

        try {
            parseCommands();
        } catch (Exception e) {
            // Debug start
            e.printStackTrace();
            // Debug end
            printHelp();
        }
    }

    private static void parseCommands() throws Exception {
        String cmd = argStream.getNext();

        switch (cmd) {
            case "-start": {
                startWaker();
            } break;

            case "-set": {
                set();
            } break;

            case "-l":
            case "-list": {
                listMappings();
            } break;

            default: printHelp();
        }
    }

    private static void printHelp() {
        println("                    " + APP_TITLE + "\n\n" + HELP_MSG);
    }

    @SuppressWarnings("squid:S2189")
    private static void startWaker() throws IOException {
        int delay;

        try {
            delay = argStream.getNextInt() * 1000;
        } catch (Exception e) {
            delay = 5000;
        }


        while (true) {
            println("Checking MACs...");

            List<String[]> macs = runArpA(false);

            for (String[] sArr : macs) {
                byte[] currentMAC = WakeOnLan.getMacBytes(sArr[1]);
                if (WakeOnLan.compareMacs(triggerMAC, currentMAC)) {
                    print("Triggered!!! Pinging " + targetIP.getHostAddress() + "\t");

                    if (targetIP.isReachable(8080)) {
                        println("...and it's ON!");
                    } else {
                        println("...and it's OFF!\n" +
                                           "Sending WoL...");

                        WakeOnLan.sendWoL(targetIP, targetMAC, "Wake-on-LAN packet sent.");
                    }

                    break;
                }
            }

            break;

//            try {
//                Thread.sleep(delay);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }
    }

    private static void set() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))){
            Bundle trgrMAC   = readMac(br, "\tTrigger MAC: "),
                   targetMAC = readMac(br, "\tTarget MAC: ");

            InetAddress targetIP = readIP(br, "\tTarget IP: ");

            JSONObject mappings = config.getJSONObject("mappings");

            JSONObject bundle = new JSONObject("{\n" +
                    "    \"mac\":\"" + targetMAC.stringMAC + "\",\n" +
                    "    \"ip\":\"" + targetIP.getHostAddress() + "\"\n" +
                    "}");

            JSONArray targets = mappings.get(trgrMAC.stringMAC);

            if (targets == null) {
                targets = new JSONArray("[]");
                targets.push(bundle);
                mappings.put(trgrMAC.stringMAC, bundle);
            } else {
                // TODO push to array
            }
            saveConfig();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static Bundle readMac(BufferedReader br, String newLineMsg) {
        Bundle res = null;

        boolean ok;

        do {
            ok = true;
            print(newLineMsg);
            try {
                String line = br.readLine().trim();
                byte[] bytes = WakeOnLan.getMacBytes(line);

                res = new Bundle(bytes, line);
            } catch (Exception e) {
                ok = false;
                print("\t\tInvalid mac Adress!\n");
            }
        } while (!ok);

        return res;
    }

    private static InetAddress readIP(BufferedReader br, String newLineMsg) {
        InetAddress res = null;

        boolean ok;

        do {
            ok = true;
            print(newLineMsg);
            try {
                String line = br.readLine();
                res = InetAddress.getByName(line);
            } catch (Exception e) {
                ok = false;
                print("\t\tInvalid IP Adress!\n");
            }
        } while (!ok);

        return res;
    }

    public static void listMappings() {
        String out = HexConverter.parseMAC(triggerMAC) +
                " -> " +
                HexConverter.parseMAC(targetMAC);

        println(out);
    }

    public static List<String[]> runArpA(boolean print) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(
                "cmd.exe", "/c", "arp -a");
        builder.redirectErrorStream(true);
        Process p = builder.start();
        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        List<String[]> res = new ArrayList<>();

        while (true) {
            line = r.readLine();

            if (line == null) { break; }

            if (line.matches("^(?![a-zA-Z]).*[0-9].*")) {
                String[] extractedLine = line
                                            .replaceAll("\\s+", " ")
                                            .trim()
                                            .split("\\s");

                res.add(extractedLine);

                if (print)
                    println("IP: " + extractedLine[0] + "  mac: " + extractedLine[1]);
            }
        }
        return res;
    }

    private static void loadConfig() {
        try (BufferedReader br = new BufferedReader(new FileReader(new File("./" + CONFIG_FILE_NAME)))) {
            String json = JsonHelper.readerToString(br);
            config = new JSONObject(json);
        } catch (Exception e) {
            config = new JSONObject(DEFAULT_CONFIG);
        }
    }

    private static void saveConfig() {
        try (PrintWriter writer = new PrintWriter(CONFIG_FILE_NAME, ENCODING)){
            String save = JsonHelper.mapToJsonObject(config.toMap());

            writer.println(save);
        } catch (Exception e) {
            e.printStackTrace();
            printErr("Cannot create config file! Make sure you have sufficient rights.");
        }
    }

    private static void printErr(String s) {
        System.err.println(s);
    }

    public static void print(Object obj) {
        System.out.print(obj);
    }

    public static void println(Object obj) {
        System.out.println(obj);
    }

}
