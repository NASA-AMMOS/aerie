package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.utils;

import com.google.gson.*;
import com.google.gson.stream.MalformedJsonException;
import gov.nasa.jpl.ammos.mpsa.apgen.model.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.apgen.model.ActivityInstanceParameter;
import gov.nasa.jpl.ammos.mpsa.apgen.model.Plan;
import org.json.JSONArray;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

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

    // TODO: Take a path for the file name instead of a string
    /**
     * Writes out a formatted JSON file
     * @param body - The JSON to write out
     * @param path - The path to which the output should be written (should not already exist)
     * @return boolean whether write was successful
     */
    public static boolean writeJson(String body, Path path) {
        try {
            String json = prettify(body);
            Files.createFile(path);
            FileOutputStream writer = new FileOutputStream(path.toString());
            writer.write(json.getBytes());
            writer.close();

        } catch (MalformedJsonException e) {
            System.err.println(String.format("Malformed JSON cannot be formatted: %s", e.getMessage()));
            return false;

        } catch (FileAlreadyExistsException e) {
            System.err.println("Output file already exists: " + e.getMessage());
            return false;

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        return true;
    }

    public static boolean writePlanToJSON(Plan plan, Path output, String adaptationId, String startTimestamp, String name) {
        Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
        JsonObject jsonPlan = buildPlanJsonObject(plan, adaptationId, startTimestamp, name);
        String jsonPlanString = gson.toJson(jsonPlan);
        return writeJson(jsonPlanString, output);
    }

    public static JsonObject buildPlanJsonObject(Plan plan, String adaptationId, String startTimestamp, String name) {
        JsonObject jsonPlan = new JsonObject();
        jsonPlan.addProperty("adaptationId", adaptationId);
        jsonPlan.addProperty("startTimestamp", startTimestamp);
        jsonPlan.addProperty("name", name);

        JsonArray activities = new JsonArray();
        for (ActivityInstance act : plan.getActivityInstanceList()) {
            JsonObject jsonAct = new JsonObject();
            jsonAct.addProperty("activityType", act.getType());
            jsonAct.addProperty("name", act.getName());

            if (act.hasAttribute("Start")) {
                jsonAct.addProperty("startTimestamp", act.getAttribute("Start").getValue());
            }

            JsonArray parameters = new JsonArray();
            for (ActivityInstanceParameter param : act.getParameters()) {
                JsonObject jsonParam = new JsonObject();
                jsonParam.addProperty("name", param.getName());
                jsonParam.addProperty("type", param.getType());
                jsonParam.addProperty("value", param.getValue());

                parameters.add(jsonParam);
            }

            activities.add(jsonAct);
        }

        jsonPlan.add("activityInstances", activities);

        return jsonPlan;
    }
}
