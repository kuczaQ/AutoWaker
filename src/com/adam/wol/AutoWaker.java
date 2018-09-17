package com.adam.wol;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.adam.util.ArgStream;


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
                    "-start [refresh rate (seconds)]      starts the Waker | default r. rate = 5s\n" +
                    "-set                                 sets trigger/target device (mac separators: '-' or ':')\n" +
                    "-list                                list all current mappings\n" +
                    "-l                                   same as -list";



//    private static final String DEFAULT_DEBUG_CONFIG =
//            "{\n" +
//            "    \"devices\": {\n" +
//            "         " +
//            "    },\n" +
//            "    \"mappings\": {\n" +
//            "        \"2c-30-33-29-7d-ef\": \"44-8a-5b-d5-f1-85\",\n" +
//                    "\"oj\": {" +
//                        "\"ico\": \"teraz\"" +
//                    "}" +
//                "}, \"test\": \"hej\"" +
//            "}";



    // Static variables
    private static AutoWakerSettings _settings;
    private static ArgStream _argStream;
    private static InetAddress _targetIP;
    private static String _triggerMACString = null;
    private static boolean _isWindows = false;
    private static boolean _isLinux = false;

    static {
        try {
            _targetIP = InetAddress.getByName("10.0.0.44");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows"))
            _isWindows = true;
        else if (os.contains("linux"))
            _isLinux = true;
    }

    private static byte[] triggerMAC = WakeOnLan.getMacBytes("2c-30-33-29-7d-ef"),
                          targetMAC = WakeOnLan.getMacBytes("44-8a-5b-d5-f1-85");


    ////////////
    //  MAIN  //
    ////////////

    public static void main(String[] args) throws IOException {
        _settings = new AutoWakerSettings();
        _settings.load();
        if (args.length == 0) {
            println(START_MSG);
            println(HELP_MSG);
            //runArpA();
            _settings.save();
            System.exit(0);
        }


        _argStream = new ArgStream(args);

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
        String cmd = _argStream.getNext();

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
            delay = _argStream.getNextInt() * 1000;
        } catch (Exception e) {
            delay = 5000;
        }


        while (true) {
            println("Checking MACs...");

            List<String[]> macs;

            if (_isWindows)
                macs = runArpWin(false);
            else if (_isLinux)
                macs = runArpLinux(true);
            else
                throw new RuntimeException("OS not supported!");

            for (String[] sArr : macs) {
                InetAddress targetIP = InetAddress.getByName(sArr[0]);
                if (checkMACTrigger(sArr[1])) {
                    print("Triggered!!! Pinging " + targetIP.getHostAddress() + "\t");

                    if (targetIP.isReachable(5000)) { // TODO make timeout configurable
                        println("...and it's ON!");
                    } else {
                        println("...and it's OFF!\n" +
                                           "Sending WoL...");

                        WakeOnLan.sendWoL(targetIP, WakeOnLan.getMacBytes(sArr[1]), "Wake-on-LAN packet sent.");
                    }

                    break;
                }
            }

            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean checkMACTrigger(String mac) {
        try {
            _settings.getMappings().getJSONArray(mac);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    private static void set() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))){
            Bundle trgrMAC   = readMac(br, "\tTrigger MAC: "),
                   targetMAC = readMac(br, "\tTarget MAC: ");

            // TODO needs lots of work

            InetAddress targetIP = readIP(br, "\tTarget IP: ");
        
            JSONObject mappings = _settings.getMappings();
            JSONObject devices = _settings.getDevices();

            //mappings.put(trgrMAC.stringMAC, targetMAC.stringMAC);

            JSONObject bundle = AutoWakerSettings.createDeviceBundle(targetIP.getHostAddress());

            _settings.addMapping(trgrMAC.stringMAC, targetMAC.stringMAC);
            _settings.addDeviceIp(targetMAC.stringMAC, targetIP.getHostAddress());
            _settings.save();

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
        JSONObject mappings = _settings.getMappings();

        //for (int a = 0; a < mappings.lenght; a++)


        String out = HexConverter.parseMAC(triggerMAC) +
                     " -> " +
                     HexConverter.parseMAC(targetMAC);

        println(out);
    }

    public static List<String[]> runArpWin (boolean print) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(
                "cmd.exe", "/c", "arp -a");

        return runArpA(
                builder,
                "^(?![a-zA-Z]).*[0-9].*",
                print,
                line -> line
                        .replaceAll("\\s+", " ")
                        .trim()
                        .split("\\s")
        );
    }

    public static List<String[]> runArpLinux (boolean print) throws IOException {
        ProcessBuilder nmap = new ProcessBuilder(
                "bash", "-c", "nmap -sn 10.0.0.0/24");
        nmap.redirectErrorStream(true);
        Process p = nmap.start();
        try {
            p.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ProcessBuilder builder = new ProcessBuilder(
                "bash", "-c", "arp -a");

        return runArpA(builder,
                "^.*$",
                print,
                line -> line
                        .replaceAll("\\s*\\[ether.*$", "")
                        .replaceAll("^\\?\\s*\\(", "")
                        .replaceAll("\\)\\s*at\\s*", " ") // <-- Space
                        .trim()
                        .split("\\s")
        );
    }

    public static List<String[]> runArpA(
            ProcessBuilder processBuilder,
            String regex,
            boolean print,
            Function<String, String[]> processLine) throws IOException {

        processBuilder.redirectErrorStream(true);
        Process p = processBuilder.start();
        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        List<String[]> res = new ArrayList<>();

        while (true) {
            line = r.readLine();

            if (line == null) { break; }

            if (line.matches(regex)) {
                String[] extractedLine = processLine.apply(line);

                res.add(extractedLine);

                if (print)
                    println("IP: " + extractedLine[0] + "  mac: " + extractedLine[1]);
            }
        }
        return res;
    }

    public static void printErr(String s) {
        System.err.println(s);
    }

    public static void print(Object obj) {
        System.out.print(obj);
    }

    public static void println(Object obj) {
        System.out.println(obj);
    }

}
