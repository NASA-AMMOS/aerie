package gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial;

import gov.nasa.jpl.aerie.contrib.streamline.core.Resources;
import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.UnitAware;

import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial.polynomial;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.replaying;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.spawn;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad.effect;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;

public final class PolynomialEffects {
  private PolynomialEffects() {}

  // Consumable style operations

  /**
   * Consume some amount of a resource instantaneously.
   */
  public static void consume(CellResource<Polynomial> resource, double amount) {
    resource.emit(effect($ -> $.subtract(polynomial(amount))));
  }

  /**
   * Consume resource according to a given polynomial profile while an action runs.
   */
  public static void consuming(CellResource<Polynomial> resource, Polynomial profile, Runnable action) {
    resource.emit(effect($ -> $.subtract(profile)));
    final Duration start = Resources.currentTime();
    action.run();
    final Duration elapsedTime = Resources.currentTime().minus(start);
    // Nullify ongoing effects by adding a profile with the same behavior,
    // but with an initial value of 0
    final Polynomial steppedProfile = profile.step(elapsedTime);
    final Polynomial counteractingProfile = steppedProfile.subtract(polynomial(steppedProfile.extract()));
    resource.emit(effect($ -> $.add(counteractingProfile)));
  }

  /**
   * Consume some amount of a resource at a uniform rate over a fixed period of time.
   */
  public static void consumeUniformly(CellResource<Polynomial> resource, double amount, Duration time) {
    consume(resource, amount / time.ratioOver(SECOND), time);
  }

  /**
   * Consume some resource a fixed rate during an action
   */
  public static void consuming(CellResource<Polynomial> resource, double rate, Runnable action) {
    consuming(resource, polynomial(0, rate), action);
  }

  /**
   * Consume some resource at a fixed rate for a fixed period of time, asynchronously.
   */
  public static void consume(CellResource<Polynomial> resource, double rate, Duration time) {
    spawn(replaying(() -> consuming(resource, rate, () -> delay(time))));
  }

  public static void restoring(CellResource<Polynomial> resource, Polynomial profile, Runnable action) {
    consuming(resource, profile.multiply(polynomial(-1)), action);
  }

  public static void restore(CellResource<Polynomial> resource, double amount) {
    consume(resource, -amount);
  }

  public static void restore(CellResource<Polynomial> resource, double amount, Duration time) {
    consumeUniformly(resource, -amount, time);
  }

  // Non-consumable style operations

  public static void using(CellResource<Polynomial> resource, Polynomial profile, Runnable action) {
    resource.emit(effect($ -> $.subtract(profile)));
    final Duration start = Resources.currentTime();
    action.run();
    final Duration elapsedTime = Resources.currentTime().minus(start);
    // Reset by adding a counteracting profile
    final Polynomial counteractingProfile = profile.step(elapsedTime);
    resource.emit(effect($ -> $.add(counteractingProfile)));
  }

  public static void using(CellResource<Polynomial> resource, double amount, Runnable action) {
    using(resource, polynomial(amount), action);
  }

  public static void using(CellResource<Polynomial> resource, double amount, Duration time) {
    using(resource, amount, () -> delay(time));
  }

  // Consumable style operations

  public static void consuming(UnitAware<CellResource<Polynomial>> resource, UnitAware<Polynomial> profile, Runnable action) {
    consuming(resource.value(), profile.value(resource.unit()), action);
  }

  public static void consume(UnitAware<CellResource<Polynomial>> resource, UnitAware<Double> amount) {
    consume(resource.value(), amount.value(resource.unit()));
  }

  public static void consume(UnitAware<CellResource<Polynomial>> resource, UnitAware<Double> amount, Duration time) {
    consumeUniformly(resource.value(), amount.value(resource.unit()), time);
  }

  public static void restoring(UnitAware<CellResource<Polynomial>> resource, UnitAware<Polynomial> profile, Runnable action) {
    restoring(resource.value(), profile.value(resource.unit()), action);
  }

  public static void restore(UnitAware<CellResource<Polynomial>> resource, UnitAware<Double> amount) {
    restore(resource.value(), amount.value(resource.unit()));
  }

  public static void restore(UnitAware<CellResource<Polynomial>> resource, UnitAware<Double> amount, Duration time) {
    restore(resource.value(), amount.value(resource.unit()), time);
  }

  // Non-consumable style operations

  // Ugly $ suffix to avoid overload conflict because of erasure.
  public static void using$(UnitAware<CellResource<Polynomial>> resource, UnitAware<Polynomial> profile, Runnable action) {
    using(resource.value(), profile.value(resource.unit()), action);
  }

  public static void using(UnitAware<CellResource<Polynomial>> resource, UnitAware<Double> amount, Runnable action) {
    using(resource.value(), amount.value(resource.unit()), action);
  }
}
