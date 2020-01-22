package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.utils;

import com.google.gson.*;
import com.google.gson.stream.MalformedJsonException;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.PlanDetail;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.apgen.model.Plan;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonUtilities {

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
     * @param path - The path to which the output should be written (should not already exist)
     * @return boolean whether write was successful
     */
    // TODO: Throw IOException instead of handling it here
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

    public static boolean writeJson(InputStream source, Path path) throws IOException {
        return writeJson(new String(source.readAllBytes()), path);
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
        for (gov.nasa.jpl.ammos.mpsa.apgen.model.ActivityInstance act : plan.getActivityInstanceList()) {
            JsonObject jsonAct = new JsonObject();
            jsonAct.addProperty("activityType", act.getType());
            jsonAct.addProperty("name", act.getName());

            if (act.hasAttribute("Start")) {
                jsonAct.addProperty("startTimestamp", act.getAttribute("Start").getValue());
            }

            JsonArray parameters = new JsonArray();
            for (gov.nasa.jpl.ammos.mpsa.apgen.model.ActivityInstanceParameter param : act.getParameters()) {
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

    public static String convertPlanToJson(PlanDetail plan) {
        return prettify(new Gson().toJson(plan, PlanDetail.class));
    }

    public static PlanDetail parsePlanJson(String planJson) {
        return new Gson().fromJson(planJson, PlanDetail.class);
    }

    public static PlanDetail parsePlanJson(InputStream jsonStream) throws IOException {
        return parsePlanJson(new String(jsonStream.readAllBytes()));
    }

    public static String convertActivityInstanceToJson(ActivityInstance activityInstance) {
        return prettify(new Gson().toJson(activityInstance, ActivityInstance.class));
    }

    public static ActivityInstance parseActivityInstanceJson(String instanceJson) {
        return new Gson().fromJson(instanceJson, ActivityInstance.class);
    }

    public static ActivityInstance parseActivityInstanceJson(InputStream jsonStream) throws IOException {
        return parseActivityInstanceJson(new String(jsonStream.readAllBytes()));
    }

    public static String convertAdaptationToJson(Adaptation adaptation) {
        return prettify(new Gson().toJson(adaptation, Adaptation.class));
    }

    public static Adaptation parseAdaptationJson(String adaptationJson) {
        return new Gson().fromJson(adaptationJson, Adaptation.class);
    }

    public static Adaptation parseAdaptationJson(InputStream jsonStream) throws IOException {
        return parseAdaptationJson(new String(jsonStream.readAllBytes()));
    }
}
