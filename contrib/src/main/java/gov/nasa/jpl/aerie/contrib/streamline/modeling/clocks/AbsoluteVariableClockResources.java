package gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks;

import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources;

import java.time.Instant;

import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad.*;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad.map;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.Absolute.relativeTo;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.VariableClock.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.VariableClockResources.negate;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;

/**
 * Utilities for {@link Absolute}&lt;{@link VariableClock}&gt; resources.
 *
 * @see VariableClockResources
 */
public final class AbsoluteVariableClockResources {
    private AbsoluteVariableClockResources() {}

    public static Resource<Absolute<VariableClock>> constant(Instant time) {
        return pure(relativeTo(time, pausedStopwatch()));
    }

    // REVIEW - For long method names like this, we could abbreviate "VariableClock" to "VarClock".
    public static Resource<Absolute<VariableClock>> asAbsoluteVariableClock(Resource<Absolute<Clock>> absoluteClock) {
        // TODO - This feels like it should be VariableClockResources::asVariableClock run through a monad with Absolute.
        return map(absoluteClock, c -> relativeTo(c.origin(), runningStopwatch(c.relativeDynamics().extract())));
    }

    public static Resource<VariableClock> between(Resource<Absolute<VariableClock>> clock1, Resource<Absolute<VariableClock>> clock2) {
        return map(clock1, clock2, (c1, c2) -> pausedStopwatch(Durations.between(c1.origin(), c2.origin()))
                .plus(c1.relativeDynamics().subtract(c2.relativeDynamics())));
    }

    public static Resource<Absolute<VariableClock>> plus(Resource<Absolute<VariableClock>> absoluteClock, Resource<VariableClock> relativeClock) {
        return map(absoluteClock, relativeClock, (abs, rel) -> Absolute.relativeTo(abs.origin(), abs.relativeDynamics().plus(rel)));
    }

    public static Resource<Absolute<VariableClock>> subtract(Resource<Absolute<VariableClock>> absoluteClock, Resource<VariableClock> relativeClock) {
        return plus(absoluteClock, negate(relativeClock));
    }

    public static Resource<Discrete<Boolean>> lessThan(Resource<Absolute<VariableClock>> clock1, Resource<Absolute<VariableClock>> clock2) {
        return VariableClockResources.lessThan(between(clock2, clock1), DiscreteResources.constant(ZERO));
    }

    public static Resource<Discrete<Boolean>> lessThanOrEquals(Resource<Absolute<VariableClock>> clock1, Resource<Absolute<VariableClock>> clock2) {
        return VariableClockResources.lessThanOrEquals(between(clock2, clock1), DiscreteResources.constant(ZERO));
    }

    public static Resource<Discrete<Boolean>> greaterThan(Resource<Absolute<VariableClock>> clock1, Resource<Absolute<VariableClock>> clock2) {
        return VariableClockResources.greaterThan(between(clock2, clock1), DiscreteResources.constant(ZERO));
    }

    public static Resource<Discrete<Boolean>> greaterThanOrEquals(Resource<Absolute<VariableClock>> clock1, Resource<Absolute<VariableClock>> clock2) {
        return VariableClockResources.greaterThanOrEquals(between(clock2, clock1), DiscreteResources.constant(ZERO));
    }
}
