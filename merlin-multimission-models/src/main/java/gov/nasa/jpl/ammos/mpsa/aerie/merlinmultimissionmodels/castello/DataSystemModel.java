package gov.nasa.jpl.ammos.mpsa.aerie.simulation.prototype;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public final class DataSystemModel {
  private final StateVector initialState;
  private final List<Pair<Double, Effect>> scheduledEffects = new ArrayList<>();

  public DataSystemModel(final List<Double> initialRates, final List<Double> initialVolumes) {
    if (initialRates.size() != initialVolumes.size()) {
      throw new Error("Number of initial rates must equal number of initial volumes");
    }

    this.initialState = new StateVector(
      Owned.of(new ArrayList<>(initialRates)),
      Owned.of(new ArrayList<>(initialVolumes)));
  }

  public double getRateOfChannelAtTime(final int channelId, final double instant) {
    if (instant < 0.0) throw new Error("Cannot query instants prior to 0");

    final ChannelStateVector currentState = new ChannelStateVector(this.initialState, channelId);
    currentState.step(instant, this.scheduledEffects);
    return currentState.rate;
  }

  public double getVolumeOfChannelAtTime(final int channelId, final double instant) {
    if (instant < 0.0) throw new Error("Cannot query instants prior to 0");

    final ChannelStateVector currentState = new ChannelStateVector(this.initialState, channelId);
    currentState.step(instant, this.scheduledEffects);
    return currentState.volume;
  }

  public void alterRateOfChannelAtTimeByDelta(final int channelId, final double instant, final double delta) {
    if (instant < 0.0) throw new Error("Cannot affect instants prior to 0");

    int i = this.scheduledEffects.size() - 1;
    for (; i >= 0; i -= 1) {
      final var entry = this.scheduledEffects.get(i);
      if (entry.getLeft() <= instant) break;
    }

    this.scheduledEffects.add(i+1, Pair.of(instant, new Effect(channelId, delta)));
  }

  private static final class Effect {
    private final int channelId;
    private final double delta;

    public Effect(final int channelId, final double delta) {
      this.channelId = channelId;
      this.delta = delta;
    }
  }

  private static final class StateVector {
    public final List<Double> volumes;
    public final List<Double> rates;

    public StateVector(final Owned<? extends List<Double>> volumes, final Owned<? extends List<Double>> rates) {
      this.volumes = volumes.ref;
      this.rates = rates.ref;
    }

    public StateVector(final StateVector other) {
      this.volumes = List.copyOf(other.volumes);
      this.rates = List.copyOf(other.rates);
    }
  }

  private static final class ChannelStateVector {
    public int channelIndex;
    public double volume;
    public double rate;

    public ChannelStateVector(final StateVector other, final int channelIndex) {
      this.channelIndex = channelIndex;
      this.volume = other.volumes.get(channelIndex);
      this.rate = other.rates.get(channelIndex);
    }

    public void step(final double duration, final List<Pair<Double, Effect>> effects) {
      int nextEffectIndex = 0;

      double totalElapsedTime = 0.0;
      while (totalElapsedTime < duration) {
        // Skip past any effects that are unrelated to this channel.
        while (nextEffectIndex < effects.size()) {
          if (effects.get(nextEffectIndex).getRight().channelId == this.channelIndex) break;
          nextEffectIndex += 1;
        }

        // Determine when the next event point is.
        final boolean hasNextEffect = (nextEffectIndex < effects.size());
        final double nextEffectTime = (hasNextEffect)
          ? effects.get(nextEffectIndex).getLeft()
          : Double.POSITIVE_INFINITY;
        final double nextEventTime = Math.min(nextEffectTime, duration);

        // Step through time to the instant of this event.
        this.volume += (nextEventTime - totalElapsedTime) * this.rate;
        totalElapsedTime = nextEventTime;

        // If this is an effect, update the current rate with it.
        if (hasNextEffect) {
          this.rate += effects.get(nextEffectIndex).getRight().delta;
          nextEffectIndex += 1;
        }
      }
    }
  }
}
