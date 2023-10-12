package gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiry;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resources;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.junit.MerlinExtension;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.set;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentData;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial.polynomial;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.*;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.EPSILON;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MerlinExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public class ComparisonsTest {
  public ComparisonsTest(final Registrar registrar) {
    Resources.init();
  }

  private final CellResource<Polynomial> p = cellResource(polynomial(0));
  private final CellResource<Polynomial> q = cellResource(polynomial(0));

  @Test
  void comparing_distinct_constants() {
    check_comparison(lessThan(constant(0), constant(1)), true, false);
    check_comparison(lessThanOrEquals(constant(0), constant(1)), true, false);
    check_comparison(greaterThan(constant(0), constant(1)), false, false);
    check_comparison(greaterThanOrEquals(constant(0), constant(1)), false, false);
  }

  @Test
  void comparing_equal_constants() {
    check_comparison(lessThan(constant(1), constant(1)), false, false);
    check_comparison(lessThanOrEquals(constant(1), constant(1)), true, false);
    check_comparison(greaterThan(constant(1), constant(1)), false, false);
    check_comparison(greaterThanOrEquals(constant(1), constant(1)), true, false);
  }

  @Test
  void comparing_diverging_linear_terms() {
    setup(() -> {
      set(p, polynomial(0, 1));
      set(q, polynomial(1, 2));
      delay(ZERO);
    });
    check_comparison(lessThan(p, q), true, false);
    check_comparison(lessThanOrEquals(p, q), true, false);
    check_comparison(greaterThan(p, q), false, false);
    check_comparison(greaterThanOrEquals(p, q), false, false);
  }

  @Test
  void comparing_converging_linear_terms() {
    setup(() -> {
      set(p, polynomial(0, 1));
      set(q, polynomial(2, -1));
      delay(ZERO);
    });
    check_comparison(lessThan(p, q), true, true);
    check_comparison(lessThanOrEquals(p, q), true, true);
    check_comparison(greaterThan(p, q), false, true);
    check_comparison(greaterThanOrEquals(p, q), false, true);
  }

  @Test
  void comparing_equal_linear_terms() {
    setup(() -> {
      set(p, polynomial(0, 1));
      set(q, polynomial(0, 1));
      delay(ZERO);
    });
    check_comparison(lessThan(p, q), false, false);
    check_comparison(lessThanOrEquals(p, q), true, false);
    check_comparison(greaterThan(p, q), false, false);
    check_comparison(greaterThanOrEquals(p, q), true, false);
  }

  @Test
  void comparing_equal_then_diverging_linear_terms() {
    setup(() -> {
      set(p, polynomial(0, 1));
      set(q, polynomial(0, 2));
      delay(ZERO);
    });
    // Notice that LT is initially false, but will immediately cross over
    check_comparison(lessThan(p, q), false, true);
    check_comparison(lessThanOrEquals(p, q), true, false);
    check_comparison(greaterThan(p, q), false, false);
    // Notice that GTE is initially true, but will immediately cross over
    check_comparison(greaterThanOrEquals(p, q), true, true);
  }

  @Test
  void comparing_diverging_nonlinear_terms() {
    setup(() -> {
      set(p, polynomial(0, 1, 1));
      set(q, polynomial(1, 2, 2));
      delay(ZERO);
    });
    check_comparison(lessThan(p, q), true, false);
    check_comparison(lessThanOrEquals(p, q), true, false);
    check_comparison(greaterThan(p, q), false, false);
    check_comparison(greaterThanOrEquals(p, q), false, false);
  }

  @Test
  void comparing_converging_nonlinear_terms() {
    setup(() -> {
      set(p, polynomial(0, 1, 1));
      set(q, polynomial(1, 2, -1));
      delay(ZERO);
    });
    check_comparison(lessThan(p, q), true, true);
    check_comparison(lessThanOrEquals(p, q), true, true);
    check_comparison(greaterThan(p, q), false, true);
    check_comparison(greaterThanOrEquals(p, q), false, true);
  }

  @Test
  void comparing_equal_nonlinear_terms() {
    setup(() -> {
      set(p, polynomial(1, 2, -1));
      set(q, polynomial(1, 2, -1));
      delay(ZERO);
    });
    check_comparison(lessThan(p, q), false, false);
    check_comparison(lessThanOrEquals(p, q), true, false);
    check_comparison(greaterThan(p, q), false, false);
    check_comparison(greaterThanOrEquals(p, q), true, false);
  }

  @Test
  void comparing_equal_then_diverging_nonlinear_terms() {
    setup(() -> {
      set(p, polynomial(1, 2, -1));
      set(q, polynomial(1, 2, 1));
      delay(ZERO);
    });
    // Notice that LT is initially false, but will immediately cross over
    check_comparison(lessThan(p, q), false, true);
    check_comparison(lessThanOrEquals(p, q), true, false);
    check_comparison(greaterThan(p, q), false, false);
    // Notice that GTE is initially true, but will immediately cross over
    check_comparison(greaterThanOrEquals(p, q), true, true);
  }

  @Test
  void extrema_of_equal_resources() {
    setup(() -> {
      set(p, polynomial(1, 2, -1));
      set(q, polynomial(1, 2, -1));
      delay(ZERO);
    });

    check_extrema(p, q, false);
  }

  @Test
  void extrema_of_diverging_resources() {
    setup(() -> {
      set(p, polynomial(0, 1, -1));
      set(q, polynomial(1, 2, 1));
      delay(ZERO);
    });

    check_extrema(p, q, false);
  }

  @Test
  void extrema_of_converging_resources() {
    setup(() -> {
      set(p, polynomial(0, 1, 1));
      set(q, polynomial(1, 2, -1));
      delay(ZERO);
    });

    check_extrema(p, q, true);
  }

  @Test
  void extrema_of_equal_then_diverging_resources() {
    setup(() -> {
      set(p, polynomial(1, 1, -1));
      set(q, polynomial(1, 2, 1));
      delay(ZERO);
    });

    check_extrema(p, q, false);
  }

  @Test
  void extrema_of_first_order_equal_then_diverging_resources() {
    setup(() -> {
      set(p, polynomial(1, 2, -1));
      set(q, polynomial(1, 2, 1));
      delay(ZERO);
    });

    check_extrema(p, q, false);
  }

  @Test
  void extrema_of_tangent_resources() {
    setup(() -> {
      set(p, polynomial(0, 2, -1));
      set(q, polynomial(2, -2, 1));
      delay(ZERO);
    });

    // No crossover because curves are tangent at t = 1, but q still dominates p
    check_extrema(p, q, false);
    // Explicitly check answer at t = 1, just to be sure:
    reset();
    delay(SECOND);
    check_extrema(p, q, false);
  }

  private void check_comparison(Resource<Discrete<Boolean>> result, boolean expectedValue, boolean expectCrossover) {
    reset();
    var resultDynamics = result.getDynamics().getOrThrow();
    assertEquals(expectedValue, resultDynamics.data().extract());
    assertEquals(expectCrossover, !resultDynamics.expiry().isNever());
    if (expectCrossover) {
      Duration crossover = resultDynamics.expiry().value().get();
      delay(crossover.minus(EPSILON));
      assertEquals(expectedValue, currentValue(result));
      delay(EPSILON);
      assertEquals(!expectedValue, currentValue(result));
    }
  }

  private void check_extrema(Resource<Polynomial> expectedMin, Resource<Polynomial> expectedMax, boolean expectCrossover) {
    reset();
    var lrMin = min(expectedMin, expectedMax);
    var rlMin = min(expectedMax, expectedMin);
    var lrMax = max(expectedMin, expectedMax);
    var rlMax = max(expectedMax, expectedMin);

    var lrMinDynamics = lrMin.getDynamics();
    var rlMinDynamics = rlMin.getDynamics();
    var lrMaxDynamics = lrMax.getDynamics();
    var rlMaxDynamics = rlMax.getDynamics();

    // min and max are exactly symmetric
    assertEquals(lrMinDynamics, rlMinDynamics);
    assertEquals(lrMaxDynamics, rlMaxDynamics);

    // Expiry for min and max are exactly the same
    Expiry expiry = lrMinDynamics.getOrThrow().expiry();
    assertEquals(expiry, lrMaxDynamics.getOrThrow().expiry());
    // Expiry is finite iff we expect a crossover
    assertEquals(expectCrossover, !expiry.isNever());

    // Data for min and max match their corresponding arguments
    assertEquals(currentData(expectedMin), currentData(lrMin));
    assertEquals(currentData(expectedMax), currentData(lrMax));

    if (expectCrossover) {
      // Just before crossover, min and max still match their originally stated arguments
      delay(expiry.value().get().minus(EPSILON));
      assertEquals(currentData(expectedMin), currentData(lrMin));
      assertEquals(currentData(expectedMax), currentData(lrMax));
      // At crossover, min and max swap
      delay(EPSILON);
      assertEquals(currentData(expectedMax), currentData(lrMin));
      assertEquals(currentData(expectedMin), currentData(lrMax));
    }
  }

  // Helper utilities to reset the simulation during a test.
  // This is helpful to group similar test cases within a single method,
  // even though the simulation can advance while running assertions.
  private Runnable setupFunction = () -> {};
  private void setup(Runnable setupFunction) {
    this.setupFunction = setupFunction;
    reset();
  }
  private void reset() {
    setupFunction.run();
  }
}
