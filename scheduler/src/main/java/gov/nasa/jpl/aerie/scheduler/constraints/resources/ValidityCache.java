package gov.nasa.jpl.aerie.scheduler.constraints.resources;


import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public abstract class ValidityCache {

  public enum Validity {
    TRUE,
    FALSE,
    UNKNOWN
  }

  private final TreeMap<Window, Validity> cache;


  public ValidityCache() {
    cache = new TreeMap<>();
    cache.put(Window.between(Duration.ZERO, Duration.MAX_VALUE), Validity.UNKNOWN);
  }

  public void put(Window range, Validity val) {
    if (range.isSingleton()) {
      throw new RuntimeException();
    }
    cache.put(range, val);
  }


  public Windows findWindowsCache(Plan plan, Windows windows) {


    Windows returnWin = new Windows();
    boolean reset = false;
    Window window = null;
    //for each input window
    for (final Window value : windows) {
      window = value;
      //for each entry in the cache
      Iterator<Map.Entry<Window, Validity>> itCache = cache.entrySet().iterator();
      var toAdd = new HashMap<Window, Validity>();
      while (itCache.hasNext()) {
        var entry = itCache.next();
        Window cacheInter = entry.getKey();

        //if entry in cache is after the window, we can break
        if (cacheInter.isStrictlyAfter(window)) {
          break;
        }

        //if entry is strictly before we can continue
        if (cacheInter.isStrictlyBefore(window)) {
          continue;
        }

        Validity val = entry.getValue();
        var intersection = Window.intersect(window, cacheInter);
        if (!intersection.isEmpty()) {
          if (val == Validity.TRUE) {
            returnWin.add(intersection);

          } else if (val == Validity.UNKNOWN) {
            itCache.remove();
            Windows fetched = fetchValue(plan, new Windows(intersection));
            Windows subtractions = Windows.subtract(intersection, cacheInter);
            for (var sub : subtractions) {
              toAdd.put(sub, Validity.UNKNOWN);
            }
            for (var fetchedWin : fetched) {
              toAdd.put(fetchedWin, Validity.TRUE);
              returnWin.add(fetchedWin);
            }
            fetched = fetched.complement();
            fetched = fetched.removeFirstAndLast();
            for (var fetchedWin : fetched) {
              toAdd.put(fetchedWin, Validity.FALSE);
            }
          }
        }

      }
      cache.putAll(toAdd);
    }
    return returnWin;
  }

  //how to get the value
  public abstract Windows fetchValue(Plan plan, Windows intervals);


}
