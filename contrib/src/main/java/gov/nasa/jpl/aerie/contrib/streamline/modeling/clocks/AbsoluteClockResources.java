package gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks;

import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;

import java.time.Instant;

import static gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource.resource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad.map;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.Clock.clock;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.constant;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;

/**
 * Utilities for {@link Absolute}&lt;{@link Clock}&gt; resources.
 *
 * @see ClockResources
 */
public final class AbsoluteClockResources {
    private AbsoluteClockResources() {}

    /**
     * Create an absolute clock.
     */
    public static MutableResource<Absolute<Clock>> absoluteClock(Instant startTime) {
        return resource(Absolute.relativeTo(startTime, clock(ZERO)));
    }

    /**
     * Returns a clock that measures the time between a discretely-changing origin and a running absolute clock.
     * <p>
     *     If origin is after clock, then the result will be a negative duration.
     * </p>
     * <p>
     *     Note that only between(Instant, Clock) is representable as a clock.
     *     Other overloads, like between(Clock, Instant) or between(Clock, Clock)
     *     should be handled through {@link AbsoluteVariableClockResources}.
     * </p>
     * @param origin A discrete Instant resource to measure from.
     * @param clock A clock to measure to.
     *
     * @see AbsoluteVariableClockResources#between
     */
    public static Resource<Clock> between(Resource<Discrete<Instant>> origin, Resource<Absolute<Clock>> clock) {
        return map(clock, origin, (clock$, origin$) -> clock(
                clock$.relativeDynamics().extract().plus(
                        Durations.between(origin$.extract(), clock$.origin()))));
    }

    public static Resource<Discrete<Boolean>> lessThan(Resource<Absolute<Clock>> clock, Resource<Discrete<Instant>> threshold) {
        return ClockResources.lessThan(between(threshold, clock), constant(ZERO));
    }

    public static Resource<Discrete<Boolean>> lessThanOrEquals(Resource<Absolute<Clock>> clock, Resource<Discrete<Instant>> threshold) {
        return ClockResources.lessThanOrEquals(between(threshold, clock), constant(ZERO));
    }

    public static Resource<Discrete<Boolean>> greaterThan(Resource<Absolute<Clock>> clock, Resource<Discrete<Instant>> threshold) {
        return ClockResources.greaterThan(between(threshold, clock), constant(ZERO));
    }

    public static Resource<Discrete<Boolean>> greaterThanOrEquals(Resource<Absolute<Clock>> clock, Resource<Discrete<Instant>> threshold) {
        return ClockResources.greaterThanOrEquals(between(threshold, clock), constant(ZERO));
    }
}
