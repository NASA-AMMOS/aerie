package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.merlin.protocol.ValueMapper;

public final class BananaValueMappers {
  public static ValueMapper<Configuration> configuration() {
    return new ConfigurationValueMapper();
  }
}
