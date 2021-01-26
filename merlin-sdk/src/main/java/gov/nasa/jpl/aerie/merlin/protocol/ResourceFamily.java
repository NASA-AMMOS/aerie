package gov.nasa.jpl.aerie.merlin.protocol;

import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

// Every type of Resource has unique types of Conditions and Dynamics.
public interface ResourceFamily<$Schema, Resource, /*->*/ Condition> {
  // TODO: Add parametric resources & operators.
  //   Parametric resources correspond to "resource families".
  //   Parametric operators correspond to things like multiplication-by-scalar.
  Map<String, Resource> getResources();
  Map<String, UnaryOperator<Resource>> getUnaryOperators();
  Map<String, BinaryOperator<Resource>> getBinaryOperators();
  Map<String, ConditionType<Condition>> getConditionTypes();

  ResourceSolver<$Schema, Resource, ?, Condition> getSolver();
}
