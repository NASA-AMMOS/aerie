package gov.nasa.jpl.aerie.command_model.activities;

import gov.nasa.jpl.aerie.command_model.Mission;
import gov.nasa.jpl.aerie.command_model.sequencing.Sequence;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;

import static gov.nasa.jpl.aerie.command_model.sequencing.SequenceEngine.Effects.activate;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad.map;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.waitUntil;

@ActivityType("AuthoredSequence")
public class AuthoredSequence {
    @Export.Parameter
    public Sequence sequence;

    @ActivityType.EffectModel
    public void run(Mission mission) {
        // TODO - we're treating this sequence as a load-and-go, add support for other sequences as well.
        var engine = mission.sequencing.loadSequence(sequence);
        // Activate the engine to start the sequence
        activate(engine);
        // Wait until that sequence is unloaded (engine available or loaded with a different sequence)
        // to consider this implementation finished.
        // This would include other sequences potentially pausing and resuming this sequence.
        waitUntil(when(map(engine, $ -> $.available() || !sequence.id().equals($.sequence().id()))));
    }
}
