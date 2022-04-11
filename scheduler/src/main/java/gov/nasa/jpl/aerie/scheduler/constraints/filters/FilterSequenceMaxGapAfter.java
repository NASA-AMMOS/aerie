package gov.nasa.jpl.aerie.scheduler.constraints.filters;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Filter windows that have at least another window preceding ending within a delay
 */
public class FilterSequenceMaxGapAfter implements TimeWindowsFilter {

  private final Duration maxDelay;

  public FilterSequenceMaxGapAfter(Duration maxDelay) {
    this.maxDelay = maxDelay;
  }

  @Override
  public Windows filter(Plan plan, Windows windows) {
    List<Window> filtered = new ArrayList<>();
    List<Window> windowsTo = StreamSupport
        .stream(windows.spliterator(), false)
        .collect(Collectors.toList());
    if (windowsTo.size() > 1) {
      int nextInd = 1;
      while (nextInd < windowsTo.size()) {
        Window after = windowsTo.get(nextInd);
        Window cur = windowsTo.get(nextInd - 1);

        if (after.start.minus(cur.end).compareTo(maxDelay) <= 0) {
          filtered.add(cur);
        }
        nextInd++;
      }
    }
    return new Windows(filtered);

  }


}
