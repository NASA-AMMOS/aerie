package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;

public abstract class ConstraintStructure {
  private ConstraintStructure() {}

  public abstract <Result> Result visit(ConstraintStructureVisitor<Result> visitor);

  public interface ConstraintStructureVisitor<Result> {
    Result onActivityConstraintStructure(String activityType, ConditionTypes.ActivityCondition condition);
    Result onStateConstraintStructure(
        String stateName,
        ConditionTypes.StateComparator comparator,
        SerializedValue value);
    Result onComplexConstraintStructure(
        ConditionTypes.Connector connector,
        ConstraintStructure left,
        ConstraintStructure right);
  }

  public static ConstraintStructure ofActivityConstraint(
      String activityType,
      ConditionTypes.ActivityCondition condition)
  {
    return new ConstraintStructure() {
      @Override
      public <Result> Result visit(final ConstraintStructureVisitor<Result> visitor) {
        return visitor.onActivityConstraintStructure(activityType, condition);
      }
    };
  }

  public static ConstraintStructure ofStateConstraint(
      String stateName,
      ConditionTypes.StateComparator comparator,
      SerializedValue value)
  {
    return new ConstraintStructure() {
      @Override
      public <Result> Result visit(final ConstraintStructureVisitor<Result> visitor) {
        return visitor.onStateConstraintStructure(stateName, comparator, value);
      }
    };
  }

  public static ConstraintStructure ofComplexConstraint(
      ConditionTypes.Connector connector,
      ConstraintStructure left,
      ConstraintStructure right)
  {
    return new ConstraintStructure() {
      @Override
      public <Result> Result visit(final ConstraintStructureVisitor<Result> visitor) {
        return visitor.onComplexConstraintStructure(connector, left, right);
      }
    };
  }
}
