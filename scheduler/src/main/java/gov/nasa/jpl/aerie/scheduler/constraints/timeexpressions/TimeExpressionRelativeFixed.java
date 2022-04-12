package gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.TimeUtility;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

import java.util.Map;

public class TimeExpressionRelativeFixed extends TimeExpression {

  protected final TimeAnchor anchor;
  protected boolean fixed = true;
  protected final String name;

  public TimeExpressionRelativeFixed(TimeAnchor anchor, boolean fixed, String name) {
    this.fixed = fixed;
    this.anchor = anchor;
    this.name = name;
  }

  @Override
  public Window computeTime(Plan plan, Window interval) {
    Duration from = null;
    if (anchor == TimeAnchor.START) {
      from = interval.start;
    } else if (anchor == TimeAnchor.END) {
      from = interval.end;
    }

    Duration res = from;
    for (Map.Entry<TimeUtility.Operator, Duration> entry : this.operations.entrySet()) {
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
