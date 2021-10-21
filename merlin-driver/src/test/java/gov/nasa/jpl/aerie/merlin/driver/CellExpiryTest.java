package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Querier;
import gov.nasa.jpl.aerie.merlin.protocol.model.Applicator;
import gov.nasa.jpl.aerie.merlin.protocol.model.DiscreteApproximator;
import gov.nasa.jpl.aerie.merlin.protocol.model.Projection;
import gov.nasa.jpl.aerie.merlin.protocol.model.ResourceFamily;
import gov.nasa.jpl.aerie.merlin.protocol.model.ResourceSolver;
import gov.nasa.jpl.aerie.merlin.protocol.types.DelimitedDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.Phantom;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.timeline.Schema;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MILLISECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.duration;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class CellExpiryTest {
  @Test
  @DisplayName("Resource profiles are re-queried by the upstream cells' expiry time")
  public void testResourceProfilingByExpiry() {
    final var model = makeModel(Schema.builder(), "/key", "value", MILLISECONDS.times(500));

    final var results = SimulationDriver.simulate(model, Map.of(), Instant.now(), Duration.SECONDS.times(5));

    final var actual = results.discreteProfiles.get("/key").getRight();

    final var expected = List.of(
        Pair.of(duration(500, MILLISECONDS), SerializedValue.of("value")),
        Pair.of(duration(500, MILLISECONDS), SerializedValue.of("value")),

        Pair.of(duration(500, MILLISECONDS), SerializedValue.of("value")),
        Pair.of(duration(500, MILLISECONDS), SerializedValue.of("value")),

        Pair.of(duration(500, MILLISECONDS), SerializedValue.of("value")),
        Pair.of(duration(500, MILLISECONDS), SerializedValue.of("value")),

        Pair.of(duration(500, MILLISECONDS), SerializedValue.of("value")),
        Pair.of(duration(500, MILLISECONDS), SerializedValue.of("value")),

        Pair.of(duration(500, MILLISECONDS), SerializedValue.of("value")),
        Pair.of(duration(500, MILLISECONDS), SerializedValue.of("value")),

        Pair.of(Duration.ZERO, SerializedValue.of("value")));

    assertEquals(expected, actual);
  }

  private <$Schema> Adaptation<$Schema, ?> makeModel(
      final Schema.Builder<$Schema> builder,
      final String resourceName,
      final String resourceValue,
      final Duration expiry
  ) {
    final var initializer = new AdaptationBuilder<>(builder);

    final var ref = initializer.allocate(
        new Object(),
        new Applicator<>() {
          @Override
          public Object duplicate(final Object o) {
            // no internal state
            return o;
          }

          @Override
          public void apply(final Object o, final Object o2) {
            // no internal state
          }

          @Override
          public void step(final Object o, final Duration duration) {
            // no internal state
          }

          @Override
          public Optional<Duration> getExpiry(final Object o) {
            return Optional.of(expiry);
          }
        },
        new Projection<>() {
          @Override
          public Object atom(final Object atom) {
            return atom;
          }

          @Override
          public Object empty() {
            return new Object();
          }

          @Override
          public Object sequentially(final Object prefix, final Object suffix) {
            return empty();
          }

          @Override
          public Object concurrently(final Object left, final Object right) {
            return empty();
          }
        }
    );

    initializer.resourceFamily(new ResourceFamily<$Schema, String>() {
      @Override
      public Map<String, String> getResources() {
        return Map.of(resourceName, resourceValue);
      }

      @Override
      public ResourceSolver<$Schema, String, ?> getSolver() {
        return new ResourceSolver<$Schema, String, String>() {
          @Override
          public String getDynamics(final String s, final Querier<? extends $Schema> now) {
            // Color this resource with the expiry of the cell.
            now.getState(ref);

            return s;
          }

          @Override
          public <Result> Result approximate(final ApproximatorVisitor<String, Result> visitor) {
            return visitor.discrete(new DiscreteApproximator<>() {
              @Override
              public ValueSchema getSchema() {
                return ValueSchema.STRING;
              }

              @Override
              public Iterable<DelimitedDynamics<SerializedValue>> approximate(final String value) {
                return List.of(DelimitedDynamics.persistent(SerializedValue.of(value)));
              }
            });
          }
        };
      }
    });

    return initializer.build(new Phantom<>(ref), Map.of());
  }
}
