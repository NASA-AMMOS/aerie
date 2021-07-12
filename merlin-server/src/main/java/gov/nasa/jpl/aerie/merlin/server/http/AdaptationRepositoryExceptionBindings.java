package gov.nasa.jpl.aerie.merlin.server.http;

import gov.nasa.jpl.aerie.merlin.server.remotes.MongoAdaptationRepository;
import io.javalin.Javalin;
import io.javalin.core.plugin.Plugin;

public final class AdaptationRepositoryExceptionBindings implements Plugin {
    @Override
    public void apply(final Javalin javalin) {
        javalin.exception(MongoAdaptationRepository.AdaptationAccessException.class, (ex, ctx) -> ctx
            .status(500)
            .result(ResponseSerializers.serializeAdaptationAccessException(ex).toString())
            .contentType("application/json"));
    }
}
