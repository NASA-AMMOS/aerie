package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import gov.nasa.jpl.aerie.scheduler.server.ResultsProtocol;
import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchRequestException;
import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchSpecificationException;
import gov.nasa.jpl.aerie.scheduler.server.models.SpecificationId;
import gov.nasa.jpl.aerie.scheduler.server.remotes.ResultsCellRepository;
import gov.nasa.jpl.aerie.scheduler.server.services.ScheduleResults;
import org.apache.commons.lang3.NotImplementedException;

public final class PostgresResultsCellRepository implements ResultsCellRepository {
  private final DataSource dataSource;

  public PostgresResultsCellRepository(final DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public ResultsProtocol.OwnerRole allocate(final SpecificationId specificationId)
  {
    try (final var connection = this.dataSource.getConnection()) {
      final var spec = getSpecification(connection, specificationId);
      final var request = createRequest(connection, spec);

      return new PostgresResultsCell(
          this.dataSource,
          new SpecificationId(request.specificationId()),
          request.specificationRevision(),
          request.analysisId());
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to get schedule specification", ex);
    } catch (final NoSuchSpecificationException ex) {
      throw new Error("Cannot allocate scheduling cell for nonexistent specification", ex);
    }
  }

  @Override
  public Optional<ResultsProtocol.ReaderRole> lookup(final SpecificationId specificationId)
  {
    try (final var connection = this.dataSource.getConnection()) {
      final var spec = getSpecification(connection, specificationId);
      final var request = getRequest(connection, specificationId, spec.revision());

      return Optional.of(new PostgresResultsCell(
          this.dataSource,
          new SpecificationId(request.specificationId()),
          request.specificationRevision(),
          request.analysisId()));
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to get schedule specification", ex);
    } catch (final NoSuchSpecificationException | NoSuchRequestException ex) {
      return Optional.empty();
    }
  }

  @Override
  public void deallocate(final ResultsProtocol.OwnerRole resultsCell)
  {
    if (!(resultsCell instanceof PostgresResultsCell cell)) {
      throw new Error("Unable to deallocate results cell of unknown type");
    }
    try (final var connection = this.dataSource.getConnection()) {
      deleteRequest(connection, cell.specId, cell.specRevision);
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to delete scheduling request", ex);
    }
  }

  private static SpecificationRecord getSpecification(final Connection connection, final SpecificationId specificationId)
  throws SQLException, NoSuchSpecificationException {
    try (final var getSpecificationAction = new GetSpecificationAction(connection)) {
      return getSpecificationAction
          .get(specificationId.id())
          .orElseThrow(() -> new NoSuchSpecificationException(specificationId));
    }
  }

  private static RequestRecord getRequest(
      final Connection connection,
      final SpecificationId specificationId,
      final long specificationRevision
  ) throws SQLException, NoSuchRequestException {
    try (final var getRequestAction = new GetRequestAction(connection)) {
      return getRequestAction
          .get(specificationId.id(), specificationRevision)
          .orElseThrow(() -> new NoSuchRequestException(specificationId, specificationRevision));
    }
  }

  private static RequestRecord createRequest(
      final Connection connection,
      final SpecificationRecord specification
  ) throws SQLException {
    try (final var createRequestAction = new CreateRequestAction(connection)) {
      return createRequestAction.apply(specification);
    }
  }

  private static void deleteRequest(
      final Connection connection,
      final SpecificationId specId,
      final long specRevision
  ) throws SQLException {
    try (final var deleteRequestAction = new DeleteRequestAction(connection)) {
      deleteRequestAction.apply(specId.id(), specRevision);
    }
  }

  public static final class PostgresResultsCell implements ResultsProtocol.OwnerRole {
    private final DataSource dataSource;
    private final SpecificationId specId;
    private final long specRevision;
    private final long analysisId;

    public PostgresResultsCell(
        final DataSource dataSource,
        final SpecificationId specId,
        final long specRevision,
        final long analysisId
    ) {
      this.dataSource = dataSource;
      this.specId = specId;
      this.specRevision = specRevision;
      this.analysisId = analysisId;
    }

    @Override
    public ResultsProtocol.State get() {
      // TODO
      throw new NotImplementedException("PostgresResultsCell not yet implemented");
    }

    @Override
    public void cancel() {
      // TODO
      throw new NotImplementedException("PostgresResultsCell not yet implemented");
    }

    @Override
    public boolean isCanceled() {
      // TODO
      throw new NotImplementedException("PostgresResultsCell not yet implemented");
    }

    @Override
    public void succeedWith(final ScheduleResults results) {
      // TODO
      throw new NotImplementedException("PostgresResultsCell not yet implemented");
    }

    @Override
    public void failWith(final String reason) {
      // TODO
      throw new NotImplementedException("PostgresResultsCell not yet implemented");
    }
  }
}
