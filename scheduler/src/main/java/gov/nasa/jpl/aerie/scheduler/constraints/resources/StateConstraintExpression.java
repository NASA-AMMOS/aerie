package gov.nasa.jpl.aerie.scheduler.constraints.resources;

import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Class describing a constraint on a generic state and providing methods to create all state constraints
 */
public class StateConstraintExpression {

  private static final Logger logger = LoggerFactory.getLogger(StateConstraintExpression.class);

  enum BuilderType {
    OR,
    AND
  }


  /**
   * finds the windows over which the constraint is satisfied
   *
   * the output windows are always a subset of the provided input
   * windows, which allows for search optimization during iterative
   * window narrowing
   *
   * @param plan IN the plan context to evaluate the constraint in
   * @param windows IN the narrowed time ranges in the plan in which
   *     to search for constraint satisfiaction
   * @return time windows in the plan over which the constraint is
   *     satisfied and overlap with the given windows
   */
  public Windows findWindows(Plan plan, Windows windows) {
    Windows tw = sc.findWindows(plan, windows);
    logger.info(name + ' ' + tw);
    return new Windows(tw);
  }


  /**
   * Constraint builder
   * Default behavior is considering that chaining statements is building conjunctions and that OR should be explicitly
   * state
   */
  public static class Builder {


    BuilderType builderType = null;

    //Conjonction is locally modeled as a list of constraint before being transformed in proper ConstraintConjunction
    protected final List<StateConstraintExpression> constraints = new LinkedList<>();

    boolean forAll = false;
    private String name = "SC_WITHOUT_NAME";

    public Builder forAll() {
      forAll = true;
      return getThis();
    }

    public Builder orBuilder() {
      if (builderType != null) {
        throw new IllegalArgumentException("Only one type of builder per expression");
      }
      builderType = BuilderType.OR;
      return getThis();
    }


    public Builder andBuilder() {
      if (builderType != null) {
        throw new IllegalArgumentException("Only one type of builder per expression");
      }
      builderType = BuilderType.AND;
      return getThis();
    }

    public Builder satisfied(StateConstraintExpression ste) {
      this.constraints.add(ste);
      return getThis();
    }


    public Builder lessThan(ExternalState state, SerializedValue value) {
      StateConstraintBelow bca = StateConstraintExpression.lessThan(state, value);
      this.constraints.add(new StateConstraintExpression(bca));
      return getThis();
    }

    public  Builder between(ExternalState state, SerializedValue value1, SerializedValue value2) {
      StateConstraintBetween bca = StateConstraintExpression.buildBetweenConstraint(state, value1, value2);
      this.constraints.add(new StateConstraintExpression(bca));
      return getThis();
    }

    public  Builder above(ExternalState state, SerializedValue value) {
      StateConstraintAbove sca = StateConstraintExpression.buildAboveConstraint(state, value);
      this.constraints.add(new StateConstraintExpression(sca));
      return getThis();
    }

    public Builder equal(ExternalState state, SerializedValue value) {
      StateConstraintEqual sce = StateConstraintExpression.buildEqualConstraint(state, value);
      this.constraints.add(new StateConstraintExpression(sce));
      return getThis();
    }

    public Builder equal(List<? extends ExternalState> states, SerializedValue value) {
      if (!forAll) {
        throw new IllegalArgumentException("forAll() should have been used before");
      } else {
        for (ExternalState state : states) {
          StateConstraintEqual sce = StateConstraintExpression.buildEqualConstraint(state, value);
          this.constraints.add(new StateConstraintExpression(sce));
        }
      }
      return getThis();
    }

    public Builder equalSet(ExternalState state, List<SerializedValue> values) {
      var sced = new StateConstraintExpressionEqualSet(state, values);
      this.constraints.add(sced);
      return getThis();
    }

    public Builder transition(ExternalState state, List<SerializedValue> fromValues, List<SerializedValue> toValues) {
      var c1 = new StateConstraintExpressionEqualSet(state, fromValues);
      var c2 = new StateConstraintExpressionEqualSet(state, toValues);
      var sced = new StateConstraintExpressionTransition(c1, c2);
      this.constraints.add(sced);
      return getThis();
    }

    public Builder notEqual(List<? extends ExternalState> states, SerializedValue value) {
      if (!forAll) {
        throw new IllegalArgumentException("forAll() should have been used before");
      } else {
        for (ExternalState state : states) {
          StateConstraintNotEqual sce = StateConstraintExpression.buildNotEqualConstraint(state, value);
          this.constraints.add(new StateConstraintExpression(sce));
        }
      }
      return getThis();
    }


    public Builder notEqual(ExternalState state, SerializedValue value) {
      StateConstraintNotEqual sce = StateConstraintExpression.buildNotEqualConstraint(state, value);
      this.constraints.add(new StateConstraintExpression(sce));
      return getThis();
    }

    public StateConstraintExpression build() {
      StateConstraintExpression constr = null;

      if (builderType == null) {
        if (constraints.size() == 1) {
          builderType = BuilderType.OR;
        } else {
          throw new IllegalArgumentException("Builder type has to be defined : or / and");
        }
      }

      if (builderType == BuilderType.AND) {
        constr = new StateConstraintExpressionConjunction(constraints, name);
      } else {
        constr = new StateConstraintExpressionDisjunction(constraints, name);
      }
      return constr;
    }


