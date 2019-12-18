package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.http;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.app.LocalApp;
import io.javalin.Javalin;
import io.javalin.core.plugin.Plugin;

public final class LocalAppExceptionBindings implements Plugin {
    @Override
    public void apply(final Javalin javalin) {
        javalin.exception(LocalApp.AdaptationLoadException.class, (ex, ctx) -> ctx
            .status(500)
            .result(ResponseSerializers.serializeAdaptationLoadException(ex).toString())
            .contentType("application/json"));
    }
}
