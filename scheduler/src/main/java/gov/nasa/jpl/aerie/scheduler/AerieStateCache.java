package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Map;
import java.util.TreeMap;

public class AerieStateCache {

  private static final Duration MAX_DIST = Duration.duration(500, Duration.MILLISECOND);

  final String stateName;
  final Class<?> type;
  final TreeMap<Duration, Object> timeValue;

  public AerieStateCache(String name, Class<?> type) {
    this.type = type;
    this.stateName = name;
    this.timeValue = new TreeMap<>();
  }

  public void setValue(Duration t, Object val) {
    timeValue.put(t, val);
  }

  public <T> T getValue(Duration t, Class<T> typeClass) {
    if (type.equals(typeClass)) {

      Map.Entry<Duration, Object> low = timeValue.floorEntry(t);
      Map.Entry<Duration, Object> high = timeValue.ceilingEntry(t);
      Map.Entry<Duration, Object> res = null;
      Duration distance;
      if (low != null && high != null) {

        var diffTlow = t.minus(low.getKey());
        var diffTHigh = high.getKey().minus(t);

        if (diffTlow.compareTo(diffTHigh) < 0) {
          res = low;
          distance = diffTlow;
        } else {
          res = high;
          distance = diffTHigh;
        }
      } else if (low != null || high != null) {
        if (low != null) {
          res = low;
          distance = t.minus(low.getKey());
        } else {
          res = high;
          distance = high.getKey().minus(t);
        }
      } else {
        return null;
      }

      if (distance.compareTo(MAX_DIST) > 0) {
        return null;
      }

      return typeClass.cast(res.getValue());
    } else {
      return null;
    }
  }


}
