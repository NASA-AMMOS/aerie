package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.AbstractMerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ViolableConstraint;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.SimulationTimeline;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.events.SampleEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleMissionStates;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleQuerier;

import java.util.List;

@Adaptation(name = "sample-adaptation", version = "0.1")
public class SampleAdaptation extends AbstractMerlinAdaptation<SampleEvent> {
    @Override
    public List<ViolableConstraint> getViolableConstraints() {
        return SampleMissionStates.violableConstraints;
    }

    @Override
    public <T> Querier<T, SampleEvent> makeQuerier(final SimulationTimeline<T, SampleEvent> database) {
        return new SampleQuerier<>(this.getActivityMapper(), database);
    }
}
