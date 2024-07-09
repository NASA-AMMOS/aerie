package gov.nasa.jpl.aerie.command_model;

import gov.nasa.jpl.aerie.command_model.sequencing.Sequencing;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar.ErrorBehavior;

public final class Mission {
    public final Sequencing sequencing;

    public Mission(Configuration configuration, gov.nasa.jpl.aerie.merlin.framework.Registrar registrar$) {
        var registrar = new Registrar(registrar$, ErrorBehavior.Throw);
        this.sequencing = new Sequencing(configuration.numberOfSequenceEngines, registrar);
    }
}
