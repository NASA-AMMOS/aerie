package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Resource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

public interface SimulationContext<$Timeline, Activity, ActivityTask> {
  /* Produce */ ActivityTask
  /* Given   */ constructActivityTask(Activity activity);

  /* Produce */ ActivityTask
  /* Given   */ duplicateActivityTask(ActivityTask task);

  /* Produce */ Map<String, ? extends Pair<ValueSchema, ? extends Resource<History<$Timeline, ?>, SerializedValue>>>
  /* Given   */ getDiscreteResources();

  /* Produce */ Map<String, ? extends Resource<History<$Timeline, ?>, RealDynamics>>
  /* Given   */ getRealResources();

  // List<ViolableConstraint> getViolableConstraints();
}
