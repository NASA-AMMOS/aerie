package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public class WindowsWrapperExpression implements Expression<Windows> {
  public final Windows windows;

  public WindowsWrapperExpression(final Windows windows) { this.windows = windows; }

  @Override
  public Windows evaluate(final SimulationResults results, final Window bounds, final Map<String, ActivityInstance> environment) {
    return windows;
  }

  public String prettyPrint(final String prefix) {
    return String.format(
      "%s(expression-wrapping %s)",
      prefix,
      this.windows.toString()
    );
  }
  /** Add the resources referenced by this expression to the given set. **/
  public void extractResources(Set<String> names) { }
}
