package gov.nasa.jpl.ammos.mpsa.aerie.simulation;

import javax.json.JsonObject;
import java.util.Objects;

public final class AppConfiguration {
  public final int http_port;

  public AppConfiguration(final int http_port) {
    this.http_port = http_port;
  }

  static public AppConfiguration parseProperties(final JsonObject json) {
    final int http_port = json.getInt("http_port");
    return new AppConfiguration(http_port);
  }

  // SAFETY: When equals is overridden, so too must hashCode.
  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof AppConfiguration)) return false;
    final AppConfiguration other = (AppConfiguration)o;

    return this.http_port == other.http_port;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.http_port);
  }

  @Override
  public String toString() {
    return "AppConfiguration {\n" +
        "  http_port = " + this.http_port + ",\n" +
        "}";
  }
}
