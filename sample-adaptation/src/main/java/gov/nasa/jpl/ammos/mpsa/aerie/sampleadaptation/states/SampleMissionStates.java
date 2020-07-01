package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ViolableConstraint;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.states.IndependentStateFactory;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.events.SampleEvent;

import java.util.List;

import static gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleQuerier.ctx;
import static gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleQuerier.query;

public final class SampleMissionStates {

    // Create IndependentStateFactory to create states from
    // Second parameter tells the factory that events should be emitted as independent events, defined in our SampleEvent
    public static final IndependentStateFactory factory = new IndependentStateFactory(query, (ev) -> ctx.emit(SampleEvent.independent(ev)));

    // Placeholder for when violable constraints are added
    public static final List<ViolableConstraint> violableConstraints = List.of();
}
