package gov.nasa.jpl.aerie.scheduler.goals;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a conjunction of goal as a goal
 */
public class CompositeAndGoal extends Goal {

  List<Goal> goals;

  private CompositeAndGoal() {}

  public static class Builder extends Goal.Builder<Builder> {

    final List<Goal> goals = new ArrayList<>();

    public Builder and(Goal goal) {
      goals.add(goal);
      return this;
    }

    public CompositeAndGoal build() {
      CompositeAndGoal cg = new CompositeAndGoal();
      fill(cg);
      cg.goals = new ArrayList<>(goals);
      return cg;
    }

    public Builder getThis() {
      return this;
    }
  }

  public List<Goal> getSubgoals() {
    return goals;
  }
}
