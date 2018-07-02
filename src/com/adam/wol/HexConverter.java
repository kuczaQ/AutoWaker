package com.adam.wol;

public class HexConverter {
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    private HexConverter() {}

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String formatMAC(String s) {
        char[] chars = s.toCharArray();
        StringBuilder res = new StringBuilder();

        for (int a = 0; a < chars.length - 1; a += 2) {
            res.append(chars[a] + "" + chars[a + 1]);

            if (a != chars.length - 2)
                res.append(":");
        }

        return res.toString();
    }

    public static String parseMAC(byte[] mac) {
        return formatMAC(bytesToHex(mac));
    }
}