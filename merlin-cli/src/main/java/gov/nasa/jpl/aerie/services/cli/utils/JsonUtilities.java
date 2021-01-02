package gov.nasa.jpl.aerie.services.cli.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.MalformedJsonException;
import gov.nasa.jpl.aerie.apgen.model.Plan;
import gov.nasa.jpl.aerie.services.cli.models.ActivityInstance;
import gov.nasa.jpl.aerie.services.cli.models.Adaptation;
import gov.nasa.jpl.aerie.services.cli.models.PlanDetail;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.stream.JsonParsingException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
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
        com.google.gson.JsonElement je = com.google.gson.JsonParser.parseString(json);
        return gson.toJson(je);
    }

    /**
     * Writes out a formatted JSON file
     * @param body - The JSON to write out
     * @param path - The path to which the output should be written (should not already exist)
     * @return boolean whether write was successful
     */
    // TODO: Throw Exceptions instead of printing error messages
    //       Outputting errors is not the responsibility of this utility class
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
        com.google.gson.JsonObject jsonPlan = buildPlanJsonObject(plan, adaptationId, startTimestamp, name);
        String jsonPlanString = gson.toJson(jsonPlan);
        return writeJson(jsonPlanString, output);
    }

    public static com.google.gson.JsonObject buildPlanJsonObject(Plan plan, String adaptationId, String startTimestamp, String name) {
        com.google.gson.JsonObject jsonPlan = new com.google.gson.JsonObject();
        jsonPlan.addProperty("adaptationId", adaptationId);
        jsonPlan.addProperty("startTimestamp", startTimestamp);
        jsonPlan.addProperty("name", name);

        com.google.gson.JsonArray activities = new com.google.gson.JsonArray();
        for (gov.nasa.jpl.aerie.apgen.model.ActivityInstance act : plan.getActivityInstanceList()) {
            com.google.gson.JsonObject jsonAct = new com.google.gson.JsonObject();
            jsonAct.addProperty("activityType", act.getType());
            jsonAct.addProperty("name", act.getName());

            if (act.hasAttribute("Start")) {
                jsonAct.addProperty("startTimestamp", act.getAttribute("Start").getValue());
            }

            com.google.gson.JsonArray parameters = new com.google.gson.JsonArray();
            for (gov.nasa.jpl.aerie.apgen.model.ActivityInstanceParameter param : act.getParameters()) {
                com.google.gson.JsonObject jsonParam = new com.google.gson.JsonObject();
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

    public static String getErrorMessageFromJsonValue(JsonValue jsonValue) throws ResponseWithoutErrorMessageException {
        try {
            switch(jsonValue.getValueType()) {
                case OBJECT:
                    return jsonValue.asJsonObject().getString("message");
                case ARRAY:
                    JsonArray jsonArray = jsonValue.asJsonArray();
                    // Return only the first error message
                    return getErrorMessageFromJsonValue(jsonArray.get(0));
                default:
                    // JsonValue doesn't contain a message
                    throw new ResponseWithoutErrorMessageException("JSON value neither object nor array");
            }
        } catch (NullPointerException | IndexOutOfBoundsException e) {
            throw new ResponseWithoutErrorMessageException(e);
        }
    }

    public static String getErrorMessageFromFailureResponse(String responseBody) throws ResponseWithoutErrorMessageException {
        try {
            JsonReader reader = Json.createReader(new StringReader(responseBody));
            return getErrorMessageFromJsonValue(reader.readValue());
        } catch (JsonParsingException e) {
            throw new ResponseWithoutErrorMessageException(e);
        }
    }

    public static String getErrorMessageFromFailureResponse(InputStream jsonStream) throws IOException, ResponseWithoutErrorMessageException {
        return getErrorMessageFromFailureResponse(new String(jsonStream.readAllBytes()));
    }

    public static class ResponseWithoutErrorMessageException extends Exception {
        public ResponseWithoutErrorMessageException(Exception sourceException) {
            super(sourceException);
        }

        public ResponseWithoutErrorMessageException(String message) {
            super(message);
        }
    }
}
