package gov.nasa.jpl.ammos.mpsa.aerie.schemas;

import java.io.InputStream;
import java.io.IOException;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaClient;
import org.everit.json.schema.ValidationException;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.net.URL;
import java.util.List;

public class Validator {
    private static Schema loadSchema(URL resourceUrl) throws IOException {
        try (final InputStream inputStream = resourceUrl.openStream()) {
            final JSONObject schemaJson = new JSONObject(new JSONTokener(inputStream));

            final org.everit.json.schema.loader.SchemaLoader schemaLoader =
                org.everit.json.schema.loader.SchemaLoader.builder()
                    .schemaClient(SchemaClient.classPathAwareClient())
                    .schemaJson(schemaJson)
                    .resolutionScope("classpath://gov/nasa/jpl/ammos/mpsa/aerie/schemas/")
                    .build();

            return schemaLoader.load().build();
        }
    }

    private static List<String> findValidationFailures(final Schema schema, final JSONObject object) {
        try {
            schema.validate(object);
            return List.of();
        } catch (final ValidationException ex) {
            return ex.getAllMessages();
        }
    }

    public static List<String> findValidationFailures(final ActivityInstance instance) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("activity-instance.json"));
        return findValidationFailures(schema, new JSONObject(instance));
    }

    public static List<String> findValidationFailures(final ActivityInstanceConstraint constraint) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("activity-instance-constraint.json"));
        return findValidationFailures(schema, new JSONObject(constraint));
    }

    public static List<String> findValidationFailures(final ActivityInstanceParameter parameter) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("activity-instance-parameter.json"));
        return findValidationFailures(schema, new JSONObject(parameter));
    }

    public static List<String> findValidationFailures(final ActivityType type) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("activity-type.json"));
        return findValidationFailures(schema, new JSONObject(type));
    }

    public static List<String> findValidationFailures(final ActivityTypeParameter parameter) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("activity-type-parameter.json"));
        return findValidationFailures(schema, new JSONObject(parameter));
    }

    public static List<String> findValidationFailures(final Adaptation adaptation) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("adaptation.json"));
        return findValidationFailures(schema, new JSONObject(adaptation));
    }

    public static List<String> findValidationFailures(final AmqpMessage adaptation) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("amqp-message.json"));
        return findValidationFailures(schema, new JSONObject(adaptation));
    }

    public static List<String> findValidationFailures(final AmqpMessageData adaptation) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("amqp-message-data.json"));
        return findValidationFailures(schema, new JSONObject(adaptation));
    }

    public static List<String> findValidationFailures(final AmqpMessageTypeEnum adaptation) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("amqp-message-type-enum.json"));
        return findValidationFailures(schema, new JSONObject(adaptation));
    }

    public static List<String> findValidationFailures(final CommandDictionary dictionary) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("command-dictionary.json"));
        return findValidationFailures(schema, new JSONObject(dictionary));
    }

    public static List<String> findValidationFailures(final MpsCommand command) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("mps-command.json"));
        return findValidationFailures(schema, new JSONObject(command));
    }

    public static List<String> findValidationFailures(final MpsCommandParameter parameter) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("mps-command-parameter.json"));
        return findValidationFailures(schema, new JSONObject(parameter));
    }

    public static List<String> findValidationFailures(final Plan plan) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("plan.json"));
        return findValidationFailures(schema, new JSONObject(plan));
    }

    public static List<String> findValidationFailures(final PlanDetail plan) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("plan-detail.json"));
        return findValidationFailures(schema, new JSONObject(plan));
    }

    public static List<String> findValidationFailures(Schedule schedule) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("schedule.json"));
        return findValidationFailures(schema, new JSONObject(schedule));
    }
}
