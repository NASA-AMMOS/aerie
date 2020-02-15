package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

public interface Channel<StimulusType> {
    void add(final Instant instant, final StimulusType stimulus);
}
