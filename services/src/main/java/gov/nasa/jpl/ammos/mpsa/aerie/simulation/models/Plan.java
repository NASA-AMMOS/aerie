package gov.nasa.jpl.ammos.mpsa.aerie.simulation.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.simulation.utils.Milliseconds;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public interface Plan<States extends StateContainer> {
  List<Pair<Milliseconds, Activity<States>>> getActivities();
}
