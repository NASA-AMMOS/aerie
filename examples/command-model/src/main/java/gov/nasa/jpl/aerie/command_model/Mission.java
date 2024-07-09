package gov.nasa.jpl.aerie.command_model;

import gov.nasa.jpl.aerie.command_model.power.Power;
import gov.nasa.jpl.aerie.command_model.sequencing.Sequencing;
import gov.nasa.jpl.aerie.command_model.sequencing.command_dictionary.DummyCommandDictionary;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar.ErrorBehavior;

public final class Mission {
    public final Sequencing sequencing;
    public final Power power;

    public Mission(gov.nasa.jpl.aerie.merlin.framework.Registrar registrar$, Configuration configuration) {
        var registrar = new Registrar(registrar$, ErrorBehavior.Throw);
        this.sequencing = new Sequencing(new DummyCommandDictionary(configuration.defaultCommandDuration), registrar);
        this.power = new Power(sequencing, registrar);
    }
}
