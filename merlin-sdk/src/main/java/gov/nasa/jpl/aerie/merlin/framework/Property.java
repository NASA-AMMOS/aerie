package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.DelimitedDynamics;

public interface Property<Model, Dynamics> {
  DelimitedDynamics<Dynamics> ask(Model model);
}
