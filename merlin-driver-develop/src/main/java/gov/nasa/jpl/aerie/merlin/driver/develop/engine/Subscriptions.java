package gov.nasa.jpl.aerie.merlin.driver.develop.engine;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class Subscriptions<TopicRef, QueryRef> {
  /** The set of topics depended upon by a given query. */
  private final Map<QueryRef, Set<TopicRef>> topicsByQuery = new HashMap<>();

  /** An index of queries by subscribed topic. */
  @DerivedFrom("topicsByQuery")
  private final Map<TopicRef, Set<QueryRef>> queriesByTopic = new HashMap<>();

  // This method takes ownership of `topics`; the set should not be referenced after calling this method.
  public void subscribeQuery(final QueryRef query, final Set<TopicRef> topics) {
    this.topicsByQuery.put(query, topics);

    for (final var topic : topics) {
      this.queriesByTopic.computeIfAbsent(topic, $ -> new HashSet<>()).add(query);
    }
  }

  public void unsubscribeQuery(final QueryRef query) {
    final var topics = this.topicsByQuery.remove(query);

    for (final var topic : topics) {
      final var queries = this.queriesByTopic.get(topic);
      if (queries == null) continue;

      queries.remove(query);
      if (queries.isEmpty()) this.queriesByTopic.remove(topic);
    }
  }

  public Set<QueryRef> invalidateTopic(final TopicRef topic) {
    final var queries = Optional
        .ofNullable(this.queriesByTopic.remove(topic))
        .orElseGet(Collections::emptySet);

    for (final var query : queries) unsubscribeQuery(query);

    return queries;
  }

  public void clear() {
    this.topicsByQuery.clear();
    this.queriesByTopic.clear();
  }
}
