package gov.nasa.jpl.aerie.merlin.server.remotes.replicated;

import gov.nasa.jpl.aerie.merlin.server.models.AdaptationJar;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.remotes.AdaptationRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// TODO: Emit a log message whenever a secondary fails or disagrees with the primary.
public final class ReplicatedAdaptationRepository implements AdaptationRepository {
  /**
   * @param adaptationIds An auxiliary repository mapping primary IDs to IDs in this secondary.
   * @param repository The secondary repository itself.
   */
  public record Secondary (IdMappingRepository adaptationIds, AdaptationRepository repository) {}

  private final AdaptationRepository primary;
  private final List<Secondary> secondaries;

  public ReplicatedAdaptationRepository(final AdaptationRepository primary, final List<Secondary> secondaries) {
    this.primary = primary;
    this.secondaries = new ArrayList<>(secondaries);
  }

  @Override
  public Map<String, AdaptationJar> getAllAdaptations() {
    final var result = this.primary.getAllAdaptations();

    for (final var secondary : this.secondaries) secondary.repository().getAllAdaptations();

    return result;
  }

  @Override
  public AdaptationJar getAdaptation(final String adaptationId) throws NoSuchAdaptationException {
    final var result = this.primary.getAdaptation(adaptationId);

    for (final var secondary : this.secondaries) {
      secondary.repository().getAdaptation(secondary.adaptationIds().lookup(adaptationId));
    }

    return result;
  }

  @Override
  public Map<String, Constraint> getConstraints(final String adaptationId) throws NoSuchAdaptationException {
    final var result = this.primary.getConstraints(adaptationId);

    for (final var secondary : this.secondaries) {
      secondary.repository().getConstraints(secondary.adaptationIds().lookup(adaptationId));
    }

    return result;
  }

  @Override
  public String createAdaptation(final AdaptationJar adaptationJar) {
    final var primaryId = this.primary.createAdaptation(adaptationJar);

    for (final var secondary : this.secondaries) {
      final var secondaryId = secondary.repository().createAdaptation(adaptationJar);
      secondary.adaptationIds().insert(primaryId, secondaryId);
    }

    return primaryId;
  }

  @Override
  public void deleteAdaptation(final String adaptationId) throws NoSuchAdaptationException {
    this.primary.deleteAdaptation(adaptationId);

    for (final var secondary : this.secondaries) {
      final var secondaryId = secondary.adaptationIds().lookup(adaptationId);

      secondary.adaptationIds().delete(adaptationId);
      secondary.repository().deleteAdaptation(secondaryId);
    }
  }

  @Override
  public void replaceConstraints(final String adaptationId, final Map<String, Constraint> constraints)
  throws NoSuchAdaptationException
  {
    this.primary.replaceConstraints(adaptationId, constraints);

    for (final var secondary : this.secondaries) {
      secondary.repository().replaceConstraints(secondary.adaptationIds().lookup(adaptationId), constraints);
    }
  }

  @Override
  public void deleteConstraint(final String adaptationId, final String constraintId) throws NoSuchAdaptationException {
    this.primary.deleteConstraint(adaptationId, constraintId);

    for (final var secondary : this.secondaries) {
      secondary.repository().deleteConstraint(secondary.adaptationIds().lookup(adaptationId), constraintId);
    }
  }
}
