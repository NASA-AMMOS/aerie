package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.http;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.Adaptation;
import io.javalin.Javalin;
import io.javalin.core.plugin.Plugin;

public final class AdaptationExceptionBindings implements Plugin {
    @Override
    public void apply(final Javalin javalin) {
        javalin.exception(Adaptation.AdaptationContractException.class, (ex, ctx) -> ctx
            .status(500)
            .result(ResponseSerializers.serializeAdaptationContractException(ex).toString())
            .contentType("application/json"));
    }
}
