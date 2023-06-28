package gov.nasa.jpl.aerie.merlin.server.http;

import gov.nasa.jpl.aerie.merlin.server.services.LocalMissionModelService;
import io.javalin.Javalin;
import io.javalin.plugin.Plugin;

import javax.json.Json;

public final class LocalAppExceptionBindings implements Plugin {
    @Override
    public void apply(final Javalin javalin) {
        javalin.exception(LocalMissionModelService.MissionModelLoadException.class, (ex, ctx) -> ctx
            .status(500)
            .result(ResponseSerializers.serializeMissionModelLoadException(ex).toString())
            .contentType("application/json"));
        javalin.exception(Unauthorized.class, (ex, ctx) -> ctx
            .status(401)
            .result(Json.createObjectBuilder()
                        .add("message", "Unauthorized: " + ex.getMessage())
                        .build().toString())
            .contentType("application/json"));
    }
}
