package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class JSONUtilities {

    /**
     * Formats a string to pretty-printed JSON
     * @param json - the unformatted JSON string
     * @return a formatted version of the input JSON string
     */
    public static String prettify(String json) {
        Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(json);
        return gson.toJson(je);
    }

    /**
     * Writes out a formatted JSON file
     * @param body - The JSON to write out
     * @param fileName - The name of the file to write
     * @return boolean whether write was successful
     */
    public static boolean writeJson(String body, String fileName) {
        try {
            String json = prettify(body);
            new File(fileName).createNewFile();
            FileOutputStream writer = new FileOutputStream(fileName);
            writer.write(json.getBytes());
            writer.close();
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
            return false;
        }
        return true;
    }
}
