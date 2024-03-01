package gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks;

import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

// TODO - Investigate if there's a monad or applicative that could be extracted from this class.
/**
 * Absolute clock wrapper.
 * <p>
 *     Converts a clock-like dynamics object for Duration (time relative to some origin)
 *     to one for Instant (absolute time).
 * </p>
 *
 * @param <D>
 */
public record Absolute<D extends Dynamics<Duration, D>>(Instant origin, D relativeDynamics)
        implements Dynamics<Instant, Absolute<D>> {
    @Override
    public Instant extract() {
        // TODO - it would be better to look up the ChronoUnit for Duration.EPSILON,
        //   so that we'll always use the highest possible precision here.
        return origin.plus(relativeDynamics.extract().in(Duration.MICROSECOND), ChronoUnit.MICROS);
    }

    @Override
    public Absolute<D> step(Duration t) {
        return new Absolute<>(origin, relativeDynamics.step(t));
    }

    public static <D extends Dynamics<Duration, D>> Absolute<D> relativeTo(Instant origin, D relativeDynamics) {
        return new Absolute<>(origin, relativeDynamics);
    }
}
