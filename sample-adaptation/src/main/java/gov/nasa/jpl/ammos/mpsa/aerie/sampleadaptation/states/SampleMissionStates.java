package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ViolableConstraint;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.states.ConsumableState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.states.IndependentStateFactory;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.Config;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.events.SampleEvent;

import java.util.List;

import static gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleQuerier.ctx;
import static gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleQuerier.query;

public final class SampleMissionStates {

    // Create IndependentStateFactory to create states from
    // Second parameter tells the factory that events should be emitted as independent events, defined in our SampleEvent
    public static final IndependentStateFactory factory = new IndependentStateFactory(query, (ev) -> ctx.emit(SampleEvent.independent(ev)));

    // TODO: Currently batteryCapacity is used, but never recharged
    public static final ConsumableState batteryCapcity = factory.createConsumableState("batteryCapacity", Config.initialBatteryCapacity);
    // TODO: Make the following data states Integers when possible
    public static final ConsumableState instrumentDataBits = factory.createConsumableState("instrumentData", 0.0);
    public static final ConsumableState cameraDataBits = factory.createConsumableState("cameraData", 0.0);
    public static final ConsumableState totalDownlinkedDataBits = factory.createConsumableState("totalDownlinkedData", 0.0);

    // Placeholder for when violable constraints are added
    public static final List<ViolableConstraint> violableConstraints = List.of();
}
