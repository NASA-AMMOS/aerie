package gov.nasa.jpl.ammos.mpsa.aerie.simulation.prototype;

import java.util.List;

public final class PrototypeDriver {
  public static void main(final String[] args) {
    final var systemModel = new DataSystemModel(
        List.of(0.0, 0.0),  // initial volumes
        List.of(1.0, 2.0)); // initial rates (units per second)

    System.out.println("# Autonomous system (no effects)");
    for (int i = 0; i < 10; i += 1) {
      System.out.printf("Time %d: %4.1f\t%4.1f\n",
          i,
          systemModel.getVolumeOfChannelAtTime(0, i),
          systemModel.getVolumeOfChannelAtTime(1, i));
    }

    System.out.println();
    System.out.println("# Controlled system (channel 0 rate += 2.0 at instant 5.0)");

    systemModel.alterRateOfChannelAtTimeByDelta(0, 5.0, 2.0);

    for (int i = 0; i < 10; i += 1) {
      System.out.printf("Time %d: %4.1f\t%4.1f\n",
          i,
          systemModel.getVolumeOfChannelAtTime(0, i),
          systemModel.getVolumeOfChannelAtTime(1, i));
    }
  }
}
