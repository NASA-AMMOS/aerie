package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.http;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.app.App;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.app.CreateSimulationMessage;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.AdaptationJar;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.NewAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.json.JsonParser;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedValue;
import io.javalin.Javalin;
import io.javalin.core.plugin.Plugin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;

import javax.json.Json;
import javax.json.stream.JsonParsingException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static gov.nasa.jpl.ammos.mpsa.aerie.adaptation.http.MerlinParsers.createSimulationMessageP;
import static gov.nasa.jpl.ammos.mpsa.aerie.adaptation.http.MerlinParsers.serializedParameterP;
import static gov.nasa.jpl.ammos.mpsa.aerie.json.BasicParsers.mapP;
import static io.javalin.apibuilder.ApiBuilder.before;
import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.head;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;

/**
 * Lift native Java agents into an HTTP-oriented service.
 *
 * The role of an {@code AdaptationBindings} object is to faithfully translate between the request/response protocol
 * of HTTP and the call/return/throw protocol of a Java method. Put differently, {@code AdaptationBindings} <i>lifts</i>
 * an object with native Java endpoints (methods) into an HTTP service with HTTP-oriented endpoints. This entails
 * translating HTTP request bodies into native Java domain objects, and translating native Java domain objects
 * (including thrown exceptions) into HTTP response bodies.
 *
 * The object to be lifted must implement the {@link App} interface. Formally, it is
 * this interface that the {@code AdaptationBindings} class lifts into the domain of HTTP; an object implementing
 * this interface defines the action to take for each HTTP request in an HTTP-independent way.
 */
public final class AdaptationBindings implements Plugin {
    private final App app;

    public AdaptationBindings(final App app) {
        this.app = app;
    }

    @Override
    public void apply(final Javalin javalin) {
        javalin.routes(() -> {
            path("adaptations", () -> {
                before(ctx -> ctx.contentType("application/json"));

                get(this::getAdaptations);
                post(this::postAdaptation);
                path(":adaptationId", () -> {
                    head(this::doesAdaptationExist);
                    get(this::getAdaptation);
                    delete(this::deleteAdaptation);
                    path("activities", () -> {
                        get(this::getActivityTypes);
                        path(":activityTypeId", () -> {
                            get(this::getActivityType);
                            path("validate", () -> {
                                post(this::validateActivityParameters);
                            });
                        });
                    });
                });
            });

            path("simulations", () -> {
                before(ctx -> ctx.contentType("application/json"));

                post(this::runSimulation);
            });
        });
    }

    private void getAdaptations(final Context ctx) {
        final Map<String, AdaptationJar> adaptations = this.app.getAdaptations();

        ctx.result(ResponseSerializers.serializeAdaptations(adaptations).toString());
    }

    private void postAdaptation(final Context ctx) {
        try {
            final NewAdaptation newAdaptation = readNewAdaptation(ctx);

            final String adaptationId = this.app.addAdaptation(newAdaptation);

            ctx.status(201)
                .header("Location", "/adaptations/" + adaptationId)
                .result(ResponseSerializers.serializedCreatedId(adaptationId).toString());
        } catch (final App.AdaptationRejectedException ex) {
            ctx.status(400).result(ResponseSerializers.serializeAdaptationRejectedException(ex).toString());
        } catch (final ValidationException ex) {
            ctx.status(400).result(ResponseSerializers.serializeValidationException(ex).toString());
        }
    }

    private void doesAdaptationExist(final Context ctx) {
        try {
            final String adaptationId = ctx.pathParam("adaptationId");

            this.app.getAdaptationById(adaptationId);

            ctx.status(200);
        } catch (final App.NoSuchAdaptationException ex) {
            ctx.status(404);
        }
    }

    private void getAdaptation(final Context ctx) {
        try {
            final String adaptationId = ctx.pathParam("adaptationId");

            final AdaptationJar adaptationJar = this.app.getAdaptationById(adaptationId);

            ctx.result(ResponseSerializers.serializeAdaptation(adaptationJar).toString());
        } catch (final App.NoSuchAdaptationException ex) {
            ctx.status(404);
        }
    }

    private void deleteAdaptation(final Context ctx) {
        try {
            final String adaptationId = ctx.pathParam("adaptationId");

            this.app.removeAdaptation(adaptationId);
        } catch (final App.NoSuchAdaptationException ex) {
            ctx.status(404);
        }
    }

    private void getActivityTypes(final Context ctx) {
        try {
            final String adaptationId = ctx.pathParam("adaptationId");

            final Map<String, ActivityType> activityTypes = this.app.getActivityTypes(adaptationId);

            ctx.result(ResponseSerializers.serializeActivityTypes(activityTypes).toString());
        } catch (final App.NoSuchAdaptationException ex) {
            ctx.status(404);
        }
    }

    private void getActivityType(final Context ctx) {
        try {
            final String adaptationId = ctx.pathParam("adaptationId");
            final String activityTypeId = ctx.pathParam("activityTypeId");

            final ActivityType activityType = this.app.getActivityType(adaptationId, activityTypeId);

            ctx.result(ResponseSerializers.serializeActivityType(activityType).toString());
        } catch (final App.NoSuchAdaptationException | App.NoSuchActivityTypeException ex) {
            ctx.status(404);
        }
    }

