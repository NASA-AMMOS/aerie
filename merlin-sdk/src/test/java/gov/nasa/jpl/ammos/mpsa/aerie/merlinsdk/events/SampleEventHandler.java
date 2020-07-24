package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.events;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.MerlinEvent;

@MerlinEvent
public interface SampleEventHandler<Result> {
  Result log(String message);
}
