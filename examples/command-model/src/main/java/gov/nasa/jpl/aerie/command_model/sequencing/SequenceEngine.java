package gov.nasa.jpl.aerie.command_model.sequencing;

import gov.nasa.jpl.aerie.command_model.events.EventDispatcher;
import gov.nasa.jpl.aerie.command_model.sequencing.Sequencing.CommandEvent;
import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources;

import java.util.Optional;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.whenever;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Context.contextualized;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Context.inContext;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Naming.name;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.set;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad.map;

public final class SequenceEngine {
    // Mutable state
    private final MutableResource<Discrete<Optional<Sequence>>> currentSequence;
    private final MutableResource<Discrete<Integer>> currentCommandIndex;
    private final MutableResource<Discrete<State>> state;

    // Derived values
    private final Resource<Discrete<String>> currentSequenceId;
    private final Resource<Discrete<Boolean>> isAvailable;
    private final Resource<Discrete<Optional<Command>>> currentCommand;
    private final Resource<Discrete<String>> currentCommandStem;
    private final Resource<Discrete<Boolean>> isActive;

    // Expose the states as immutable resources.
    // We'll manage the states with methods on the class, rather than directly exposing mutable resources.
    // If we wanted to be really paranoid about protecting these, we could return currentSequence::getDynamics instead.
    // That would create a new anonymous object, preventing the caller from casting the object back to MutableResource.
    public Resource<Discrete<Optional<Sequence>>> currentSequence() { return currentSequence; }

    public Resource<Discrete<String>> currentSequenceId() { return currentSequenceId(); }
    public Resource<Discrete<Boolean>> isAvailable() { return isAvailable; }
    public Resource<Discrete<Integer>> currentCommandIndex() { return currentCommandIndex; }
    public Resource<Discrete<Optional<Command>>> currentCommand() { return currentCommand; }
    public Resource<Discrete<String>> currentCommandStem() { return currentCommandStem; }
    public Resource<Discrete<State>> state() { return state; }
    public Resource<Discrete<Boolean>> isActive() { return isActive; }

    public SequenceEngine(String name, EventDispatcher<CommandEvent> commandEvents) {
        // For ease of debugging later, name everything we derive here.
        // This is generally overkill, but it's not bad practice to attach meaningful names where possible.
        // This is especially true when using map(...) directly, as this *doesn't* attach a derived name automatically.
        currentSequence = name(discreteResource(Optional.empty()), name + "/currentSequence");
        currentCommandIndex = name(discreteResource(0), name + "/currentCommandIndex");
        state = name(discreteResource(State.INACTIVE), name + "/state");

        currentSequenceId = name(map(currentSequence, seq -> seq.map(Sequence::id).orElse("")), name + "/currentSequenceId");
        isAvailable = name(map(currentSequence, Optional::isEmpty), name + "/isAvailable");
        currentCommand = name(
                map(currentSequence, currentCommandIndex, (seq, i) -> seq.flatMap($ -> $.getCommand(i))),
                name + "/currentCommand");
        currentCommandStem = name(map(currentCommand, cmd -> cmd.map(c -> c.stem).orElse("")), name + "/currentCommandStem");
        isActive = name(DiscreteResources.equals(state, constant(State.ACTIVE)), name + "/isActive");

        inContext(name, () -> whenever(isActive, () -> {
            // Get the next command. If currentCommand is an error, shut this engine down gracefully by returning no command.
            currentValue(currentCommand, Optional.empty()).ifPresentOrElse(
                    command -> {
                        commandEvents.emit(CommandEvent.atStart(command));
                        var result = command.run();
                        commandEvents.emit(CommandEvent.atEnd(command));
                        // Update the equivalent of the program-counter to advance to the next command
                        set(currentCommandIndex, result.nextCommandIndex());
                    },
                    () -> {
                        // TODO - Log something about the sequence ending.
                        reset();
                    }
            );
        }));
    }

    public void loadSequence(Sequence sequence) {
        if (State.ACTIVE.equals(currentValue(state))) {
            // TODO - we're interrupting a sequence... perhaps that should be noted somewhere?
            reset();
        }

        set(currentSequence, Optional.of(sequence));
    }

    public void activate() {
        // TODO - Error check that we aren't activating an already-active engine?
        set(state, State.ACTIVE);
    }

    public void deactivate() {
        // TODO - Error check that we aren't deactivating an already-inactive engine?
        set(state, State.INACTIVE);
    }

    public void reset() {
        set(currentSequence, Optional.empty());
        set(currentCommandIndex, 0);
        deactivate();
    }


    public enum State {
        INACTIVE,
        ACTIVE
    }
}
