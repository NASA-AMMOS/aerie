package gov.nasa.jpl.aerie.configwithdefaults;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;

public final class Configuration {

  @ActivityType.Parameter
  public Double sinkRate;

  public Configuration(final Double sinkRate) {
    this.sinkRate = sinkRate;
  }

  public Configuration() {
    this(0.5);
  }
}
