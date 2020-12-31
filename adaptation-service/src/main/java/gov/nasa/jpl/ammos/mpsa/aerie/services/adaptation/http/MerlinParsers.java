package gov.nasa.jpl.ammos.mpsa.aerie.services.adaptation.http;

import gov.nasa.jpl.ammos.mpsa.aerie.services.adaptation.app.CreateSimulationMessage;
import gov.nasa.jpl.ammos.mpsa.aerie.json.BasicParsers;
import gov.nasa.jpl.ammos.mpsa.aerie.json.JsonParseResult;
import gov.nasa.jpl.ammos.mpsa.aerie.json.JsonParser;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.time.Duration;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.JsonString;
import javax.json.JsonValue.ValueType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Collections;

import static gov.nasa.jpl.ammos.mpsa.aerie.json.BasicParsers.boolP;
import static gov.nasa.jpl.ammos.mpsa.aerie.json.BasicParsers.chooseP;
import static gov.nasa.jpl.ammos.mpsa.aerie.json.BasicParsers.doubleP;
import static gov.nasa.jpl.ammos.mpsa.aerie.json.BasicParsers.listP;
import static gov.nasa.jpl.ammos.mpsa.aerie.json.BasicParsers.longP;
import static gov.nasa.jpl.ammos.mpsa.aerie.json.BasicParsers.mapP;
import static gov.nasa.jpl.ammos.mpsa.aerie.json.BasicParsers.nullP;
import static gov.nasa.jpl.ammos.mpsa.aerie.json.BasicParsers.productP;
import static gov.nasa.jpl.ammos.mpsa.aerie.json.BasicParsers.recursiveP;
import static gov.nasa.jpl.ammos.mpsa.aerie.json.BasicParsers.stringP;
import static gov.nasa.jpl.ammos.mpsa.aerie.json.Uncurry.uncurry3;
import static gov.nasa.jpl.ammos.mpsa.aerie.json.Uncurry.uncurry5;

public abstract class MerlinParsers {
  private MerlinParsers() {}

  // This builder must be used to get optional subsecond values
  // See: https://stackoverflow.com/questions/30090710/java-8-datetimeformatter-parsing-for-optional-fractional-seconds-of-varying-sign
  private static final DateTimeFormatter timestampFormat =
      new DateTimeFormatterBuilder().appendPattern("uuuu-DDD'T'HH:mm:ss")
                                    .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true).toFormatter();

  public static final JsonParser<Instant> instantP = json -> {
    if (!(json instanceof JsonString)) return JsonParseResult.failure("expected string");
    try {
      String timestamp = ((JsonString)json).getString();
      return JsonParseResult.success(
          LocalDateTime.parse(timestamp, timestampFormat)
                       .atZone(ZoneOffset.UTC)
                       .toInstant());
    } catch (DateTimeParseException e) {
      return JsonParseResult.failure("invalid timestamp format");
    }
  };

  public static final JsonParser<Duration> durationP
      = longP
      . map(microseconds -> Duration.of(microseconds, Duration.MICROSECONDS));

  public static final JsonParser<SerializedValue> serializedParameterP =
      recursiveP(selfP -> BasicParsers
          . <SerializedValue>sumP()
          . when(ValueType.NULL,
              nullP.map(SerializedValue::of))
          . when(ValueType.TRUE,
              boolP.map(SerializedValue::of))
          . when(ValueType.FALSE,
              boolP.map(SerializedValue::of))
          . when(ValueType.STRING,
              stringP.map(SerializedValue::of))
          . when(ValueType.NUMBER, chooseP(
              longP.map(SerializedValue::of),
              doubleP.map(SerializedValue::of)))
          . when(ValueType.ARRAY,
              listP(selfP).map(SerializedValue::of))
          . when(ValueType.OBJECT,
              mapP(selfP).map(SerializedValue::of)));

  public static final JsonParser<Pair<Duration, SerializedActivity>> scheduledActivityP
      = productP
      . field("defer", durationP)
      . field("type", stringP)
      . optionalField("parameters", mapP(serializedParameterP))
      . map(uncurry3(defer -> type -> parameters ->
          Pair.of(defer, new SerializedActivity(type, parameters.orElse(Collections.emptyMap())))));

  public static final JsonParser<CreateSimulationMessage> createSimulationMessageP
      = productP
      . field("adaptationId", stringP)
      . field("startTime", instantP)
      . field("samplingDuration", durationP)
      . field("samplingPeriod", durationP)
      . field("activities", mapP(scheduledActivityP))
      . map(uncurry5(adaptationId -> startTime -> samplingDuration -> samplingPeriod -> activities ->
          new CreateSimulationMessage(adaptationId, startTime, samplingDuration, samplingPeriod, activities)));
}
