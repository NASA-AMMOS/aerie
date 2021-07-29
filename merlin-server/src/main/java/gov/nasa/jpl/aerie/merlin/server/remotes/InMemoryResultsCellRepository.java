package gov.nasa.jpl.aerie.merlin.server.remotes;

import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class InMemoryResultsCellRepository implements ResultsCellRepository {
  public record Key(String planId, long planRevision) {}

  private final Map<InMemoryResultsCellRepository.Key, InMemoryCell> cells = new HashMap<>();


  @Override
  public ResultsProtocol.OwnerRole allocate(final String planId, final long planRevision) {
    final var cell = new InMemoryCell();
    this.cells.put(new InMemoryResultsCellRepository.Key(planId, planRevision), cell);
    return cell;
  }

  @Override
  public Optional<ResultsProtocol.ReaderRole> lookup(final String planId, final long planRevision) {
    return Optional.ofNullable(this.cells.get(new InMemoryResultsCellRepository.Key(planId, planRevision)));
  }

  @Override
  public void deallocate(final String planId, final long planRevision) {
    this.cells.remove(new InMemoryResultsCellRepository.Key(planId, planRevision));
  }

  public boolean isEqualTo(final InMemoryResultsCellRepository other) {
    return this.cells.equals(other.cells);
  }

  /** @deprecated Use {@link #isEqualTo(InMemoryResultsCellRepository)}. */
  @Deprecated
  @Override
  public boolean equals(final Object other) {
    if (!(other instanceof InMemoryResultsCellRepository o)) return false;
    return this.isEqualTo(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.cells);
  }

  @Override
  public String toString() {
    return this.cells.toString();
  }

  public static final class InMemoryCell implements ResultsProtocol.OwnerRole {
    private volatile boolean canceled = false;
    private volatile ResultsProtocol.State state = new ResultsProtocol.State.Incomplete();

    @Override
    public ResultsProtocol.State get() {
      return this.state;
    }

    @Override
    public void cancel() {
      this.canceled = true;
    }

    @Override
    public boolean isCanceled() {
      return this.canceled;
    }

    @Override
    public void succeedWith(final SimulationResults results) {
      if (!(this.state instanceof ResultsProtocol.State.Incomplete)) {
        throw new IllegalStateException("Cannot transition to success state from state %s".formatted(
            this.state.getClass().getCanonicalName()));
      }

      this.state = new ResultsProtocol.State.Success(results);
    }

    @Override
    public void failWith(final String reason) {
      if (!(this.state instanceof ResultsProtocol.State.Incomplete)) {
        throw new IllegalStateException("Cannot transition to failed state from state %s".formatted(
            this.state.getClass().getCanonicalName()));
      }

      this.state = new ResultsProtocol.State.Failed(reason);
    }

    public boolean isEqualTo(final InMemoryCell other) {
      if (this.canceled != other.canceled) return false;
      if (!Objects.equals(this.state, other.state)) return false;

      return true;
    }

    @Deprecated
    @Override
    public boolean equals(final Object other) {
      if (!(other instanceof InMemoryCell o)) return false;
      return this.isEqualTo(o);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.canceled, this.state);
    }
  }
}
