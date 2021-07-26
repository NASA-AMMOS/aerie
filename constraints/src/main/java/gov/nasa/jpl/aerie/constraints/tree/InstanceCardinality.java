package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import javax.swing.text.html.Option;
import javax.xml.datatype.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class InstanceCardinality implements Expression<List<Violation>> {
  public final String activityType;
  public final long minimum;
  public final long maximum;

  public InstanceCardinality(
      final String activityType,
      final long minimum,
      final long maximum
  )
  {
    this.activityType = activityType;
    this.minimum = minimum;
    this.maximum = maximum;
  }

  @Override
  public List<Violation> evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment) {

    long numberOfActivities = results.activities.stream().filter(a -> a.type.equals(activityType)).count();

    //If activity number is less than minimum: show violation over full results window
    //If activity number is greater than maximum: show violation from start of first instance to end of last instance
    // else if activity number is satisfied, no violations
    if (numberOfActivities < this.minimum) {
      return List.of(new Violation(new Windows(results.bounds)));
    } else if (numberOfActivities > this.maximum) {
      Optional<ActivityInstance> earliestActivity = results.activities
          .stream()
          .filter(a -> a.type.equals(activityType))
          .min(Comparator.comparing((ActivityInstance a) -> a.window.start));
      Optional<ActivityInstance> latestActivity = results.activities
          .stream()
          .filter(a -> a.type.equals(activityType))
          .max(Comparator.comparing((ActivityInstance a) -> a.window.end));

      if (earliestActivity.isEmpty() || latestActivity.isEmpty()) {
        return List.of(new Violation(new Windows(results.bounds)));
      } else {
        return List.of(new Violation(new Windows(Window.between(earliestActivity.get().window.start, latestActivity.get().window.end))));
      }

    } else {
      return List.of();
    }
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(instance-cardinality %s %s %s)",
        prefix,
        this.activityType,
        String.valueOf(this.minimum),
        String.valueOf(this.maximum)
    );
  }

  @Override
  public void extractResources(final Set<String> names) { }

  @Override
  public int hashCode() {
    return Objects.hash(this.activityType, this.minimum, this.maximum);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof InstanceCardinality)) return false;
    final var o = (InstanceCardinality) obj;

    return Objects.equals(this.activityType, o.activityType) &&
           Objects.equals(this.minimum, o.minimum) &&
           Objects.equals(this.maximum, o.maximum);
  }

}
