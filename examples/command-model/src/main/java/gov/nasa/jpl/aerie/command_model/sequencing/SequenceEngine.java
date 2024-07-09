package gov.nasa.jpl.aerie.command_model.sequencing;

import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.merlin.framework.annotations.AutoValueMapper;

import java.util.Optional;

import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Naming.name;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteDynamicsMonad.effect;

@AutoValueMapper.Record
public record SequenceEngine(int engineNumber, boolean active, ExecutableSequence sequence, int currentCommandIndex) {
    public static SequenceEngine available(int engineNumber) {
        return new SequenceEngine(engineNumber, false, null, 0);
    }

    public Optional<ExecutableCommand> currentCommand() {
        return Optional.ofNullable(sequence).flatMap($ -> $.getCommand(currentCommandIndex));
    }

    public boolean available() {
        return sequence == null;
    }

    /**
     * Effects for manipulating a sequence engine.
     */
    public final static class Effects {
        private Effects() {}

        public static void load(MutableResource<Discrete<SequenceEngine>> engine, ExecutableSequence sequence) {
            engine.emit(name(effect(engine$ -> new SequenceEngine(
                    engine$.engineNumber(), false, sequence, 0)),
                    "Load sequence %s", sequence.id()));
        }

        public static void unload(MutableResource<Discrete<SequenceEngine>> engine) {
            engine.emit("Unload sequence", effect(engine$ -> new SequenceEngine(
                    engine$.engineNumber(), false, null, 0)));
        }

        public static void advance(MutableResource<Discrete<SequenceEngine>> engine, int nextCommandIndex) {
            engine.emit(name(effect(engine$ -> new SequenceEngine(
                    engine$.engineNumber(), engine$.active(), engine$.sequence(), nextCommandIndex)),
                    "Advance to step %s", nextCommandIndex));
        }

        public static void activate(MutableResource<Discrete<SequenceEngine>> engine) {
            engine.emit("Activate sequence engine", effect(engine$ -> new SequenceEngine(
                    engine$.engineNumber(), true, engine$.sequence(), engine$.currentCommandIndex())));
        }

        public static void deactivate(MutableResource<Discrete<SequenceEngine>> engine) {
            engine.emit("Deactivate sequence engine", effect(engine$ -> new SequenceEngine(
                    engine$.engineNumber(), false, engine$.sequence(), engine$.currentCommandIndex())));
        }
    }
}
