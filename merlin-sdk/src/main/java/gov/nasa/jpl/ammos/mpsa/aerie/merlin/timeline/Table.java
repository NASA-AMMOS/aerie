package gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.effects.Applicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.effects.Projection;

import java.util.HashMap;
import java.util.Map;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.SimulationTimeline.START_INDEX;

/* package-local */
final class Table<$Timeline, Event, Effect, Model> {
  private final SimulationTimeline<$Timeline, Event> database;
  private final Projection<Event, Effect> projection;
  private final Applicator<Effect, Model> applicator;

  private final Map<Integer, Model> cache = new HashMap<>();

  public Table(
      final SimulationTimeline<$Timeline, Event> database,
      final Projection<Event, Effect> projection,
      final Applicator<Effect, Model> applicator)
  {
    this.database = database;
    this.projection = projection;
    this.applicator = applicator;
  }

  public void clearCache() {
    this.cache.clear();
  }

  public Model getAt(final History<$Timeline, Event> history) {
    // If we already have a model cached for this time point, we can just bail now.
    if (history.getIndex() == START_INDEX) {
      return this.applicator.initial();
    } else if (this.cache.containsKey(history.getIndex())) {
      return this.cache.get(history.getIndex());
    }

    // Look for a cached model anytime back to our most recent branch point, if any.
    final var baseIndex = (history.getLastBranchBase() != null) ? history.getLastBranchBase().getIndex() : START_INDEX;
    var previousIndex = history.getIndex();
    while (previousIndex != baseIndex && !this.cache.containsKey(previousIndex)) {
      previousIndex = database.get(previousIndex).getPrevious();
    }

    final Model model;
    if (previousIndex == baseIndex) {
      // We didn't find anything. We'll have to query our previous segment for its model.
      if (history.getLastBranchBase() != null) {
        model = this.applicator.duplicate(this.getAt(history.getLastBranchBase()));
      } else {
        // Well, we don't have a previous segment. Start with the initial model.
        model = this.applicator.initial();
      }
    } else {
      // We found something! Since we have exclusive ownership over our branch segment, we'll remove it from the cache,
      // update it directly, and put it back in the cache at our current time point.
      model = this.cache.remove(previousIndex);
    }

    // Compute the effects that have occurred since our last update on this branch.
    final var effects = this.database.evaluate(this.projection, previousIndex, history.getIndex());

    // Step this model up to the current point in time.
    for (final var effect : effects) {
      this.applicator.step(model, effect.getKey());
      this.applicator.apply(model, effect.getValue());
    }

    // Cache this model for future queries.
    this.cache.put(history.getIndex(), model);

    return model;
  }
}
