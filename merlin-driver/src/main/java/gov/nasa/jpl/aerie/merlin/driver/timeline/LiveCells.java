package gov.nasa.jpl.aerie.merlin.driver.timeline;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public final class LiveCells {
  // INVARIANT: Every Query<T> maps to a LiveCell<T>; that is, the type parameters are correlated.
  private final Map<Query<?>, LiveCell<?>> cells = new HashMap<>();
  private final Map<Topic<?>, HashSet<LiveCell<?>>> cellsForTopic = new HashMap<>();

  private final EventSource source;
  private final LiveCells parent;

  public LiveCells(final EventSource source) {
    this.source = source;
    this.parent = null;
  }

  public LiveCells(final EventSource source, final LiveCells parent) {
    this.source = source;
    this.parent = parent;
  }

  public int size() {
    return cells.size();
  }

  public <State> Optional<State> getState(final Query<State> query) {
    return getCell(query).map(Cell::getState);
  }

  public Optional<Duration> getExpiry(final Query<?> query) {
    return getCell(query).flatMap(Cell::getExpiry);
  }

  public <State> LiveCell<State> put(final Query<State> query, final Cell<State> cell) {
    // SAFETY: The query and cell share the same State type parameter.
    final var liveCell = new LiveCell<>(cell, this.source.cursor());
    this.cells.put(query, liveCell);
    cell.getTopics().forEach(t -> this.cellsForTopic.computeIfAbsent(t, $ -> new HashSet<>()).add(liveCell));
    return liveCell;
  }

  public Collection<LiveCell<?>> getCells() {
    return cells.values();
  }

  public Set<LiveCell<?>> getCells(final Topic<?> topic) {
    var c4t = cellsForTopic.get(topic);
    if (c4t != null && !c4t.isEmpty()) return c4t; // assumes one cell per topic; TODO: give up on multiple cells per topic and change signature to getCell(topic)->LiveCell ?
    Set<LiveCell<?>> cells = new HashSet<>();
    if (parent == null) return cells;
    var parentCells = parent.getCells(topic);
    // Need to get the duplicated cell in cells corresponding to each matching parent cell
    for (var c : parentCells) {
      Stream<Query<?>> queries = parent.cells.keySet().stream().filter(q -> parent.cells.get(q).equals(c));
      var newCells = queries.map(q -> {
        // need to call getCell() just to generate the duplicate of the parent cell
        getCell(q);
        // getCell() above returns Cell instead of LiveCell, so we throw that result away and get it directly.
        return this.cells.get(q);
      });
      cells.addAll(newCells.toList());
    }
    return cells;
  }

  private <State> Optional<Cell<State>> getCell(final Query<State> query) {
    Optional<LiveCell<State>> liveCell = getLiveCell(query);
    return liveCell.isPresent() ? Optional.of(liveCell.get().get()) : Optional.empty();
  }

  public <State> Optional<LiveCell<State>> getLiveCell(final Query<State> query) {
    // First, check if we have this cell already.
    {
      // SAFETY: By the invariant, if there is an entry for this query, it is of type Cell<State>.
      @SuppressWarnings("unchecked")
      final var cell = (LiveCell<State>) this.cells.get(query);

      if (cell != null) return Optional.of(cell);
    }

    // Otherwise, go ask our parent for the cell.
    if (this.parent == null) return Optional.empty();
    final var cell$ = this.parent.getCell(query);
    if (cell$.isEmpty()) return Optional.empty();

    final var cell = put(query, cell$.get().duplicate());

    return Optional.of(cell);
  }
}
