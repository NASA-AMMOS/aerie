package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints;

public class ConditionTypes {

  public enum StateComparator {
    LESS_THAN,
    GREATER_THAN,
    LESS_THAN_OR_EQUAL_TO,
    GREATER_THAN_OR_EQUAL_TO,
    EQUAL_WITHIN,
    EQUAL_TO
  }

  public enum ActivityCondition {
    OCCURRING
  }

  public enum Connector {
    AND,
    OR,
    MINUS
  }
}
