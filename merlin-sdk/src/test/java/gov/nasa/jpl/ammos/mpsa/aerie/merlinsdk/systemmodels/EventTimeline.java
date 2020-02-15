package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class EventTimeline implements Iterable<EventTimeline.Event> {
    private final Map<Instant, List<Stimulus>> timeline = new TreeMap<>();
    private final Set<String> usedKeys = new HashSet<>();

    private void add(final Instant time, final String key, final Object value) {
        this.timeline
            .computeIfAbsent(time, k -> new ArrayList<>())
            .add(new Stimulus(key, value));
    }

    public <StimulusType> Channel<StimulusType> createChannel(final String key) {
        if (this.usedKeys.contains(key)) {
            throw new RuntimeException("Channel key `" + key + "` is already in use");
        } else {
            this.usedKeys.add(key);
        }

        return (instant, stimulus) -> this.add(instant, key, stimulus);
    }

    private static final class Stimulus {
        private final String key;
        private final Object value;

        private Stimulus(final String key, final Object value) {
            this.key = key;
            this.value = value;
        }
    }

    public static final class Event {
        public final Instant time;
        public final String key;
        public final Object value;

        private Event(final Instant time, final String key, final Object value) {
            this.time = time;
            this.key = key;
            this.value = value;
        }
    }

    @Override
    public Iterator<Event> iterator() {
        return new Iterator<>() {
            private final Iterator<Map.Entry<Instant, List<Stimulus>>> timelineIterator = timeline.entrySet().iterator();
            private Instant currentInstant = null;
            private Iterator<Stimulus> instantIterator = null;

            // Postcondition: `this.instantIterator.next()` gives the next event, if there is a next event.
            private void moveToNext() {
                while ((this.instantIterator == null || !this.instantIterator.hasNext()) && this.timelineIterator.hasNext()) {
                    final var entry = this.timelineIterator.next();
                    this.currentInstant = entry.getKey();
                    this.instantIterator = entry.getValue().iterator();
                }
            }

            @Override
            public boolean hasNext() {
                this.moveToNext();

                return (this.instantIterator != null && this.instantIterator.hasNext());
            }

            @Override
            public Event next() {
                this.moveToNext();

                final var stimulus = this.instantIterator.next();
                return new Event(this.currentInstant, stimulus.key, stimulus.value);
            }
        };
    }
}
