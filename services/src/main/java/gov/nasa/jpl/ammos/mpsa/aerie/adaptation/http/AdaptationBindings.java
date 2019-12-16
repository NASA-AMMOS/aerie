package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.http;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.app.App;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.AdaptationAccessException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.UnconstructableActivityInstanceException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.ValidationException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchAdaptationException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchActivityTypeException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.*;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.utilities.AdaptationLoader;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;

import javax.json.Json;
import javax.json.JsonValue;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;

public final class AdaptationBindings {
    private final App app;

    public AdaptationBindings(final App app) {
        this.app = app;
    }

    public void registerRoutes(final Javalin javalin) {
        javalin.routes(() -> {
            path("adaptations", () -> {
                get(this::getAdaptations);
                post(this::postAdaptation);
                path(":adaptationId", () -> {
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
        });

        javalin.exception(ValidationException.class, (ex, ctx) -> ctx
            .status(400)
            .result(ResponseSerializers.serializeValidationException(ex).toString())
            .contentType("application/json")
        ).exception(InvalidEntityException.class, (ex, ctx) -> ctx
            .status(400)
            .result(ResponseSerializers.serializeInvalidEntityException(ex).toString())
            .contentType("application/json")
        ).exception(UnconstructableActivityInstanceException.class, (ex, ctx) -> ctx
            .status(400)
            .result(ResponseSerializers.serializeUnconstructableActivityInstanceException(ex).toString())
            .contentType("application/json")
        ).exception(NoSuchAdaptationException.class, (ex, ctx) -> ctx
            .status(404)
        ).exception(NoSuchActivityTypeException.class, (ex, ctx) -> ctx
            .status(404)
        ).exception(Adaptation.AdaptationContractException.class, (ex, ctx) -> ctx
            .status(500)
            .result(ResponseSerializers.serializeAdaptationContractException(ex).toString())
            .contentType("application/json")
        ).exception(AdaptationAccessException.class, (ex, ctx) -> {
            System.err.println(ex.getMessage());
            ctx.status(500);
        });
    }

    private void getAdaptations(final Context ctx) {
        final Map<String, AdaptationJar> adaptations = this.app.getAdaptations();

        final JsonValue response = ResponseSerializers.serializeAdaptations(adaptations);
        ctx.result(response.toString()).contentType("application/json");
    }

    private void postAdaptation(final Context ctx) throws ValidationException {
        final NewAdaptation newAdaptation = readNewAdaptation(ctx);

        final String adaptationId;
        try {
            adaptationId = this.app.addAdaptation(newAdaptation);
        } catch (final App.AdaptationRejectedException ex) {
            throw new ValidationException("adaptation rejected", List.of(ex.getMessage()));
        }

        ctx.status(201)
                .header("Location", "/adaptations/" + adaptationId)
                .result(ResponseSerializers.serializedCreatedId(adaptationId).toString())
                .contentType("application/json");
    }

    private void getAdaptation(final Context ctx) throws NoSuchAdaptationException {
        final String adaptationId = ctx.pathParam("adaptationId");

        final AdaptationJar adaptationJar = this.app.getAdaptationById(adaptationId);

        final JsonValue response = ResponseSerializers.serializeAdaptation(adaptationJar);
        ctx.result(response.toString()).contentType("application/json");
    }

    private void deleteAdaptation(final Context ctx) throws NoSuchAdaptationException {
        final String adaptationId = ctx.pathParam("adaptationId");

        this.app.removeAdaptation(adaptationId);
    }

    private void getActivityTypes(final Context ctx) throws NoSuchAdaptationException, Adaptation.AdaptationContractException, AdaptationLoader.AdaptationLoadException {
        final String adaptationId = ctx.pathParam("adaptationId");

        final Map<String, ActivityType> activityTypes = this.app.getActivityTypes(adaptationId);

        final JsonValue response = ResponseSerializers.serializeActivityTypes(activityTypes);
        ctx.result(response.toString()).contentType("application/json");
    }

    private void getActivityType(final Context ctx) throws NoSuchAdaptationException, NoSuchActivityTypeException, Adaptation.AdaptationContractException, AdaptationLoader.AdaptationLoadException {
        final String adaptationId = ctx.pathParam("adaptationId");
        final String activityTypeId = ctx.pathParam("activityTypeId");

        final ActivityType activityType = this.app.getActivityType(adaptationId, activityTypeId);

        final JsonValue response = ResponseSerializers.serializeActivityType(activityType);
        ctx.result(response.toString()).contentType("application/json");
    }

    private void validateActivityParameters(final Context ctx)
        throws InvalidEntityException, NoSuchAdaptationException, Adaptation.AdaptationContractException, NoSuchActivityTypeException,
        UnconstructableActivityInstanceException, AdaptationLoader.AdaptationLoadException
    {
        final String adaptationId = ctx.pathParam("adaptationId");
        final String activityTypeId = ctx.pathParam("activityTypeId");

        final JsonValue requestJson = Json.createReader(new StringReader(ctx.body())).readValue();
        final Map<String, SerializedParameter> activityParameters = RequestDeserializers.deserializeActivityParameterMap(requestJson);
        final SerializedActivity serializedActivity = new SerializedActivity(activityTypeId, activityParameters);

        final List<String> failures = this.app.validateActivityParameters(adaptationId, serializedActivity);

        final JsonValue response = ResponseSerializers.serializeFailureList(failures);
        ctx.result(response.toString()).contentType("application/json");
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
}
