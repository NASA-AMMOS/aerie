package gov.nasa.jpl.aerie.merlin.server.remotes.replicated;

import java.util.HashMap;
import java.util.Map;

public final class InMemoryIdMappingRepository implements IdMappingRepository {
  private final Map<String, String> mappings;

  public InMemoryIdMappingRepository(final Map<String, String> mappings) {
    this.mappings = new HashMap<>(mappings);
  }

  @Override
  public String lookup(final String primaryId) {
    return this.mappings.get(primaryId);
  }

  @Override
  public void insert(final String primaryId, final String secondaryId) {
    this.mappings.put(primaryId, secondaryId);
  }

  @Override
  public void delete(final String primaryId) {
    this.mappings.remove(primaryId);
  }
}
