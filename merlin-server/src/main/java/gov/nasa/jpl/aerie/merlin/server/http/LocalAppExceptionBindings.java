package gov.nasa.jpl.aerie.merlin.server.http;

import gov.nasa.jpl.aerie.merlin.server.services.LocalAdaptationService;
import io.javalin.Javalin;
import io.javalin.core.plugin.Plugin;

public final class LocalAppExceptionBindings implements Plugin {
    @Override
    public void apply(final Javalin javalin) {
        javalin.exception(LocalAdaptationService.AdaptationLoadException.class, (ex, ctx) -> ctx
            .status(500)
            .result(ResponseSerializers.serializeAdaptationLoadException(ex).toString())
            .contentType("application/json"));
    }
}
