package gov.nasa.jpl.ammos.mpsa.aerie.simulation.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.simulation.models.Plan;
import gov.nasa.jpl.ammos.mpsa.aerie.simulation.utils.Milliseconds;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

import static gov.nasa.jpl.ammos.mpsa.aerie.simulation.utils.Milliseconds.ms;

public final class DevPlan implements Plan<DevStates> {
  @Override
  public List<Pair<Milliseconds, Activity<DevStates>>> getActivities() {
    return List.of(
        Pair.of(ms(500), new DevActivity()),
        Pair.of(ms(1000), new DevActivity()),
        Pair.of(ms(1500), new DevActivity()),
        Pair.of(ms(2000), new DevActivity()),
        Pair.of(ms(2500), new DevActivity()),
        Pair.of(ms(3000), new DevActivity()),
        Pair.of(ms(3500), new DevActivity())
    );
  }
}
