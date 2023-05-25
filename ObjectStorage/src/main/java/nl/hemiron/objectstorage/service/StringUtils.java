package nl.hemiron.objectstorage.service;

import java.util.Base64;

public class StringUtils {

    private StringUtils() {
    }

    public static String decodeBase64(String encoded) throws IllegalArgumentException {
        var decodedBytes = Base64.getDecoder().decode(encoded);
        return new String(decodedBytes);
    }

    public static String getFilenameFromBase64(String encoded) {
        var decoded = decodeBase64(encoded);
        var splitString = decoded.split("/");
        return splitString[splitString.length - 1];
    }
}
