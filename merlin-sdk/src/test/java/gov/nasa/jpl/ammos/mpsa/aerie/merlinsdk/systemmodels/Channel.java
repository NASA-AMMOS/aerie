package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Channel<StimulusType> implements Iterable<Channel.Entry<StimulusType>> {
    private static int nextChannelId = 0;
    public final int id;

    private final EventTimeline<Key> timeline;
    private final List<StimulusType> stimuli = new ArrayList<>();

    public Channel(final EventTimeline<Key> timeline) {
        this.id = nextChannelId++;
        this.timeline = timeline;
    }

    public void scheduleEffect(final Instant instant, final StimulusType stimulus) {
        this.stimuli.add(stimulus);
        final var stimulusId = this.stimuli.size() - 1;

        this.timeline.add(instant, new Key(this.id, stimulusId));
    }

    public StimulusType getStimulusByKey(final Key key) {
        if (this.id != key.channelId) {
            throw new RuntimeException("Attempted to retrieve a stimulus from a channel that doesn't own it");
        }
        return this.stimuli.get(key.stimulusId);
    }

    public static class Key {
        public final int channelId;
        public final int stimulusId;

        private Key(final int channelId, final int stimulusId) {
            this.channelId = channelId;
            this.stimulusId = stimulusId;
        }
    }

    @Override
    public Iterator<Entry<StimulusType>> iterator() {
        final var iterator = this.stimuli.iterator();

        return new Iterator<>() {
            private int nextId = 0;

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Entry<StimulusType> next() {
                return new Entry<>(new Key(Channel.this.id, this.nextId++), iterator.next());
            }
        };
    }

    public static final class Entry<StimulusType> {
        public final Key key;
        public final StimulusType stimulus;

        private Entry(final Key key, final StimulusType stimulus) {
            this.key = key;
            this.stimulus = stimulus;
        }
    }
}
