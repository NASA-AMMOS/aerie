package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.activities.ActivityTypeStateFactory;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.Constraint;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ViolableConstraint;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.independent.DoubleState;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.independent.IndependentStateFactory;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.Config;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.instrument.RunInstrument;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.events.SampleEvent;

import java.util.List;

import static gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleQuerier.activityQuerier;
import static gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleQuerier.ctx;
import static gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleQuerier.query;

public final class SampleMissionStates {
    private static final ActivityTypeStateFactory activities = new ActivityTypeStateFactory(activityQuerier);

    // Create IndependentStateFactory to create states from
    // Second parameter tells the factory that events should be emitted as independent events, defined in our SampleEvent
    public static final IndependentStateFactory factory = new IndependentStateFactory(query, (ev) -> ctx.emit(SampleEvent.independent(ev)));

    // TODO: Currently batteryCapacity is used, but never recharged
    public static final DoubleState batteryCapacity = factory.cumulative("batteryCapacity", Config.initialBatteryCapacity);
    // TODO: Make the following data states Integers when possible
    public static final DoubleState instrumentDataBits = factory.cumulative("instrumentData", 0.0);
    public static final DoubleState cameraDataBits = factory.cumulative("cameraData", 0.0);
    public static final DoubleState totalDownlinkedDataBits = factory.cumulative("totalDownlinkedData", 0.0);

    public static final List<ViolableConstraint> violableConstraints = List.of(
            new ViolableConstraint(batteryCapacity.whenLessThan(Config.startBatteryCapacity_J * 0.3))
                    .withId("minSOC")
                    .withName("Min Battery SoC")
                    .withMessage("Battery Capacity severely low")
                    .withCategory("severe"),
            new ViolableConstraint(activities
                                       .ofType(RunInstrument.class)
                                       .exists(act -> Constraint.and(
                                           act.whenActive(),
                                           batteryCapacity.whenLessThan(Config.startBatteryCapacity_J * 0.3))))
                .withId("minSOCinUse")
                .withName("Min Battery SoC In Use")
                .withMessage("Battery Capacity severely low and being further depleted")
                .withCategory("severe"),
            new ViolableConstraint(batteryCapacity.whenLessThan(Config.startBatteryCapacity_J * 0.5)
                    .and(batteryCapacity.whenGreaterThanOrEqualTo(Config.startBatteryCapacity_J * 0.3)))
                    .withId("lowSOC")
                    .withName("Low Battery SoC")
                    .withMessage("Battery Capacity moderately low")
                    .withCategory("moderate"),
            new ViolableConstraint(instrumentDataBits.whenGreaterThan(3e+6))
                    .withId("maxAllocatedInstrumentData")
                    .withName("Max Instrument Data")
                    .withMessage("Exceeded max instrument data space available")
                    .withCategory("warning"),
            new ViolableConstraint(cameraDataBits.whenGreaterThan(1.5e+7))
                    .withId("maxAllocatedCameraData")
                    .withName("Max Camera Data")
                    .withMessage("Exceeded max camera data space available")
                    .withCategory("warning")
    );
    static {
        final var constraintNames = new java.util.HashSet<String>();
        for (final var violableConstraint : violableConstraints) {
            if (!constraintNames.add(violableConstraint.name)) {
                throw new Error("More than one violable constraint with name \"" + violableConstraint.name + "\". Each name must be unique.");
            }
        }
    }
}
