package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.http;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.app.App;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.AdaptationAccessException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.UnconstructableActivityInstanceException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.ValidationException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchAdaptationException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchActivityTypeException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.*;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import io.javalin.Javalin;
import io.javalin.core.util.FileUtil;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;

import javax.json.Json;
import javax.json.JsonValue;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private void postAdaptation(final Context ctx) throws ValidationException, IOException {
        final UploadedFile uploadedFile = ctx.uploadedFile("file");
        if (uploadedFile == null)
            throw new ValidationException("No adaptation JAR provided", new ArrayList<>());

        final NewAdaptation adaptation = new NewAdaptation();
        adaptation.name = ctx.formParam("name");
        adaptation.version = ctx.formParam("version");
        adaptation.mission = ctx.formParam("mission");
        adaptation.owner = ctx.formParam("owner");

        final Path path = Files.createTempFile(uploadedFile.getFilename(), "");
        FileUtil.streamToFile(uploadedFile.getContent(), path.toString());
        adaptation.path = path;

        final String adaptationId = this.app.addAdaptation(adaptation);

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

    private void getActivityTypes(final Context ctx) throws NoSuchAdaptationException, Adaptation.AdaptationContractException {
        final String adaptationId = ctx.pathParam("adaptationId");

        final Map<String, ActivityType> activityTypes = this.app.getActivityTypes(adaptationId);

        final JsonValue response = ResponseSerializers.serializeActivityTypes(activityTypes);
        ctx.result(response.toString()).contentType("application/json");
    }

    private void getActivityType(final Context ctx) throws NoSuchAdaptationException, NoSuchActivityTypeException, Adaptation.AdaptationContractException {
        final String adaptationId = ctx.pathParam("adaptationId");
        final String activityTypeId = ctx.pathParam("activityTypeId");

        final ActivityType activityType = this.app.getActivityType(adaptationId, activityTypeId);

        final JsonValue response = ResponseSerializers.serializeActivityType(activityType);
        ctx.result(response.toString()).contentType("application/json");
    }

    private void validateActivityParameters(final Context ctx)
        throws InvalidEntityException, NoSuchAdaptationException, Adaptation.AdaptationContractException, NoSuchActivityTypeException,
        UnconstructableActivityInstanceException
    {
        final String adaptationId = ctx.pathParam("adaptationId");
        final String activityTypeId = ctx.pathParam("activityTypeId");

        final JsonValue requestJson = Json.createReader(new StringReader(ctx.body())).readValue();
        final Map<String, SerializedParameter> activityParameters = RequestDeserializers.deserializeActivityParameterMap(requestJson);
        final SerializedActivity serializedActivity = new SerializedActivity(activityTypeId, activityParameters);

        final Activity<?> activity = this.app.instantiateActivity(adaptationId, serializedActivity);

        final List<String> failures = activity.validateParameters();
        if (failures == null) {
            // TODO: The HTTP binding layer is a poor place to put knowledge about the adaptation contract.
            //   Move this logic somewhere better.
            throw new Adaptation.AdaptationContractException(activity.getClass().getName() + ".validateParameters() returned null");
        }

        final JsonValue response = ResponseSerializers.serializeFailureList(failures);

        ctx.result(response.toString()).contentType("application/json");
    }
}
