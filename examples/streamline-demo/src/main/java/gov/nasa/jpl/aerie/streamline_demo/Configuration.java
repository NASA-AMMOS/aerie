package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Parameter;

public final class Configuration {
  @Parameter
  public boolean traceResources = false;

  @Parameter
  public double approximationTolerance = 1e-2;

}
