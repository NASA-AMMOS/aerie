package gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware;

import java.util.function.BiFunction;
import java.util.function.Function;

import static gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.StandardUnits.SECOND;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.UnitAware.unitAware;

public final class UnitAwareOperations {
  private UnitAwareOperations() {}

  // Implements covariance for UnitAware type functor explicitly.
  // TODO: rethink this, since using ? extends ... in method signatures cleans up the mdoel code
  public static <A, B extends A> UnitAware<A> simplify(UnitAware<B> b) {
    return UnitAware.unitAware(b.value(), b.unit(), u -> simplify(b.in(u)));
  }

  public static <A> UnitAware<A> add(BiFunction<A, Double, A> scaling, UnitAware<? extends A> a, UnitAware<? extends A> b, BiFunction<A, A, A> addition) {
    return unitAware(addition.apply(a.value(), b.value(a.unit())), a.unit(), scaling);
  }

  public static <A> UnitAware<A> subtract(BiFunction<A, Double, A> scaling, UnitAware<? extends A> a, UnitAware<? extends A> b, BiFunction<A, A, A> subtraction) {
    return add(scaling, a, b, subtraction);
  }

  public static <A, B, C> UnitAware<C> multiply(BiFunction<C, Double, C> scaling, UnitAware<? extends A> a, UnitAware<? extends B> b, BiFunction<A, B, C> multiplication) {
    return unitAware(multiplication.apply(a.value(), b.value()), a.unit().multiply(b.unit()), scaling);
  }

  public static <A, B, C> UnitAware<C> divide(BiFunction<C, Double, C> scaling, UnitAware<? extends A> a, UnitAware<? extends B> b, BiFunction<A, B, C> division) {
    return unitAware(division.apply(a.value(), b.value()), a.unit().divide(b.unit()), scaling);
  }

  public static <A, B> UnitAware<A> integrate(BiFunction<A, Double, A> scaling, UnitAware<? extends A> a, UnitAware<? extends B> b, BiFunction<A, B, A> integration) {
    final Unit newUnit = a.unit().multiply(SECOND);
    return unitAware(integration.apply(a.value(), b.value(newUnit)), newUnit, scaling);
  }

  public static <A> UnitAware<A> differentiate(BiFunction<A, Double, A> scaling, UnitAware<? extends A> a, Function<A, A> differentiation) {
    return unitAware(differentiation.apply(a.value()), a.unit().divide(SECOND), scaling);
  }

  public static <A extends Comparable<A>> int compare(UnitAware<? extends A> a, UnitAware<? extends A> b) {
    return a.value().compareTo(b.value(a.unit()));
  }

  public static <A extends Comparable<A>> boolean lessThan(UnitAware<? extends A> a, UnitAware<? extends A> b) {
    return compare(a, b) < 0;
  }

  public static <A extends Comparable<A>> boolean lessThanOrEquals(UnitAware<? extends A> a, UnitAware<? extends A> b) {
    return compare(a, b) <= 0;
  }

  public static <A extends Comparable<A>> boolean greaterThan(UnitAware<? extends A> a, UnitAware<? extends A> b) {
    return compare(a, b) > 0;
  }

  public static <A extends Comparable<A>> boolean greaterThanOrEquals(UnitAware<? extends A> a, UnitAware<? extends A> b) {
    return compare(a, b) >= 0;
  }
}
