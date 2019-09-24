package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.http;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.controllers.IAdaptationController;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.AdaptationAccessException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.ValidationException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchAdaptationException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchActivityTypeException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.*;
import io.javalin.Javalin;
import io.javalin.core.util.FileUtil;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
                            path("parameters", () -> {
                               get(this::getActivityTypeParameters);
                            });
                        });
                    });
                });
            });
        });

        javalin.exception(JsonbException.class, (ex, ctx) -> {
            ctx.status(400).result(JsonbBuilder.create().toJson(List.of(ex.getMessage())));
        }).exception(ValidationException.class, (ex, ctx) -> {
            ctx.status(400).result(JsonbBuilder.create().toJson(ex.getValidationErrors()));
        }).exception(NoSuchAdaptationException.class, (ex, ctx) -> {
            ctx.status(404);
        }).exception(NoSuchActivityTypeException.class, (ex, ctx) -> {
            ctx.status(404);
        }).exception(AdaptationAccessException.class, (ex, ctx) -> {
            System.err.println(ex.getMessage());
            ctx.status(500);
        });
    }

    private void getAdaptations(final Context ctx) {
        final Map<String, ResponseAdaptation> adaptations = this.appController
                .getAdaptations()
                .collect(Collectors.toMap(Pair::getKey, p -> new ResponseAdaptation(p.getValue())));

        ctx.result(JsonbBuilder.create().toJson(adaptations)).contentType("application/json");
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

        ctx.status(201).result(adaptationId);
    }

    private void getAdaptation(final Context ctx) throws NoSuchAdaptationException {
        final String adaptationId = ctx.pathParam("adaptationId");

        final ResponseAdaptation adaptation = new ResponseAdaptation(this.appController.getAdaptationById(adaptationId));

        ctx.result(JsonbBuilder.create().toJson(adaptation)).contentType("application/json");
    }

    private void deleteAdaptation(final Context ctx) throws NoSuchAdaptationException {
        final String adaptationId = ctx.pathParam("adaptationId");

        this.appController.removeAdaptation(adaptationId);
    }

    private void getActivityTypes(final Context ctx) throws NoSuchAdaptationException {
        final String adaptationId = ctx.pathParam("adaptationId");

        final Map<String, ActivityType> activityTypes = this.appController
                .getActivityTypes(adaptationId)
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

        ctx.result(JsonbBuilder.create().toJson(activityTypes)).contentType("application/json");
    }

    private void getActivityType(final Context ctx) throws NoSuchAdaptationException, NoSuchActivityTypeException {
        final String adaptationId = ctx.pathParam("adaptationId");
        final String activityTypeId = ctx.pathParam("activityTypeId");

        final ActivityType activityType = this.appController
                .getActivityType(adaptationId, activityTypeId);

        ctx.result(JsonbBuilder.create().toJson(activityType)).contentType("application/json");
    }

    private void getActivityTypeParameters(final Context ctx) throws NoSuchAdaptationException, NoSuchActivityTypeException {
        final String adaptationId = ctx.pathParam("adaptationId");
        final String activityTypeId = ctx.pathParam("activityTypeId");

        final List<ActivityTypeParameter> activityTypeParameters = this.appController
                .getActivityTypeParameters(adaptationId, activityTypeId)
                .collect(Collectors.toList());

        ctx.result(JsonbBuilder.create().toJson(activityTypeParameters)).contentType("application/json");
    }
}
