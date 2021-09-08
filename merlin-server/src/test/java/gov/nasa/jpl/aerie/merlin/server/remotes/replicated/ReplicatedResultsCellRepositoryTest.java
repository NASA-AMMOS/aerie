package gov.nasa.jpl.aerie.merlin.server.remotes.replicated;

import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.remotes.InMemoryResultsCellRepository;
import gov.nasa.jpl.aerie.merlin.server.remotes.ResultsCellRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public final class ReplicatedResultsCellRepositoryTest {
  private static final InMemoryResultsCellRepository.Key KEY = new InMemoryResultsCellRepository.Key("test", 1);

  @Nested
  @DisplayName("The replicated repository should transmit logical operations to the primary repository")
  public final class TransmitsToPrimary {
    @Test
    public void transmitsAllocate() {
      final Function<ResultsCellRepository, ResultsProtocol.OwnerRole> script = (repository) -> {
        return repository.allocate(KEY.planId(), KEY.planRevision());
      };

      final var primary = new InMemoryResultsCellRepository();
      final var expected = new InMemoryResultsCellRepository();

      final var cell0 = script.apply(new ReplicatedResultsCellRepository(primary, List.of()));
      final var cell1 = script.apply(expected);

      assertAll(Stream.of(
          // The repositories themselves should be in the same state.
          () -> assertEquals(expected, primary),
          // The received cells should be in the same state.
          () -> assertEquals(cell0.get(), cell1.get()),
          () -> assertEquals(cell0.isCanceled(), cell1.isCanceled())));
    }

    @Test
    public void transmitsLookup() {
      final Function<ResultsCellRepository, Optional<ResultsProtocol.ReaderRole>> script = (repository) -> {
        repository.allocate(KEY.planId(), KEY.planRevision());
        return repository.lookup(KEY.planId(), KEY.planRevision());
      };

      final var primary = new InMemoryResultsCellRepository();
      final var expected = new InMemoryResultsCellRepository();

      final var cell0 = script.apply(new ReplicatedResultsCellRepository(primary, List.of()));
      final var cell1 = script.apply(expected);

      assertAll(Stream.of(
          // The repositories themselves should be in the same state.
          () -> assertEquals(expected, primary),
          // The received cells should be in the same state.
          () -> assertEquals(cell0.orElseThrow().get(), cell1.orElseThrow().get())));
    }

    @Test
    public void transmitsDeallocate() {
      final Consumer<ResultsCellRepository> script = (repository) -> {
        repository.allocate(KEY.planId(), KEY.planRevision());
        repository.deallocate(KEY.planId(), KEY.planRevision());
      };

      final var primary = new InMemoryResultsCellRepository();
      final var expected = new InMemoryResultsCellRepository();

      script.accept(new ReplicatedResultsCellRepository(primary, List.of()));
      script.accept(expected);

      // The repositories should be in the same state.
      assertEquals(expected, primary);
    }

    @Test
    @Disabled("TODO: Test that the replicated repository transmits cell operations to the primary")
    public void transmitsCellOperations() {
      fail();
    }
  }

  @Nested
  @DisplayName("The primary and its secondaries should be pairwise equivalent")
  public final class SecondariesReflectPrimary {
    @Test
    public void reflectsAllocate() {
      final var primary = new InMemoryResultsCellRepository();
      final var secondaries = List.of(new InMemoryResultsCellRepository(), new InMemoryResultsCellRepository());
      final var replicator = new ReplicatedResultsCellRepository(primary, secondaries);

      replicator.allocate(KEY.planId(), KEY.planRevision());

      assertAll(secondaries.stream().map(secondary -> () -> assertEquals(primary, secondary)));
    }

    @Test
    @Disabled("TODO: Test that each secondary reflects the primary after every repository-level operation")
    public void reflectsRepositoryOperations() {
      fail();
    }

    @Test
    @Disabled("TODO: Test that each secondary reflects the primary after every cell-level operation")
    public void reflectsCellOperations() {
      fail();
    }
  }
}
