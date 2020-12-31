package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.DelimitedDynamics;

public interface Property<Model, Dynamics> {
  DelimitedDynamics<Dynamics> ask(Model model);
}
