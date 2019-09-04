package gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes;

import org.apache.commons.lang3.NotImplementedException;

import java.util.Optional;
import java.util.stream.Stream;

public final class RemotePlanRepository implements PlanRepository {
  @Override
  public PlanTransaction newPlan() {
    throw new NotImplementedException("TODO: communicate with remote service");
  }

  @Override
  public Optional<PlanTransaction> getPlan(final String id) {
    throw new NotImplementedException("TODO: communicate with remote service");
  }

  @Override
  public Stream<PlanTransaction> getAllPlans() {
    throw new NotImplementedException("TODO: communicate with remote service");
  }
}
