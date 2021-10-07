package gov.nasa.jpl.aerie.scheduler;


import java.util.*;

public abstract class ValidityCache {

  public enum Validity {
    TRUE,
    FALSE,
    UNKNOWN
  }

  private TreeMap<Range<Time>, Validity> cache;


  public ValidityCache() {
    cache = new TreeMap<Range<Time>, Validity>();
    cache.put(new Range<Time>(Time.ofZero(), Time.ofMax()), Validity.UNKNOWN);
  }

  public void put(Range<Time> range, Validity val) {
    if (range.isSingleton()) {
      throw new RuntimeException();
    }
    cache.put(range, val);
  }


  public TimeWindows findWindowsCache(Plan plan, TimeWindows windows) {


    TimeWindows returnWin = new TimeWindows();
    boolean reset = false;
    Range<Time> window = null;
    //for each input window
    var it = windows.getRangeSet().iterator();
    while (it.hasNext()) {
      window = it.next();
      //for each entry in the cache
      Iterator<Map.Entry<Range<Time>, Validity>> itCache = cache.entrySet().iterator();
      var toAdd = new HashMap<Range<Time>, Validity>();
      while (itCache.hasNext()) {
        var entry = itCache.next();
        Range<Time> cacheInter = entry.getKey();

        //if entry in cache is after the window, we can break
        if (cacheInter.isAfter(window)) {
          break;
        }

        //if entry is strictly before we can continue
        if (cacheInter.isBefore(window)) {
          continue;
        }

        Validity val = entry.getValue();
        var intersection = window.intersect(cacheInter);
        if (intersection != null) {
          if (val == Validity.TRUE) {
            returnWin.union(intersection);

          } else if (val == Validity.UNKNOWN) {
            itCache.remove();
            TimeWindows fetched = fetchValue(plan, TimeWindows.of(intersection));
            List<Range<Time>> subtractions = cacheInter.subtract(intersection);
            for (var sub : subtractions) {
              toAdd.put(sub, Validity.UNKNOWN);
            }
            for (var fetchedWin : fetched.getRangeSet()) {
              toAdd.put(fetchedWin, Validity.TRUE);
              returnWin.union(fetchedWin);
            }
            fetched.complement();
            fetched.removeFirstLast();
            for (var fetchedWin : fetched.getRangeSet()) {
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
  public abstract TimeWindows fetchValue(Plan plan, TimeWindows intervals);


}
