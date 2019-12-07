package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.http;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.controllers.IAdaptationController;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.AdaptationAccessException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.AdaptationContractException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.ValidationException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchAdaptationException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchActivityTypeException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.*;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import io.javalin.Javalin;
import io.javalin.core.util.FileUtil;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.Json;
import javax.json.JsonValue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;

public final class AdaptationBindings {
    private final IAdaptationController appController;

    public AdaptationBindings(final IAdaptationController appController) {
        this.appController = appController;
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
                        });
                    });
                });
            });
        });

        javalin.exception(ValidationException.class, (ex, ctx) -> ctx
            .status(400)
            .result(Json.createArrayBuilder(ex.getValidationErrors()).build().toString())
            .contentType("application/json")
        ).exception(NoSuchAdaptationException.class, (ex, ctx) -> ctx
            .status(404)
        ).exception(NoSuchActivityTypeException.class, (ex, ctx) -> ctx
            .status(404)
        ).exception(AdaptationContractException.class, (ex, ctx) -> ctx
            .result(Json.createObjectBuilder().add("message", ex.getMessage()).build().toString())
            .status(500)
        ).exception(AdaptationAccessException.class, (ex, ctx) -> {
            System.err.println(ex.getMessage());
            ctx.status(500);
        });
    }

    private void getAdaptations(final Context ctx) {
        final Map<String, Adaptation> adaptations = this.appController
                .getAdaptations()
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

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

        final String adaptationId = this.appController.addAdaptation(adaptation);

        ctx.status(201)
                .header("Location", "/adaptations/" + adaptationId)
                .result(ResponseSerializers.serializedCreatedId(adaptationId).toString())
                .contentType("application/json");
    }

    private void getAdaptation(final Context ctx) throws NoSuchAdaptationException {
        final String adaptationId = ctx.pathParam("adaptationId");

        final Adaptation adaptation = this.appController.getAdaptationById(adaptationId);

        final JsonValue response = ResponseSerializers.serializeAdaptation(adaptation);
        ctx.result(response.toString()).contentType("application/json");
    }

    private void deleteAdaptation(final Context ctx) throws NoSuchAdaptationException {
        final String adaptationId = ctx.pathParam("adaptationId");

        this.appController.removeAdaptation(adaptationId);
    }

    private void getActivityTypes(final Context ctx) throws NoSuchAdaptationException, AdaptationContractException {
        final String adaptationId = ctx.pathParam("adaptationId");

        final Map<String, ActivityType> activityTypes = this.appController.getActivityTypes(adaptationId);

        final JsonValue response = ResponseSerializers.serializeActivityTypes(activityTypes);
        ctx.result(response.toString()).contentType("application/json");
    }

    private void getActivityType(final Context ctx) throws NoSuchAdaptationException, NoSuchActivityTypeException, AdaptationContractException {
        final String adaptationId = ctx.pathParam("adaptationId");
        final String activityTypeId = ctx.pathParam("activityTypeId");

        final ActivityType activityType = this.appController.getActivityType(adaptationId, activityTypeId);

        final JsonValue response = ResponseSerializers.serializeActivityType(activityType);
        ctx.result(response.toString()).contentType("application/json");
    }

}
