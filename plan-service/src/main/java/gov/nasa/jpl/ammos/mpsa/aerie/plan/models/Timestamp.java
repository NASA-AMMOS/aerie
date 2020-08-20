package gov.nasa.jpl.ammos.mpsa.aerie.plan.models;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Timestamp {

  private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("uuuu-DDD'T'HH:mm:ss[.n]");
  private ZonedDateTime time;

  private Timestamp(String timestamp) throws DateTimeParseException {
    time = LocalDateTime.parse(timestamp, format).atZone(ZoneOffset.UTC);
  }

  public static Timestamp fromString(String timestamp) throws DateTimeParseException {
    return new Timestamp(timestamp);
  }

  public Instant toInstant() {
    return time.toInstant();
  }

  public String toString() {
    return format.format(time);
  }

  public boolean equals(final Object object) {
    if (object.getClass() != Timestamp.class) {
      return false;
    }

    final Timestamp other = (Timestamp)object;
    return this.time.equals(other.time);
  }
}
