package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects;

/**
 * An action upon one value by another.
 *
 * <p>
 * An implementation of this interface <b>shall</b> guarantee that its arguments are observationally unchanged after
 * invocation.
 * </p>
 *
 * <p>
 * When the {@link Receiver} type should be mutated, the standard {@link java.util.function.Function} interface should
 * be used instead.
 * </p>
 *
 * @param <Receiver> The type to be acted upon.
 * @param <Actor> The type of information with which to act upon the receiver.
 */
@FunctionalInterface
public interface Action<Receiver, Actor> {
  Receiver apply(Receiver state, Actor actor);
}
