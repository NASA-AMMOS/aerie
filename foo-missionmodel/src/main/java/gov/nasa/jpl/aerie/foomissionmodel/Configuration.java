package gov.nasa.jpl.aerie.foomissionmodel;

public class Configuration {

  public final Double sinkRate;

  public Configuration(final Double sinkRate) {
    this.sinkRate = sinkRate;
  }

  public Configuration() {
    this(0.5);
  }
}
