package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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



  static Duration fromString(String s){
    var leftright = s.split(".");
    String left = s, right=null;
    if(leftright.length!=0){
      if(leftright.length>2){
        throw new IllegalArgumentException("One decimal point expected");
      }
      left = leftright[0];
      right = leftright[1];
    }

    var pattern = Pattern.compile("(\\d*):(\\d\\d):(\\d\\d)");

    var w = pattern.matcher(left);
    Duration d = Duration.ZERO;
    if(w.matches()){

      var hours = Integer.valueOf(w.group(1));
      d = d.plus(hours, Duration.HOURS);
      var min= Integer.valueOf(w.group(2));
      d = d.plus(min, Duration.MINUTE);
      var sec= Integer.valueOf(w.group(3));
      d = d.plus(sec, Duration.SECONDS);
    } else{
      //does not match
      throw new IllegalArgumentException("Duration expected");
    }
    if(right!= null) {
      var dec = Integer.valueOf(right);

      if(right.length() == 3){
        d = d.plus(dec, Duration.MILLISECOND);
      } else if (right.length() == 6) {
        d = d.plus(dec, Duration.MICROSECOND);
      } else{
        throw new IllegalArgumentException("Decimal precision should be 3 or 6");
      }
    }
    return d;
  }


}
