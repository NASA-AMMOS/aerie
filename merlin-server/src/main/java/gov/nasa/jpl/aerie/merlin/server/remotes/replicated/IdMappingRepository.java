package gov.nasa.jpl.aerie.merlin.server.remotes.replicated;

public interface IdMappingRepository {
  String lookup(String primaryId);
  void insert(String primaryId, String secondaryId);
  void delete(String primaryId);
}
