package gov.nasa.jpl.ammos.mpsa.aerie.simulation.prototype;

public final class DataMissionModel {
  private final DataSystemModel model;

  public DataMissionModel(final DataSystemModel model) {
    this.model = model;
  }

  public VolumeState getVolumeStateAtInstant(final int channel, final double instant) {
    return new VolumeState(channel, instant);
  }

  public RateState getRateStateAtInstant(final int channel, final double instant) {
    return new RateState(channel, instant);
  }

  public final class VolumeState {
    private final int channel;
    private final double instant;

    private VolumeState(final int channel, final double instant) {
      this.channel = channel;
      this.instant = instant;
    }

    public Double get() {
      return model.getVolumeOfChannelAtTime(channel, instant);
    }
  }

  public final class RateState {
    public final int channel;
    public final double instant;

    private RateState(final int channel, final double instant) {
      this.channel = channel;
      this.instant = instant;
    }

    public double get() {
      return model.getRateOfChannelAtTime(channel, instant);
    }

    public void increment(final double delta) {
      model.alterRateOfChannelAtTimeByDelta(channel, instant, delta);
    }

    public void decrement(final double delta) {
      model.alterRateOfChannelAtTimeByDelta(channel, instant, -delta);
    }
  }
}
