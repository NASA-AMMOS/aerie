package gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes;

import org.apache.commons.lang3.NotImplementedException;

import java.util.Optional;

public final class RemoteAdaptationService implements AdaptationService {
  @Override
  public Optional<Adaptation> getAdaptationById(String adaptationId) {
    throw new NotImplementedException("TODO: communicate with remote service");
  }
}
