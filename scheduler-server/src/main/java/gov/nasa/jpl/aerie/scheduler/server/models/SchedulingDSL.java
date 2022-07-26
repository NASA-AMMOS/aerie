package gov.nasa.jpl.aerie.scheduler.server.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.Expression;
import gov.nasa.jpl.aerie.constraints.tree.WindowsExpression;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.TimeUtility;
import gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions.TimeAnchor;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SchedulingDSL {
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
  @JsonSubTypes({
      @JsonSubTypes.Type(value = GoalSpecifier.RecurrenceGoalDefinition.class),
      @JsonSubTypes.Type(value = GoalSpecifier.CoexistenceGoalDefinition.class),
      @JsonSubTypes.Type(value = GoalSpecifier.CardinalityGoalDefinition.class),
      @JsonSubTypes.Type(value = GoalSpecifier.GoalAnd.class),
      @JsonSubTypes.Type(value = GoalSpecifier.GoalOr.class),
      @JsonSubTypes.Type(value = GoalSpecifier.GoalApplyWhen.class)
  })
  public sealed interface GoalSpecifier {

    @JsonTypeName("ActivityRecurrenceGoal")
    record RecurrenceGoalDefinition(
        ActivityTemplate activityTemplate,
        Duration interval
    ) implements GoalSpecifier {}

    @JsonTypeName("ActivityCoexistenceGoal")
    record CoexistenceGoalDefinition(
        ActivityTemplate activityTemplate,
        ConstraintExpression forEach,
        Optional<ActivityTimingConstraint> startConstraint,
        Optional<ActivityTimingConstraint> endConstraint
    ) implements GoalSpecifier {}

    @JsonTypeName("ActivityCardinalityGoal")
    record CardinalityGoalDefinition(
        ActivityTemplate activityTemplate,
        CardinalitySpecification specification,
        ClosedOpenInterval inPeriod
    ) implements GoalSpecifier {}

    @JsonTypeName("GoalAnd")
    record GoalAnd(List<GoalSpecifier> goals) implements GoalSpecifier {}

    @JsonTypeName("GoalOr")
    record GoalOr(List<GoalSpecifier> goals) implements GoalSpecifier {}

    @JsonTypeName("ApplyWhen")
    record GoalApplyWhen(
        GoalSpecifier goal,
        @JsonProperty("window")
        Expression<Windows> windows
    ) implements GoalSpecifier {}
  }

  public record LinearResource(String name) {}
  public record CardinalitySpecification(Optional<Duration> duration, Optional<Integer> occurrence){}
  public record ClosedOpenInterval(Duration start, Duration end){}
  public record ActivityTemplate(String activityType, Map<String, SerializedValue> args) {}

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
  @JsonSubTypes({
      @JsonSubTypes.Type(value = ConstraintExpression.Activity.class),
      @JsonSubTypes.Type(value = ConstraintExpression.Windows.class)
  })
  public sealed interface ConstraintExpression {
    @JsonTypeName("ActivityExpression")
    record Activity(String type) implements ConstraintExpression {}

    @JsonTypeName("WindowsExpressionRoot")
    record Windows(WindowsExpression expression) implements ConstraintExpression {}
  }
  public record ActivityTimingConstraint(TimeAnchor windowProperty, TimeUtility.Operator operator, Duration operand, boolean singleton) {}
}
