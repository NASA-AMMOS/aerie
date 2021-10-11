package gov.nasa.jpl.aerie.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class mocking the behavior of an externally defined state and implementing ExternalState interface
 *
 * @param <T> the type of the variable managed by the state
 */
public class MockState<T extends Comparable<T>> implements
    ExternalState<T>
{

  Map<Range<Time>, T> values;

  public void initFromStateFile(Map<Time, T> fileValues) {
    values = new TreeMap<Range<Time>, T>();
    Time start = null;
    T val = null;
    for (Map.Entry<Time, T> entry : fileValues.entrySet()) {
      if (start != null && val != null) {
        values.put(new Range<Time>(start, entry.getKey()), val);
      }
      start = entry.getKey();
      val = entry.getValue();
    }
  }


  public T getValueAtTime(Time t) {
    for (Map.Entry<Range<Time>, T> intv : values.entrySet()) {
      if (intv.getKey().contains(t)) {
        return intv.getValue();
      }
    }
    return null;
  }

  public Map<Time, T> valuesInterval(Time t1, Time t2) {
    return null;
  }

  public TimeWindows whenValueBetween(T inf, T sup, TimeWindows windows) {
    TimeWindows returnWindows = new TimeWindows();

    Collection<Range<Time>> windowsR = windows.getRangeSet();
    for (Map.Entry<Range<Time>, T> intv : values.entrySet()) {
      if (intv.getValue().compareTo(inf) >= 0 && intv.getValue().compareTo(sup) <= 0) {
        Range<Time> stateRange = intv.getKey();
        for (Range<Time> range : windowsR) {
          Range<Time> inter = range.intersect(stateRange);
          if (inter != null) {
            returnWindows.union(inter);
          }
        }
      }
    }
    return returnWindows;
  }

  public TimeWindows whenValueBelow(T val, TimeWindows windows) {

    TimeWindows returnWindows = new TimeWindows();

    Collection<Range<Time>> windowsR = windows.getRangeSet();
    for (Map.Entry<Range<Time>, T> intv : values.entrySet()) {
      if (intv.getValue().compareTo(val) < 0) {
        Range<Time> stateRange = intv.getKey();
        for (Range<Time> range : windowsR) {
          Range<Time> inter = range.intersect(stateRange);
          if (inter != null) {
            returnWindows.union(inter);
          }
        }
      }
    }
    return returnWindows;
  }

  public TimeWindows whenValueAbove(T val, TimeWindows windows) {
    TimeWindows returnWindows = new TimeWindows();

    Collection<Range<Time>> windowsR = windows.getRangeSet();
    for (Map.Entry<Range<Time>, T> intv : values.entrySet()) {
      if (intv.getValue().compareTo(val) > 0) {
        Range<Time> stateRange = intv.getKey();
        for (Range<Time> range : windowsR) {
          Range<Time> inter = range.intersect(stateRange);
          if (inter != null) {
            returnWindows.union(inter);
          }
        }
      }
    }
    return returnWindows;

  }

  public TimeWindows whenValueEqual(T val, TimeWindows windows) {
    TimeWindows returnWindows = new TimeWindows();

    Collection<Range<Time>> windowsR = windows.getRangeSet();
    for (Map.Entry<Range<Time>, T> intv : values.entrySet()) {
      if (intv.getValue().compareTo(val) == 0) {
        Range<Time> stateRange = intv.getKey();
        for (Range<Time> range : windowsR) {
          Range<Time> inter = range.intersect(stateRange);
          if (inter != null) {
            returnWindows.union(inter);
          }
        }
      }
    }
    return returnWindows;

  }

  @Override
  public Map<Range<Time>, T> getTimeline(TimeWindows timeDomain) {
    return values;
  }

  @Override
  public TimeWindows whenValueNotEqual(T val, TimeWindows windows) {
    TimeWindows returnWindows = new TimeWindows();
    Collection<Range<Time>> result = new ArrayList<Range<Time>>();
    Collection<Range<Time>> windowsR = windows.getRangeSet();
    for (Map.Entry<Range<Time>, T> intv : values.entrySet()) {
      if (intv.getValue().compareTo(val) != 0) {
        Range<Time> stateRange = intv.getKey();
        for (Range<Time> range : windowsR) {
          Range<Time> inter = range.intersect(stateRange);
          if (inter != null) {
            result.add(inter);
            //returnWindows.union(inter);
          }
        }
      }
    }
    return TimeWindows.of(result);
  }

  public void draw() {
    for (Map.Entry<Range<Time>, T> v : values.entrySet()) {
      if (v.getValue() instanceof Boolean) {
        Boolean val = (Boolean) v.getValue();
        String toPrint = "";
        if (val) {
          toPrint = "X";
        } else {
          toPrint = "-";
        }
        int max = (int) v.getKey().getMaximum().toEpochMilliseconds() / 1000;
        int min = (int) v.getKey().getMinimum().toEpochMilliseconds() / 1000;

        for (int i = min; i < max; i++) {
          System.out.print(toPrint + "  ");

        }

      }
    }
    System.out.println("");


  }

}
