package com.adam.wol;


import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

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
it st


    // Static variables
    private static AutoWakerSettings settings;
    private static ArgStream argStream;
    private static InetAddress targetIP;
    private static String triggerMACString = null;
    private static boolean isWindows = false;
    private static boolean isLinux = false;

    static {
        try {
            targetIP = InetAddress.getByName("10.0.0.44");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        print(System.getProperty("os.name"));
    }

    private static byte[] triggerMAC = WakeOnLan.getMacBytes("2c-30-33-29-7d-ef"),
                          targetMAC = WakeOnLan.getMacBytes("44-8a-5b-d5-f1-85");


    ////////////
    //  MAIN  //
    ////////////

    public static void main(String[] args) throws IOException {
        settings = new AutoWakerSettings();
        settings.load();
        if (args.length == 0) {
            println(START_MSG);
            println(HELP_MSG);
            //runArpA();
            settings.save();
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

            List<String[]> macs;

            //= runArpA(true);

            if (false)
            for (String[] sArr : macs) {
                byte[] currentMAC = WakeOnLan.getMacBytes(sArr[1]);
                if (WakeOnLan.compareMacs(triggerMAC, currentMAC)) {
                    print("Triggered!!! Pinging " + targetIP.getHostAddress() + "\t");

                    if (targetIP.isReachable(5000)) { // TODO make timeout configurable
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

    private static boolean checkMACTrigger(String mac) {

        return false;
    }

    private static void set() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))){
            Bundle trgrMAC   = readMac(br, "\tTrigger MAC: "),
                   targetMAC = readMac(br, "\tTarget MAC: ");

            // TODO needs lots of work

            InetAddress targetIP = readIP(br, "\tTarget IP: ");
        
            JSONObject mappings = settings.getMappings();
            JSONObject devices = settings.getDevices();

            //mappings.put(trgrMAC.stringMAC, targetMAC.stringMAC);

            JSONObject bundle = AutoWakerSettings.createDeviceBundle(targetIP.getHostAddress());

            settings.addMapping(trgrMAC.stringMAC, targetMAC.stringMAC);
            settings.addDeviceIp(targetMAC.stringMAC, targetIP.getHostAddress());
            settings.save();

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
        JSONObject mappings = settings.getMappings();

        //for (int a = 0; a < mappings.lenght; a++)


        String out = HexConverter.parseMAC(triggerMAC) +
                     " -> " +
                     HexConverter.parseMAC(targetMAC);

        println(out);
    }

    public static List<String[]> runArpWin (boolean print) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(
                "cmd.exe", "/c", "arp -a");

        return runArpA(builder, "^(?![a-zA-Z]).*[0-9].*", print);
    }

    public static List<String[]> runArpLinux (boolean print) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(
                "bash", "arp -a");

        return runArpA(builder, "^(?![a-zA-Z]).*[0-9].*", print);
    }

    public static List<String[]> runArpA(ProcessBuilder processBuilder, String regex, boolean print) throws IOException {

        processBuilder.redirectErrorStream(true);
        Process p = processBuilder.start();
        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        List<String[]> res = new ArrayList<>();

        while (true) {
            line = r.readLine();

            if (line == null) { break; }

            if (line.matches(regex)) {
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
