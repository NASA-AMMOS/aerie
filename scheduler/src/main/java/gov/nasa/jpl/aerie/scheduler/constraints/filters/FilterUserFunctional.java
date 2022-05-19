package gov.nasa.jpl.aerie.scheduler.constraints.filters;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

import java.util.function.Function;

public class FilterUserFunctional extends FilterFunctional {

  final Function<Window, Boolean> function;

  public FilterUserFunctional(final Function<Window, Boolean> function) {
    this.function = function;
  }

  @Override
  public boolean shouldKeep(final SimulationResults simulationResults, final Plan plan, final Window range) {
    return function.apply(range);
  }
}
