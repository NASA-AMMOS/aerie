package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.discrete;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Solver;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Windows;

import java.util.Set;

public final class DiscreteSolver<ResourceType>
    implements Solver<ResourceType, ResourceType, Set<ResourceType>>
{
  @Override
  public ResourceType valueAt(final ResourceType value, final Duration elapsedTime) {
    return value;
  }

  @Override
  public Windows whenSatisfied(final ResourceType value, final Set<ResourceType> condition) {
    return new Windows((condition.contains(value)) ? Window.FOREVER : Window.EMPTY);
  }
}
