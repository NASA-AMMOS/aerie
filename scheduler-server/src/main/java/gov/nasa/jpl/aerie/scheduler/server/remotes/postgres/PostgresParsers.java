package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;

import static gov.nasa.jpl.aerie.json.BasicParsers.mapP;
import static gov.nasa.jpl.aerie.scheduler.server.http.SerializedValueJsonParser.serializedValueP;

public final class PostgresParsers {
  public static final JsonParser<Map<String, SerializedValue>> simulationArgumentsP = mapP(serializedValueP);
}
