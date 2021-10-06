package gov.nasa.jpl.aerie.scheduler.aerie;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import gov.nasa.jpl.aerie.scheduler.Duration;

import java.io.IOException;

class DurationTypeAdapter extends TypeAdapter<Duration> {
  @Override
  public void write(JsonWriter out, Duration value) throws IOException {
    out.value(value.toMicroseconds());
  }

  @Override
  public Duration read(JsonReader in) throws IOException {
    return Duration.ofMicroseconds(in.nextLong());
  }
}
