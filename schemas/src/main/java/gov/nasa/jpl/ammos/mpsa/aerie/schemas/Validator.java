package gov.nasa.jpl.ammos.mpsa.aerie.schemas;

import java.io.InputStream;
import java.io.IOException;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaClient;
import org.everit.json.schema.ValidationException;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.net.URL;

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

    private static boolean validate(final Schema schema, final JSONObject object) {
        try {
            schema.validate(object);
            return true;
        } catch (final ValidationException ex) {
            return false;
        }
    }

    public static boolean validate(final ActivityInstance instance) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("activity-instance.json"));
        return validate(schema, new JSONObject(instance));
    }

    public static boolean validate(final ActivityInstanceConstraint constraint) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("activity-instance-constraint.json"));
        return validate(schema, new JSONObject(constraint));
    }

    public static boolean validate(final ActivityInstanceParameter parameter) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("activity-instance-parameter.json"));
        return validate(schema, new JSONObject(parameter));
    }

    public static boolean validate(final ActivityType type) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("activity-type.json"));
        return validate(schema, new JSONObject(type));
    }

    public static boolean validate(final ActivityTypeParameter parameter) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("activity-type-parameter.json"));
        return validate(schema, new JSONObject(parameter));
    }

    public static boolean validate(final Adaptation adaptation) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("adaptation.json"));
        return validate(schema, new JSONObject(adaptation));
    }

    public static boolean validate(final AmqpMessage adaptation) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("amqp-message.json"));
        return validate(schema, new JSONObject(adaptation));
    }

    public static boolean validate(final AmqpMessageData adaptation) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("amqp-message-data.json"));
        return validate(schema, new JSONObject(adaptation));
    }

    public static boolean validate(final AmqpMessageTypeEnum adaptation) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("amqp-message-type-enum.json"));
        return validate(schema, new JSONObject(adaptation));
    }

    public static boolean validate(final CommandDictionary dictionary) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("command-dictionary.json"));
        return validate(schema, new JSONObject(dictionary));
    }

    public static boolean validate(final MpsCommand command) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("mps-command.json"));
        return validate(schema, new JSONObject(command));
    }

    public static boolean validate(final MpsCommandParameter parameter) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("mps-command-parameter.json"));
        return validate(schema, new JSONObject(parameter));
    }

    public static boolean validate(final Plan plan) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("plan.json"));
        return validate(schema, new JSONObject(plan));
    }

    public static boolean validate(final PlanDetail plan) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("plan-detail.json"));
        return validate(schema, new JSONObject(plan));
    }

    public static boolean validate(Schedule schedule) throws IOException {
        final Schema schema = loadSchema(Validator.class.getResource("schedule.json"));
        return validate(schema, new JSONObject(schedule));
    }
}
