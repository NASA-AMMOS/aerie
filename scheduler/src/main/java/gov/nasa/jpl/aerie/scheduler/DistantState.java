package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Map;
import java.util.TreeMap;

public class DistantState<T extends Comparable<T>> implements ExternalState<T> {

  String name;
  static Duration STEP = Duration.duration(1, Duration.SECONDS);
  AerieController aerieLink;
  Plan plan;
  Class<?> classOf;

  @SuppressWarnings("unchecked")
  public DistantState(String name, AerieController aerieLink, Class<?> classOf, Plan plan) {
    this.name = name;
    this.aerieLink = aerieLink;
    this.classOf = classOf;
    this.plan = plan;
  }

  @Override
  public Windows whenValueBetween(T inf, T sup, Windows timeDomain) {
    Windows win = new Windows();
    Duration curMin = null;
    Duration curMax = null;
    for (var g : timeDomain) {
      for (Duration t = g.start; t.shorterThan(g.end); t = t.plus(STEP)) {
        T val = this.getValueAtTime(t);
        if (val.compareTo(inf) > 0 && val.compareTo(sup) < 0) {
          //reached interval
          if (curMin == null) {
            curMin = t;
            curMax = t;
          } else {
            //still in good interval
            curMax = t;
          }
        }
        //not good but we are just leaving a good interval
        else if (curMin != null && curMax != null) {
          win.add(Window.betweenClosedOpen(curMin, curMax));
          curMax = null;
          curMin = null;
        }
      }
    }
    return win;
  }


  @Override
  public Windows whenValueBelow(T val, Windows timeDomain) {
    Windows win = new Windows();
    Duration curMin = null;
    Duration curMax = null;
    for (var g : timeDomain) {
      for (Duration t = g.start; t.shorterThan(g.end); t = t.plus(STEP)) {
        T value = this.getValueAtTime(t);
        if (value.compareTo(val) < 0) {
          //reached interval
          if (curMin == null) {
            curMin = t;
            curMax = t;
          } else {
            //still in good interval
            curMax = t;
          }
        }
        //not good but we are just leaving a good interval
        else if (curMin != null && curMax != null) {
          win.add(Window.betweenClosedOpen(curMin, curMax));
          curMax = null;
          curMin = null;
        }
      }
    }
    return win;
  }

  @Override
  public Windows whenValueAbove(T val, Windows timeDomain) {
    Windows win = new Windows();
    Duration curMin = null;
    Duration curMax = null;
    for (var g : timeDomain) {
      for (Duration t = g.start; t.shorterThan(g.end); t = t.plus(STEP)) {
        T value = this.getValueAtTime(t);
        if (value.compareTo(val) > 0) {
          //reached interval
          if (curMin == null) {
            curMin = t;
            curMax = t;
          } else {
            //still in good interval
            curMax = t;
          }
        }
        //not good but we are just leaving a good interval
        else if (curMin != null && curMax != null) {
          win.add(Window.betweenClosedOpen(curMin, curMax));
          curMax = null;
          curMin = null;
        }
      }
    }
    return win;
  }

  @Override
  public Windows whenValueEqual(T val, Windows timeDomain) {
    Windows win = new Windows();
    Duration curMin = null;
    Duration curMax = null;
    for (var g : timeDomain) {
      for (Duration t = g.start; t.shorterThan(g.end); t = t.plus(STEP)) {
        T value = this.getValueAtTime(t);
        if (value.compareTo(val) == 0) {
          //reached interval
          if (curMin == null) {
            curMin = t;
            curMax = t;
          } else {
            //still in good interval
            curMax = t;
          }
        }
        //not good but we are just leaving a good interval
        else if (curMin != null && curMax != null) {
          win.add(Window.betweenClosedOpen(curMin, curMax));
          curMax = null;
          curMin = null;
        }
      }
    }
    return win;
  }

  @Override
  public Map<Window, T> getTimeline(Windows timeDomain) {
    Map<Window, T> timeline = new TreeMap<>();
    Duration curMin = null;
    Duration curMax = null;
    T curVal = null;
    for (var g : timeDomain) {
      for (Duration t = g.start; t.shorterThan(g.end); t = t.plus(STEP)) {
        T value = this.getValueAtTime(t);
        //corner case
        if (curVal == null) {
          curVal = value;
        }
        //same value as before
        if (curVal.compareTo(value) == 0) {
          //reached interval
          if (curMin == null) {
            curMin = t;
            curMax = t;
          } else {
            //still in good interval
            curMax = t;
          }
        }
        //not the same
        else if (curMin != null && curMax != null) {
          //post last interval
          timeline.put(Window.betweenClosedOpen(curMin, curMax), curVal);
          curMax = t;
          curVal = value;
          curMin = t;
        }
      }
    }
    return timeline;
  }

  @Override
  public Windows whenValueNotEqual(T val, Windows timeDomain) {
    Windows win = new Windows();
    Duration curMin = null;
    Duration curMax = null;
    for (var g : timeDomain) {
      for (Duration t = g.start; t.shorterThan(g.end); t = t.plus(STEP)) {
        T value = this.getValueAtTime(t);
        if (value.compareTo(val) != 0) {
          //reached interval
          if (curMin == null) {
            curMin = t;
            curMax = t;
          } else {
            //still in good interval
            curMax = t;
          }
        }
        //not good but we are just leaving a good interval
        else if (curMin != null && curMax != null) {
          win.add(Window.betweenClosedOpen(curMin, curMax));
          curMax = null;
          curMin = null;
        }
      }
    }
    return win;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T getValueAtTime(Duration t) {
    T ret;
    if (Double.class.equals(classOf)) {
      ret = (T) aerieLink.getDoubleValue(plan, name, t);
    } else if (Integer.class.equals(classOf)) {
      ret = (T) aerieLink.getIntegerValue(plan, name, t);
    } else {
      throw new IllegalArgumentException("Not implemented");
    }

    return ret;
  }
}
