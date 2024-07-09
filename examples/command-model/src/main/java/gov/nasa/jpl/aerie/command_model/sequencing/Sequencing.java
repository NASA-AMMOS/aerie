package gov.nasa.jpl.aerie.command_model.sequencing;

import gov.nasa.jpl.aerie.command_model.events.EventDispatcher;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.EnumValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.IntegerValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.StringValueMapper;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.sumInt;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad.map;

public class Sequencing {
    public final List<SequenceEngine> sequenceEngines;
    // TODO - consider making EventDispatcher an interface, and building a version that indexes on command stem for efficiency
    public final EventDispatcher<CommandEvent> commandEvents;

    public final Resource<Discrete<Integer>> availableSequenceEngines;

    public Sequencing(int numberOfSequenceEngines, Registrar registrar) {
        commandEvents = new EventDispatcher<>();
        sequenceEngines = IntStream.range(0, numberOfSequenceEngines)
                .mapToObj(i -> new SequenceEngine("SequenceEngine_" + i, commandEvents))
                .toList();
        availableSequenceEngines = sumInt(sequenceEngines.stream().map(engine ->
                map(engine.isAvailable(), a -> a ? 1 : 0)));

        registrar.discrete("availableSequenceEngines", availableSequenceEngines, new IntegerValueMapper());
        sequenceEngines.forEach(engine -> {
            registrar.discrete(engine.state(), new EnumValueMapper<>(SequenceEngine.State.class));
            registrar.discrete(engine.currentSequenceId(), new StringValueMapper());
            registrar.discrete(engine.currentCommandIndex(), new IntegerValueMapper());
            registrar.discrete(engine.currentCommandStem(), new StringValueMapper());
        });
    }

    public void loadSequence(Sequence sequence) {
        // Find the next available engine, and load the sequence there.
        // If the engine has errored out for any reason, treat it as unavailable.
        for (var sequenceEngine : sequenceEngines) {
            if (currentValue(sequenceEngine.isAvailable(), false)) {
                sequenceEngine.loadSequence(sequence);
                return;
            }
        }
        // If no sequence engine is available, just return
        // TODO: Log some kind of error when we get here, noting that we can't run this sequence?
    }

    public void addListener(String commandStem, Consumer<Command> action) {
        addListener(CommandEventTime.START, commandStem, action);
    }

    public void addListener(CommandEventTime time, String commandStem, Consumer<Command> action) {
        commandEvents.registerEventListener(commandEvent -> {
            if (Objects.equals(time, commandEvent.time())
                    && Objects.equals(commandStem, commandEvent.command().stem)) {
                action.accept(commandEvent.command());
            }
            // Else, this didn't match, so ignore it.
        });
    }

    record CommandEvent(Command command, CommandEventTime time) {
        public static CommandEvent atStart(Command command) {
            return new CommandEvent(command, CommandEventTime.START);
        }

        public static CommandEvent atEnd(Command command) {
            return new CommandEvent(command, CommandEventTime.END);
        }

        @Override
        public String toString() {
            return "%s of %s".formatted(time, command);
        }
    }
    enum CommandEventTime {
        START,
        END
    }
}
