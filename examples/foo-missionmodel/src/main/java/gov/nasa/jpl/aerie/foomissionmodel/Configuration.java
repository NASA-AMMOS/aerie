package gov.nasa.jpl.aerie.foomissionmodel;

import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;

public final class Configuration {

  @Export.Parameter public Double sinkRate;

  public Configuration(final Double sinkRate) {
    this.sinkRate = sinkRate;
  }

  public Configuration() {
    this(0.5);
  }
}