    /**
     * returns the current builder object (but typed at the lowest level)
     *
     * should be implemented by the builder at the bottom of the type heirarchy
     *
     * @return reference to the current builder object (specifically typed)
     */
    protected Builder getThis() {
      return this;
    }


    public Builder name(String name) {
      this.name = name;
      return getThis();
    }


  }


  /**
   * Builds a below constraint
   *
   * @param state the state on which the constraint applies
   * @param value the value below which the state should be to satisfy the constraint
   * @return a below state constraint
   */
  protected static StateConstraintBelow lessThan(ExternalState state, SerializedValue value) {
    return lessThan(state, value, null);
  }

  /**
   * Builds an above constraint
   *
   * @param state the state on which the constraint applies
   * @param value the value above which the state should be to satisfy the constraint
   * @param timeDomain x
   * @return a above state constraint
   */
  protected static StateConstraintBelow lessThan(
      ExternalState state,
      SerializedValue value,
      Windows timeDomain)
  {
    StateConstraintBelow sc = new StateConstraintBelow();
    sc.setDomainUnary(value);
    sc.setState(state);
    sc.setTimeDomain(timeDomain);
    return sc;
  }


  /**
   * Builds an above constraint
   *
   * @param state the state on which the constraint applies
   * @param value the value above which the state should be to satisfy the constraint
   * @return a above state constraint
   */
  protected static  StateConstraintAbove buildAboveConstraint(
      ExternalState state,
      SerializedValue value)
  {
    return buildAboveConstraint(state, value, null);
  }


  /**
   * Builds an above constraint
   *
   * @param state the state on which the constraint applies
   * @param value the value above which the state should be to satisfy the constraint
   * @param timeDomain x
   * @return a above state constraint
   */
  protected static StateConstraintAbove buildAboveConstraint(
      ExternalState state,
      SerializedValue value,
      Windows timeDomain)
  {
    StateConstraintAbove sc = new StateConstraintAbove();
    sc.setDomainUnary(value);
    sc.setState(state);
    sc.setTimeDomain(timeDomain);
    return sc;
  }


  /**
   * Builds an above constraint
   *
   * @param state the state on which the constraint applies
   * @param value1 the lower bounds of the interval in which the state should be to satisfy the constraint
   * @param value2 the upper bounds of the interval in which the state should be to satisfy the constraint
   * @return a between state constraint
   */
  protected static StateConstraintBetween buildBetweenConstraint(
      ExternalState state,
      SerializedValue value1,
      SerializedValue value2)
  {
    return buildBetweenConstraint(state, value1, value2, null);
  }

  /**
   * Builds an above constraint
   *
   * @param state the state on which the constraint applies
   * @param value1 the lower bounds of the interval in which the state should be to satisfy the constraint
   * @param value2 the upper bounds of the interval in which the state should be to satisfy the constraint
   * @param timeDomain x
   * @param <T> the state type
   * @return a between state constraint
   */
  protected static <T extends Comparable<T>> StateConstraintBetween buildBetweenConstraint(
      ExternalState state,
      SerializedValue value1,
      SerializedValue value2,
      Windows timeDomain)
  {
    StateConstraintBetween sc = new StateConstraintBetween();
    sc.setValueDefinition(Arrays.asList(value1, value2));
    sc.setState(state);
    sc.setTimeDomain(timeDomain);
    return sc;
  }

  /**
   * Builds an equal constraint
   *
   * @param state the state on which the constraint applies
   * @param value the value to which the state should be equal to satisfy the constraint
   * @return an equal state constraint
   */
  protected static StateConstraintEqual buildEqualConstraint(
      ExternalState state,
      SerializedValue value)
  {
    return buildEqualConstraint(state, value, null);
  }

  /**
   * Builds an equal constraint
   *
   * @param state the state on which the constraint applies
   * @param value the value to which the state should be equal to satisfy the constraint
   * @param timeDomain x
   * @return an equal state constraint
   */
  protected static StateConstraintEqual buildEqualConstraint(
      ExternalState state,
      SerializedValue value,
      Windows timeDomain)
  {
    StateConstraintEqual sc = new StateConstraintEqual();
    sc.setDomainUnary(value);
    sc.setState(state);
    sc.setTimeDomain(timeDomain);
    return sc;
  }


  /**
   * Builds an equal constraint
   *
   * @param state the state on which the constraint applies
   * @param value the value to which the state should be equal to satisfy the constraint
   * @return an equal state constraint
   */
  protected static StateConstraintNotEqual buildNotEqualConstraint(
      ExternalState state,
      SerializedValue value)
  {
    StateConstraintNotEqual sc = new StateConstraintNotEqual();
    sc.setDomainUnary(value);
    sc.setState(state);
    sc.setTimeDomain(null);
    return sc;
  }


  public StateConstraintExpression(StateConstraint sc) {
    this.name = "SC_WITHOUT_NAME";
    this.sc = sc;
  }

  protected StateConstraintExpression(StateConstraint sc, String name) {
    this.name = name;
    this.sc = sc;
  }

  final StateConstraint sc;
  final String name;

}
