package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class EventTimeline<StimulusType> implements Iterable<EventTimeline.Event<StimulusType>> {
    private final Map<Instant, List<StimulusType>> timeline = new TreeMap<>();

    public void add(final Instant time, final StimulusType stimulus) {
        this.timeline.computeIfAbsent(time, k -> new ArrayList<>()).add(stimulus);
    }

    public static class Event<StimulusType> {
        public final Instant time;
        public final StimulusType stimulus;

        private Event(final Instant time, final StimulusType stimulus) {
            this.time = time;
            this.stimulus = stimulus;
        }
    }

    @Override
    public Iterator<Event<StimulusType>> iterator() {
        return new Iterator<>() {
            private final Iterator<Map.Entry<Instant, List<StimulusType>>> timelineIterator = timeline.entrySet().iterator();
            private Instant currentInstant = null;
            private Iterator<StimulusType> instantIterator = null;

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
            public Event<StimulusType> next() {
                this.moveToNext();

                return new Event<>(this.currentInstant, this.instantIterator.next());
            }
        };
    }
}
