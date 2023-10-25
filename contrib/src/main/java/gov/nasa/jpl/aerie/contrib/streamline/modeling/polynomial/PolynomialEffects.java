package gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial;

import gov.nasa.jpl.aerie.contrib.streamline.core.Resources;
import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.StandardUnits;
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
    resource.emit(
        "Consume %.1e discretely".formatted(amount),
        effect($ -> $.subtract(polynomial(amount))));
  }

  /**
   * Consume resource according to a given polynomial profile while an action runs.
   */
  public static void consuming(CellResource<Polynomial> resource, Polynomial profile, Runnable action) {
    withConsumableEffects("consuming", resource, profile, action);
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

  /**
   * Restore resource according to a given polynomial profile while an action runs.
   */
  public static void restoring(CellResource<Polynomial> resource, Polynomial profile, Runnable action) {
    withConsumableEffects("restoring", resource, profile.multiply(polynomial(-1)), action);
  }

  /**
   * Consume some amount of a resource instantaneously.
   */
  public static void restore(CellResource<Polynomial> resource, double amount) {
    resource.emit(
        "Restore %.1e discretely".formatted(amount),
        effect($ -> $.add(polynomial(amount))));
  }

  /**
   * Restore some amount of a resource at a uniform rate over a fixed period of time.
   */
  public static void restoreUniformly(CellResource<Polynomial> resource, double amount, Duration time) {
    restore(resource, amount / time.ratioOver(SECOND), time);
  }

  /**
   * Restore some resource a fixed rate during an action
   */
  public static void restoring(CellResource<Polynomial> resource, double rate, Runnable action) {
    restoring(resource, polynomial(0, rate), action);
  }

  /**
   * Restore some resource at a fixed rate for a fixed period of time, asynchronously.
   */
  public static void restore(CellResource<Polynomial> resource, double rate, Duration time) {
    spawn(replaying(() -> restoring(resource, rate, () -> delay(time))));
  }

  private static void withConsumableEffects(String verb, CellResource<Polynomial> resource, Polynomial profile, Runnable action) {
    resource.emit("Start %s according to profile %s".formatted(verb, profile), effect($ -> $.subtract(profile)));
    final Duration start = Resources.currentTime();
    action.run();
    final Duration elapsedTime = Resources.currentTime().minus(start);
    // Nullify ongoing effects by adding a profile with the same behavior,
    // but with an initial value of 0
    final Polynomial steppedProfile = profile.step(elapsedTime);
    final Polynomial counteractingProfile = steppedProfile.subtract(polynomial(steppedProfile.extract()));
    resource.emit("End %s according to profile %s".formatted(verb, profile), effect($ -> $.add(counteractingProfile)));
  }

  // Non-consumable style operations

  /**
   * Decrease resource according to a given polynomial profile while an action runs,
   * restoring the resource to its original profile when the action completes.
   */
  public static void using(CellResource<Polynomial> resource, Polynomial profile, Runnable action) {
    withNonConsumableEffect("using", resource, profile, action);
  }

  /**
   * Decrease resource by a fixed amount while an action runs,
   * restoring the resource to its original profile when the action completes.
   */
  public static void using(CellResource<Polynomial> resource, double amount, Runnable action) {
    using(resource, polynomial(amount), action);
  }

  /**
   * Decrease resource by a fixed amount for a fixed time,
   * restoring the resource to its original profile when the action completes.
   */
  public static void using(CellResource<Polynomial> resource, double amount, Duration time) {
    using(resource, amount, () -> delay(time));
  }

  /**
   * Increase resource according to a given polynomial profile while an action runs,
   * restoring the resource to its original profile when the action completes.
   */
  public static void providing(CellResource<Polynomial> resource, Polynomial profile, Runnable action) {
    withNonConsumableEffect("providing", resource, profile.multiply(polynomial(-1)), action);
  }

  /**
   * Increase resource by a fixed amount while an action runs,
   * restoring the resource to its original profile when the action completes.
   */
  public static void providing(CellResource<Polynomial> resource, double amount, Runnable action) {
    providing(resource, polynomial(amount), action);
  }

  /**
   * Increase resource by a fixed amount for a fixed time,
   * restoring the resource to its original profile when the action completes.
   */
  public static void providing(CellResource<Polynomial> resource, double amount, Duration time) {
    providing(resource, amount, () -> delay(time));
  }

  private static void withNonConsumableEffect(String verb, CellResource<Polynomial> resource, Polynomial profile, Runnable action) {
    resource.emit("Start %s profile %s".formatted(verb, profile), effect($ -> $.subtract(profile)));
    final Duration start = Resources.currentTime();
    action.run();
    final Duration elapsedTime = Resources.currentTime().minus(start);
    // Reset by adding a counteracting profile
    final Polynomial counteractingProfile = profile.step(elapsedTime);
    resource.emit("Finish %s profile %s".formatted(verb, profile), effect($ -> $.add(counteractingProfile)));
  }

  // Consumable style operations

  /**
   * Consume some amount of a resource instantaneously.
   */
  public static void consume(UnitAware<CellResource<Polynomial>> resource, UnitAware<Double> amount) {
    consume(resource.value(), amount.value(resource.unit()));
  }

  /**
   * Consume resource according to a given polynomial profile while an action runs.
   */
  public static void consuming$(UnitAware<CellResource<Polynomial>> resource, UnitAware<Polynomial> profile, Runnable action) {
    consuming(resource.value(), profile.value(resource.unit()), action);
  }

  /**
   * Consume some amount of a resource at a uniform rate over a fixed period of time.
   */
  public static void consumeUniformly(UnitAware<CellResource<Polynomial>> resource, UnitAware<Double> amount, Duration time) {
    consumeUniformly(resource.value(), amount.value(resource.unit()), time);
  }

  /**
   * Consume some resource a fixed rate during an action
   */
  public static void consuming(UnitAware<CellResource<Polynomial>> resource, UnitAware<Double> rate, Runnable action) {
    consuming(resource.value(), rate.value(resource.unit().divide(StandardUnits.SECOND)), action);
  }

  /**
   * Consume some resource at a fixed rate for a fixed period of time, asynchronously.
   */
  public static void consume(UnitAware<CellResource<Polynomial>> resource, UnitAware<Double> rate, Duration time) {
    spawn(replaying(() -> consuming(resource, rate, () -> delay(time))));
  }

  /**
   * Restore resource according to a given polynomial profile while an action runs.
   */
  public static void restoring$(UnitAware<CellResource<Polynomial>> resource, UnitAware<Polynomial> profile, Runnable action) {
    restoring(resource.value(), profile.value(resource.unit()), action);
  }

  /**
   * Restore some amount of a resource instantaneously.
   */
  public static void restore(UnitAware<CellResource<Polynomial>> resource, UnitAware<Double> amount) {
    restore(resource.value(), amount.value(resource.unit()));
  }

  /**
   * Restore some amount of a resource at a uniform rate over a fixed period of time.
   */
  public static void restoreUniformly(UnitAware<CellResource<Polynomial>> resource, UnitAware<Double> amount, Duration time) {
    restoreUniformly(resource.value(), amount.value(resource.unit()), time);
  }

  /**
   * Restore some resource a fixed rate during an action
   */
  public static void restoring(UnitAware<CellResource<Polynomial>> resource, UnitAware<Double> rate, Runnable action) {
    restoring(resource.value(), rate.value(resource.unit().divide(StandardUnits.SECOND)), action);
  }

  /**
   * Restore some resource at a fixed rate for a fixed period of time, asynchronously.
   */
  public static void restore(UnitAware<CellResource<Polynomial>> resource, UnitAware<Double> rate, Duration time) {
    restore(resource.value(), rate.value(resource.unit()), time);
  }

  // Non-consumable style operations

  /**
   * Decrease resource according to a given polynomial profile while an action runs,
   * restoring the resource to its original profile when the action completes.
   */
  public static void using$(UnitAware<CellResource<Polynomial>> resource, UnitAware<Polynomial> profile, Runnable action) {
    using(resource.value(), profile.value(resource.unit()), action);
  }

  /**
   * Decrease resource by a fixed amount while an action runs,
   * restoring the resource to its original profile when the action completes.
   */
  public static void using(UnitAware<CellResource<Polynomial>> resource, UnitAware<Double> amount, Runnable action) {
    using(resource.value(), amount.value(resource.unit()), action);
  }

  /**
   * Decrease resource by a fixed amount for a fixed time,
   * restoring the resource to its original profile when the action completes.
   */
  public static void using(UnitAware<CellResource<Polynomial>> resource, UnitAware<Double> amount, Duration time) {
    using(resource.value(), amount.value(resource.unit()), time);
  }

  /**
   * Increase resource according to a given polynomial profile while an action runs,
   * restoring the resource to its original profile when the action completes.
   */
  public static void providing$(UnitAware<CellResource<Polynomial>> resource, UnitAware<Polynomial> profile, Runnable action) {
    providing(resource.value(), profile.value(resource.unit()), action);
  }

  /**
   * Increase resource by a fixed amount while an action runs,
   * restoring the resource to its original profile when the action completes.
   */
  public static void providing(UnitAware<CellResource<Polynomial>> resource, UnitAware<Double> amount, Runnable action) {
    providing(resource.value(), amount.value(resource.unit()), action);
  }

  /**
   * Increase resource by a fixed amount for a fixed time,
   * restoring the resource to its original profile when the action completes.
   */
  public static void providing(UnitAware<CellResource<Polynomial>> resource, UnitAware<Double> amount, Duration time) {
    providing(resource.value(), amount.value(resource.unit()), time);
  }
}
