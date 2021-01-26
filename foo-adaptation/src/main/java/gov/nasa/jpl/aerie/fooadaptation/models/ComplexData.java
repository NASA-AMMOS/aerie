package gov.nasa.jpl.aerie.fooadaptation.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import gov.nasa.jpl.aerie.contrib.cells.register.RegisterCell;
import gov.nasa.jpl.aerie.contrib.models.Accumulator;
import gov.nasa.jpl.aerie.fooadaptation.generated.Model;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.resources.discrete.DiscreteResource;
import gov.nasa.jpl.aerie.merlin.framework.resources.real.RealResource;
import gov.nasa.jpl.aerie.merlin.protocol.RealDynamics;
import org.apache.commons.lang3.tuple.Pair;

public final class ComplexData extends Model {

  public enum ImagerHardwareState {
    OFF,
    ON;
  }

  public enum ImagerResMode {
    LOW_RES,
    MED_RES,
    HI_RES
  }

  /** Simplifies instantiation of a cell reference, and associated resource, and an optional list of listeners. */
  public static class MutableResource<T, R> {
    private final List<Consumer<T>> listeners = new ArrayList<>();
    private final CellRef<Pair<Optional<T>, Set<T>>, RegisterCell<T>> ref;
    public final R resource;

    public MutableResource(final Registrar registrar, final T initValue, final Function<CellRef<Pair<Optional<T>, Set<T>>, RegisterCell<T>>, R> resourceMapper) {
      this.ref = registrar.cell(new RegisterCell<>(initValue));
      this.resource = resourceMapper.apply(this.ref);
    }

    public void set(final T newValue) {
      ref.emit(Pair.of(Optional.of(newValue), Set.of(newValue)));
      this.listeners.forEach(l -> l.accept(newValue));
    }

    public void addListener(final Consumer<T> listener) {
      this.listeners.add(listener);
    }

    public static <T> MutableResource<T, DiscreteResource<T>> discrete(final Registrar registrar, final T initValue) {
      return new MutableResource<>(registrar, initValue, (ref) -> DiscreteResource.atom(ref, RegisterCell::getValue));
    }

    public static MutableResource<Double, RealResource> real(final Registrar registrar, final Double initValue) {
      return new MutableResource<>(registrar, initValue, (ref) -> RealResource.atom(ref, (c) -> RealDynamics.constant(c.getValue())));
    }
  }

  private final double imagerBpp; // Imager bits/pixel

  // Internal accumulator
  private final Accumulator imagerDataAcc;

  // Exposed mutable resources
  public final MutableResource<ImagerResMode, DiscreteResource<ImagerResMode>> imagerResMode;
  public final MutableResource<Double, RealResource> imagerFrameRate;
  public final MutableResource<ImagerHardwareState, DiscreteResource<ImagerHardwareState>> imagerHardwareState;
  public final MutableResource<Boolean, DiscreteResource<Boolean>> imagingInProgress;

  // Exposed derived resources
  public final DiscreteResource<Pair<Integer, Integer>> imagerResPx;
  public final RealResource imagerDataRate;
  public final RealResource imagerDataVolume;

  public ComplexData(
      final Registrar registrar,
      final double imagerBpp,
      final ImagerResMode initImagerResMode,
      final double initImagerFrameRate) {
    super(registrar);

    this.imagerBpp = imagerBpp;

    // Mutable
    this.imagerResMode = MutableResource.discrete(registrar, initImagerResMode);
    this.imagerFrameRate = MutableResource.real(registrar, initImagerFrameRate);
    this.imagerHardwareState = MutableResource.discrete(registrar, ImagerHardwareState.OFF);
    this.imagingInProgress = MutableResource.discrete(registrar, false);

    // Derived
    this.imagerResPx = this.imagerResMode.resource.map(this::getCorrespondingImagerResPx);
    this.imagerDataAcc = new Accumulator(registrar, 0, 0);
    this.imagerHardwareState.addListener((state) -> this.imagerDataAcc.rate.add(calculateImagerDataRate(state) - this.imagerDataAcc.rate.get()));
    this.imagerDataRate = this.imagerDataAcc.rate.resource;
    this.imagerDataVolume = this.imagerDataAcc.volume.resource;
  }

  /** Get an imager resolution width/height given the imager's resolution mode. */
  private Pair<Integer, Integer> getCorrespondingImagerResPx(final ImagerResMode mode) {
    switch (mode) {
      case LOW_RES:
        return Pair.of(640, 480);
      case MED_RES:
        return Pair.of(960, 720);
      case HI_RES:
        return Pair.of(1280, 960);
    }
    throw new IllegalArgumentException("Unexpected imager resolution mode: "+mode.name());
  }

  /** Get a imager data rate given the imager's hardware state. */
  private double calculateImagerDataRate(final ImagerHardwareState state) {
    if (state == ImagerHardwareState.ON) {
      // Derive rate from other resources
      final var px = imagerResPx.ask();
      return px.getLeft() * px.getRight() * imagerBpp * imagerFrameRate.resource.ask();
    }
    // Otherwise, zero the rate
    return 0;
  }
}
