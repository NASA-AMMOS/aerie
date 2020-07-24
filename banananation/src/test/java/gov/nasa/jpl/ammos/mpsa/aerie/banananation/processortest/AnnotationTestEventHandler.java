package gov.nasa.jpl.ammos.mpsa.aerie.banananation.processortest;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.MerlinEvent;

@MerlinEvent
public interface AnnotationTestEventHandler<Result> {
  Result log(String message);
}