    private void validateActivityParameters(final Context ctx) {
        try {
            final String adaptationId = ctx.pathParam("adaptationId");
            final String activityTypeId = ctx.pathParam("activityTypeId");

            final Map<String, SerializedValue> activityParameters = parseJson(ctx.body(), mapP(serializedParameterP));
            final SerializedActivity serializedActivity = new SerializedActivity(activityTypeId, activityParameters);

            final List<String> failures = this.app.validateActivityParameters(adaptationId, serializedActivity);

            ctx.result(ResponseSerializers.serializeFailureList(failures).toString());
        } catch (final App.NoSuchAdaptationException ex) {
            ctx.status(404);
        } catch (final InvalidEntityException ex) {
            ctx.status(400).result(ResponseSerializers.serializeInvalidEntityException(ex).toString());
        }
    }

    private void runSimulation(final Context ctx) {
        try {
            final CreateSimulationMessage message = parseJson(ctx.body(), createSimulationMessageP);

            final var results = this.app.runSimulation(message);

            ctx.result(ResponseSerializers.serializeSimulationResults(results).toString());
        } catch (final JsonParsingException ex) {
            // Request entity is not valid JSON.
            // TODO: report this failure with a better response body
            ctx.status(400).result(Json.createObjectBuilder().add("kind", "invalid-json").build().toString());
        } catch (final InvalidEntityException ex) {
            // Request entity does not have the expected shape.
            ctx.status(400).result(ResponseSerializers.serializeInvalidEntityException(ex).toString());
        } catch (final App.NoSuchAdaptationException ex) {
            // The requested adaptation does not exist.
            ctx.status(404);
        } catch (final Adaptation.UnconstructableActivityInstanceException | Adaptation.NoSuchActivityTypeException e) {
            // The adaptation could not instantiate the provided activities.
            // TODO: report these failures with a better response body
            ctx.status(400).result(Json.createObjectBuilder().add("kind", "invalid-activities").build().toString());
        }
      }

    private NewAdaptation readNewAdaptation(final Context ctx) throws ValidationException {
        final List<String> validationErrors = new ArrayList<>();

        String name = null;
        String version = null;
        String mission = null;
        String owner = null;
        UploadedFile uploadedFile = null;
        {
            for (final var formParam : ctx.formParamMap().entrySet()) {
                final String key = formParam.getKey();
                final List<String> values = formParam.getValue();

                switch (key) {
                    case "name":
                        if (values.isEmpty()) validationErrors.add("Zero values provided for key `" + key + "`; expected one.");
                        else if (values.size() > 1) validationErrors.add("Multiple values provided for key `" + key + "`; expected one.");
                        else name = values.get(0);
                        break;
                    case "version":
                        if (values.isEmpty()) validationErrors.add("Zero values provided for key `" + key + "`; expected one.");
                        else if (values.size() > 1) validationErrors.add("Multiple values provided for key `" + key + "`; expected one.");
                        else version = values.get(0);
                        break;
                    case "mission":
                        if (values.isEmpty()) validationErrors.add("Zero values provided for key `" + key + "`; expected one.");
                        else if (values.size() > 1) validationErrors.add("Multiple values provided for key `" + key + "`; expected one.");
                        else mission = values.get(0);
                        break;
                    case "owner":
                        if (values.isEmpty()) validationErrors.add("Zero values provided for key `" + key + "`; expected one.");
                        else if (values.size() > 1) validationErrors.add("Multiple values provided for key `" + key + "`; expected one.");
                        else owner = values.get(0);
                        break;
                    case "file":
                        if (values.size() > 0) {
                            validationErrors.add("Key `" + key + "` does not contain an upload file (needs HTTP multipart `file` parameter).");
                        } else {
                            uploadedFile = ctx.uploadedFile("file");
                            if (uploadedFile == null) validationErrors.add("Key `" + key + "` does not contain an upload file.");
                        }
                        break;
                    default:
                        validationErrors.add("Unknown key `" + key + "`");
                        break;
                }
            }

            if (name == null) validationErrors.add("No value provided for key `name`");
            if (version == null) validationErrors.add("No value provided for key `version`");
            if (mission == null) validationErrors.add("No value provided for key `mission`");
            if (owner == null) validationErrors.add("No value provided for key `owner`");
            if (uploadedFile == null) validationErrors.add("No upload file provided for key `file`");

            // TODO: Throw an InvalidEntityException instead, once it supports capturing fine-grained information
            //   about where in the entity body the failures occur.
            if (!validationErrors.isEmpty()) throw new ValidationException("Validation failed", validationErrors);
        }

        return NewAdaptation.builder()
            .setName(ctx.formParam("name"))
            .setVersion(ctx.formParam("version"))
            .setMission(ctx.formParam("mission"))
            .setOwner(ctx.formParam("owner"))
            .setJarSource(uploadedFile.getContent())
            .build();
    }

    private <T> T parseJson(final String subject, final JsonParser<T> parser) throws InvalidEntityException {
        final var requestJson = Json.createReader(new StringReader(subject)).readValue();
        return parser.parse(requestJson).getSuccessOrThrow(() -> new InvalidEntityException());
    }
}
