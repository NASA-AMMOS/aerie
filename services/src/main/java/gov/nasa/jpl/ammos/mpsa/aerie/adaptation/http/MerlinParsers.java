package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.http;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.app.CreateSimulationMessage;
import gov.nasa.jpl.ammos.mpsa.aerie.json.BasicParsers;
import gov.nasa.jpl.ammos.mpsa.aerie.json.JsonParser;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.JsonValue.ValueType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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

  public static final JsonParser<Instant> instantP
      = stringP
      . map(s -> LocalDateTime
          // TODO: handle parse errors
          . parse(s, DateTimeFormatter.ofPattern("uuuu-DDD'T'HH:mm:ss[.n]"))
          . atZone(ZoneOffset.UTC)
          . toInstant());

  public static final JsonParser<Duration> durationP
      = longP
      . map(microseconds -> Duration.of(microseconds, TimeUnit.MICROSECONDS));

  public static final JsonParser<SerializedParameter> serializedParameterP =
      recursiveP(selfP -> BasicParsers
          . <SerializedParameter>sumP()
          . when(ValueType.NULL,
              nullP.map(SerializedParameter::of))
          . when(ValueType.TRUE,
              boolP.map(SerializedParameter::of))
          . when(ValueType.FALSE,
              boolP.map(SerializedParameter::of))
          . when(ValueType.STRING,
              stringP.map(SerializedParameter::of))
          . when(ValueType.NUMBER, chooseP(
              longP.map(SerializedParameter::of),
              doubleP.map(SerializedParameter::of)))
          . when(ValueType.ARRAY,
              listP(selfP).map(SerializedParameter::of))
          . when(ValueType.OBJECT,
              mapP(selfP).map(SerializedParameter::of)));

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
      . field("samplingFrequency", durationP)
      . field("activities", listP(scheduledActivityP))
      . map(uncurry5(adaptationId -> startTime -> samplingDuration -> samplingFrequency -> activities ->
          new CreateSimulationMessage(adaptationId, startTime, samplingDuration, samplingFrequency, activities)));
}
