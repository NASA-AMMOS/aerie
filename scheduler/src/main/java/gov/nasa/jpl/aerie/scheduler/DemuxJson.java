package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.json.JSONObject;

import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class DemuxJson implements ValueSchema.Visitor<Object> {
  String paramName;
  JSONObject params;

  public DemuxJson(String paramName, JSONObject params){
    this.paramName = paramName;
    this.params = params;
  }

  @Override
  public Object onReal() {
    return params.getDouble(paramName);
  }

  @Override
  public Object onInt() {
    return params.getInt(paramName);
  }

  @Override
  public Object onBoolean() {
    return params.getBoolean(paramName);
  }

  @Override
  public Object onString() {
    return params.getString(paramName);
  }

  @Override
  public Object onDuration() {
    return Duration.of(params.getLong(paramName), Duration.SECONDS);

  }

  @Override
  public Object onPath() {
    return params.getString(paramName);
  }

  @Override
  public Object onSeries(ValueSchema value) {
    var array = params.getJSONArray(paramName);
    Object[] ret = new Object[array.length()];

    for(int i = 0; i < array.length(); i++){
      final var map = new HashMap<>();
      map.put(String.valueOf(i),array.get(i));
      final var tmp = new JSONObject(map);
      final var demux = new DemuxJson(String.valueOf(i), tmp);
      ret[i] = value.match(demux);
    }
    return ret;
  }

  @Override
  public Object onStruct(Map<String, ValueSchema> value) {
    Map<String, Object> ret = new HashMap<>();
    var struct = params.getJSONObject(paramName);
    for(var sub:value.keySet()){
      var valueSchema = value.get(sub);
      var demux = new DemuxJson(sub, struct);
      ret.put(sub, valueSchema.match(demux));
    }
    return ret;
  }

  @Override
  public Object onVariant(List<ValueSchema.Variant> variants) {
    return params.getString(paramName);
  }


  //TODO: resolve near dupl with scheduler-server...GraphQLParsers eg by elevating to merlin library somewhere
  public static final Pattern intervalPattern = Pattern.compile(
      "^(?<sign>[+-])?" //optional sign prefix, as in +322:21:15
      + "(((?<hr>\\d+):)?" //optional hours field, as in  322:21:15
      + "(?<min>\\d+):)?" //optional minutes field, as in 22:15
      + "(?<sec>\\d+" //required seconds field, as in 15
      + "(\\.\\d*)?)$"); //optional decimal sub-seconds, as in 15. or 15.111

  public static Duration fromString(final String in) {

    final var matcher = intervalPattern.matcher(in);
    if (!matcher.matches()) {
      throw new DateTimeParseException("unable to parse HH:MM:SS.sss duration from \"" + in + "\"", in, 0);
    }
    final var signValues = Map.of("+",1,"-",-1);
    final var sign = Optional.ofNullable(matcher.group("sign")).map(signValues::get).orElse(1);
    final var hr = Optional.ofNullable(matcher.group("hr")).map(Integer::parseInt)
                           .map(java.time.Duration::ofHours).orElse(java.time.Duration.ZERO);
    final var min = Optional.ofNullable(matcher.group("min")).map(Integer::parseInt)
                            .map(java.time.Duration::ofMinutes).orElse(java.time.Duration.ZERO);
    final var sec = Optional.ofNullable(matcher.group("sec")).map(Double::parseDouble)
                            .map(s -> (long) (s * 1000 * 1000))//seconds->millis->micros
                            .map(us -> java.time.Duration.of(us, ChronoUnit.MICROS))
                            .orElse(java.time.Duration.ZERO);
    final var total = hr.plus(min).plus(sec).multipliedBy(sign);
    return Duration.of(total.getNano() / 1000, Duration.MICROSECOND);
  }


}
