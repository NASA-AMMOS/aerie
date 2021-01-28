package gov.nasa.jpl.aerie.fooadaptation.models;

import gov.nasa.jpl.aerie.contrib.models.Accumulator;
import gov.nasa.jpl.aerie.contrib.models.Register;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.resources.discrete.DiscreteResource;
import gov.nasa.jpl.aerie.merlin.framework.resources.real.RealResource;
import org.apache.commons.lang3.tuple.Pair;

public final class Imager {
  private final double bitsPerPixel;

  private final Register<ImagerMode> mode;
  private final Register<Double> frameRate;
  private final Accumulator imagedBits;

  public final DiscreteResource<ImagerMode> imagerMode;
  public final DiscreteResource<Pair<Integer, Integer>> imagerResPx;
  public final RealResource imagerDataRate;
  public final RealResource imagerDataVolume;
  public final DiscreteResource<Boolean> imagingInProgress;
  public final DiscreteResource<Boolean> conflicted;

  public Imager(final Registrar registrar, final double bitsPerPixel, final ImagerMode mode, final double frameRate) {
    this.bitsPerPixel = bitsPerPixel;

    this.mode = Register.create(registrar.descend("mode"), mode);
    this.frameRate = Register.create(registrar.descend("frameRate"), frameRate);
    this.imagedBits = new Accumulator(registrar.descend("imagedBits"));

    this.imagerMode = this.mode.value;
    this.imagingInProgress = this.imagerMode.map($ -> $ != ImagerMode.OFF);
    this.imagerResPx = this.imagerMode.map(ImagerMode::getResolution);
    this.imagerDataVolume = this.imagedBits.volume.resource;
    this.imagerDataRate = this.imagedBits.rate.resource;
    this.conflicted = this.mode.conflicted;
  }

  public void beginImaging(final ImagerMode mode, final double frameRate) {
    final var newRate = mode.getTotalPixels() * this.bitsPerPixel * frameRate;

    this.mode.set(mode);
    this.frameRate.set(frameRate);
    this.imagedBits.rate.add(newRate - this.imagedBits.rate.get());
  }

  public void endImaging() {
    this.mode.set(ImagerMode.OFF);
    this.frameRate.set(0.0);
    this.imagedBits.rate.add(-this.imagedBits.rate.get());
  }
}
