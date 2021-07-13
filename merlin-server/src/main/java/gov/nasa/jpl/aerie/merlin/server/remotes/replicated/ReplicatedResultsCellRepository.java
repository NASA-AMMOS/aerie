package gov.nasa.jpl.aerie.merlin.server.remotes.replicated;

import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.remotes.ResultsCellRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Replicates logical operations on a primary repository to a list of secondary repositories.
 */
// TODO: Emit a log message whenever a secondary fails or disagrees with the primary.
public final class ReplicatedResultsCellRepository implements ResultsCellRepository {
  private final ResultsCellRepository primary;
  private final List<? extends ResultsCellRepository> secondaries;

  public ReplicatedResultsCellRepository(
      final ResultsCellRepository primary,
      final List<? extends ResultsCellRepository> secondaries
  ) {
    this.primary = primary;
    this.secondaries = new ArrayList<>(secondaries);
  }

  @Override
  public ResultsProtocol.OwnerRole allocate(final String planId, final long planRevision) {
    final var primaryCell = this.primary.allocate(planId, planRevision);

    final var secondaryCells = new ArrayList<ResultsProtocol.OwnerRole>(this.secondaries.size());
    for (final var secondary : this.secondaries) secondaryCells.add(secondary.allocate(planId, planRevision));

    return new OwnerRole(primaryCell, secondaryCells);
  }

  @Override
  public Optional<ResultsProtocol.ReaderRole> lookup(final String planId, final long planRevision) {
    final var primaryCell = this.primary.lookup(planId, planRevision);

    final var secondaryCells = new ArrayList<ResultsProtocol.ReaderRole>(this.secondaries.size());
    for (final var secondary : this.secondaries)
      secondary
          .lookup(planId, planRevision)
          .ifPresent(secondaryCells::add);

    return primaryCell.map($ -> new ReaderRole($, secondaryCells));
  }

  @Override
  public void deallocate(final String planId, final long planRevision) {
    this.primary.deallocate(planId, planRevision);

    for (final var secondary : this.secondaries) secondary.deallocate(planId, planRevision);
  }

  private record ReaderRole(ResultsProtocol.ReaderRole primary, List<? extends ResultsProtocol.ReaderRole> secondaries)
      implements ResultsProtocol.ReaderRole
  {
    @Override
    public ResultsProtocol.State get() {
      final var result = this.primary.get();

      for (final var cell : this.secondaries) cell.get();

      return result;
    }

    @Override
    public void cancel() {
      this.primary.cancel();

      for (final var cell : this.secondaries) cell.cancel();
    }
  }

  private record OwnerRole(ResultsProtocol.OwnerRole primary, List<? extends ResultsProtocol.OwnerRole> secondaries)
      implements ResultsProtocol.OwnerRole
  {
    @Override
    public ResultsProtocol.State get() {
      return new ReaderRole(this.primary, this.secondaries).get();
    }

    @Override
    public void cancel() {
      new ReaderRole(this.primary, this.secondaries).cancel();
    }

    @Override
    public boolean isCanceled() {
      final var result = this.primary.isCanceled();

      for (final var cell : this.secondaries) cell.isCanceled();

      return result;
    }

    @Override
    public void succeedWith(final SimulationResults results) {
      this.primary.succeedWith(results);

      for (final var cell : this.secondaries) cell.succeedWith(results);
    }

    @Override
    public void failWith(final String reason) {
      this.primary.failWith(reason);

      for (final var cell : this.secondaries) cell.failWith(reason);
    }
  }
}
