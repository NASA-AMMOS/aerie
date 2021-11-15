package gov.nasa.jpl.aerie.scheduler;

import java.util.Map;
import java.util.TreeMap;

public class AerieStateCache {

  private static final Duration MAX_DIST = new Duration(0.5);

  String stateName;
  Class<?> type;
  TreeMap<Time, Object> timeValue;

  public AerieStateCache(String name, Class<?> type) {
    this.type = type;
    this.stateName = name;
    this.timeValue = new TreeMap<Time, Object>();
  }

  public void setValue(Time t, Object val) {
    timeValue.put(t, val);
  }

  public <T> T getValue(Time t, Class<T> typeClass) {
    if (type.equals(typeClass)) {

      Map.Entry<Time, Object> low = timeValue.floorEntry(t);
      Map.Entry<Time, Object> high = timeValue.ceilingEntry(t);
      Map.Entry<Time, Object> res = null;
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
