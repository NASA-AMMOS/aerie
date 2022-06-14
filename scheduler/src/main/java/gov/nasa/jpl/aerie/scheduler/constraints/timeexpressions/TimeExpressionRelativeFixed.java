package gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.TimeUtility;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

public class TimeExpressionRelativeFixed extends TimeExpression {

  protected final TimeAnchor anchor;
  protected boolean fixed = true;
  protected final String name;

  public TimeExpressionRelativeFixed(final TimeAnchor anchor, final boolean fixed, final String name) {
    this.fixed = fixed;
    this.anchor = anchor;
    this.name = name;
  }

  @Override
  public Window computeTime(final SimulationResults simulationResults, final Plan plan, final Window interval) {
    Duration from = null;
    if (anchor == TimeAnchor.START) {
      from = interval.start;
    } else if (anchor == TimeAnchor.END) {
      from = interval.end;
    }

    Duration res = from;
    for (final var entry : this.operations) {
      res = TimeUtility.performOperation(entry.getKey(), res, entry.getValue());
    }

    Window retRange;

    //if we want an range of possibles
    if (!fixed) {
      if (res.compareTo(from) > 0) {
        retRange = Window.between(from, res);

      } else {
        retRange = Window.between(res, from);
      }
      // we just want to compute the absolute timepoint
    } else {
      retRange = Window.between(res, res);
    }

    return retRange;
  }
}
