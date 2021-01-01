package gov.nasa.jpl.ammos.mpsa.aerie.services.adaptation.http;

import gov.nasa.jpl.ammos.mpsa.aerie.services.adaptation.models.AdaptationFacade;
import io.javalin.Javalin;
import io.javalin.core.plugin.Plugin;

public final class AdaptationExceptionBindings implements Plugin {
    @Override
    public void apply(final Javalin javalin) {
        javalin.exception(AdaptationFacade.AdaptationContractException.class, (ex, ctx) -> ctx
            .status(500)
            .result(ResponseSerializers.serializeAdaptationContractException(ex).toString())
            .contentType("application/json"));
    }
}
