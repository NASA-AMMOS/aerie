package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * Class mocking the behavior of an externally defined state and implementing ExternalState interface
 *
 * @param <T> the type of the variable managed by the state
 */
public class MockState<T extends Comparable<T>> implements
    ExternalState<T>
{

  Map<Window, T> values;

  public void initFromStateFile(Map<Duration, T> fileValues) {
    values = new TreeMap<Window, T>();
    Duration start = null;
    T val = null;
    for (Map.Entry<Duration, T> entry : fileValues.entrySet()) {
      if (start != null && val != null) {
        values.put(Window.betweenClosedOpen(start, entry.getKey()), val);
      }
      start = entry.getKey();
      val = entry.getValue();
    }

  }


  public T getValueAtTime(Duration t) {
    for (Map.Entry<Window, T> intv : values.entrySet()) {
      if (intv.getKey().contains(t)) {
        return intv.getValue();
      }
    }
    return null;
  }



  public Windows whenValue(Windows windows, Predicate<T> pred) {
    Windows returnWindows = new Windows();

    for (Map.Entry<Window, T> intv : values.entrySet()) {
      if (pred.test(intv.getValue())){
        Window stateRange = intv.getKey();
        for (Window range : windows) {
          var inter = Window.intersect(range, stateRange);
          if (!inter.isEmpty()) {
            returnWindows.add(inter);
          }
        }
      }
    }
    return returnWindows;
  }


  public Windows whenValueBetween(T inf, T sup, Windows windows) {
   return whenValue(windows,(x) -> x.compareTo(inf) >= 0 && x.compareTo(sup) <= 0);
  }
  public Windows whenValueBelow(T val, Windows windows) {
    return whenValue(windows,(x) -> x.compareTo(val) < 0);
  }
  public Windows whenValueAbove(T val, Windows windows) {
    return whenValue(windows,(x) -> x.compareTo(val) > 0);
  }

  public Windows whenValueEqual(T val, Windows windows) {
    return whenValue(windows,(x) -> x.compareTo(val) == 0);
  }

  public Windows whenValueNotEqual(T val, Windows windows) {
    return whenValue(windows,(x) -> x.compareTo(val) != 0);
  }
  @Override
  public Map<Window, T> getTimeline(Windows timeDomain) {
    return values;
  }


  public void draw() {
    for (Map.Entry<Window, T> v : values.entrySet()) {
      if (v.getValue() instanceof Boolean) {
        Boolean val = (Boolean) v.getValue();
        String toPrint = "";
        if (val) {
          toPrint = "X";
        } else {
          toPrint = "-";
        }
        int max = (int) v.getKey().end.in(Duration.SECONDS);
        int min = (int) v.getKey().start.in(Duration.SECONDS);

        for (int i = min; i < max; i++) {
          System.out.print(toPrint + "  ");

        }

      }
    }
    System.out.println("");


  }

}
