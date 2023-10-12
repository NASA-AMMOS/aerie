package gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface UnitAware<T> {

  default T value(Unit desiredUnit) {
    return in(desiredUnit).value();
  }

  T value();

  Unit unit();

  UnitAware<T> in(Unit desiredUnit);

  /**
   * General constructor, used primarily by library code.
   */
  static <T> UnitAware<T> unitAware(T value, Unit unit, Function<Unit, UnitAware<T>> rescale) {

    return new UnitAware<>() {
      @Override
      public T value() {
        return value;
      }

      @Override
      public Unit unit() {
        return unit;
      }

      @Override
      public UnitAware<T> in(Unit desiredUnit) {
        if (unit == desiredUnit) {
          // Short-circuit for performance
          return this;
        }
        if (!unit.dimension.equals(desiredUnit.dimension)) {
          // TODO: Should this be its own kind of exception? A UnitConversionException or DimensionMismatchException?
          throw new IllegalArgumentException("Cannot convert %s to desired unit %s due to dimension mismatch (%s vs %s)."
                .formatted(unit, desiredUnit, unit.dimension, desiredUnit.dimension));
        }
        return rescale.apply(desiredUnit);
      }

      @Override
      public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (UnitAware<?>) obj;
        return Objects.equals(value, that.value()) && Objects.equals(unit, that.unit());
      }

      @Override
      public int hashCode() {
        return Objects.hash(value, unit);
      }

      @Override
      public String toString() {
        return "UnitAware[" + "value=" + value + ", " + "unit=" + unit + ']';
      }
    };
  }

  /**
   * Scaling-function constructor, used primarily in code outside this library.
   */
  static <T> UnitAware<T> unitAware(T value, Unit unit, BiFunction<T, Double, T> scaling) {
    return unitAware(
        value,
        unit,
        desiredUnit -> unitAware(scaling.apply(value, unit.multiplier / desiredUnit.multiplier), desiredUnit, scaling));
  }
}
