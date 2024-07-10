package gov.nasa.jpl.aerie.command_model.sequencing;

import gov.nasa.jpl.aerie.command_model.Mission;
import gov.nasa.jpl.aerie.command_model.activities.CommandSpan;
import gov.nasa.jpl.aerie.command_model.events.EventDispatcher;
import gov.nasa.jpl.aerie.command_model.generated.ActivityActions;
import gov.nasa.jpl.aerie.command_model.sequencing.command_dictionary.CommandDictionary;
import gov.nasa.jpl.aerie.contrib.streamline.core.*;
import gov.nasa.jpl.aerie.contrib.streamline.debugging.Logging;
import gov.nasa.jpl.aerie.contrib.streamline.debugging.Naming;
import gov.nasa.jpl.aerie.contrib.streamline.debugging.SimpleLogger;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static gov.nasa.jpl.aerie.command_model.sequencing.SequenceEngine.Effects.*;
import static gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.$int;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.whenever;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Context.contextualized;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Logging.LOGGER;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Naming.getName;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Naming.name;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.increment;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.discreteResource;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteDynamicsMonad.map;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad.map;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.*;

public class Sequencing {
    // This limit is just to prevent the system from completely running away if things go wrong.
    private static final int MAX_ENGINES = 1_000;

    // TODO - There's something a little off in this model...
    //   Each SequenceEngine in the structure below should be wrapped in its own dynamics, or maybe even be its own resource?
    //   That way, we could represent an engine erroring out or expiring (whatever that means) independently of the other resources.
    //   In fact, this is a more general pattern - Basically a way of "dynamically" creating MutableResource<T>'s
    //   by factoring them into a single larger MutableResource<Map<K, ErrorCatching<Expiring<T>>>>>.
    //   We lose efficiency because an effect on any individual dynamic resource becomes an effect on all dynamics resources,
    //   (or at least all dynamic resources with the same root "real" resource), but that's the price of dynamic allocation, I guess...
    //   For efficiency, we could pre-allocate a handful of static "real" copies, and operate independently on them.
    //   Then only the "overflow" would need to be dynamic.
    private final MutableResource<Discrete<Map<Integer, SequenceEngine>>> sequenceEngines;
    private final MutableResource<Discrete<Integer>> spawnedSequenceEngineDaemons;

    private final CommandDictionary commandDictionary;
    private final Mission mission;
    private final EventDispatcher<CommandEvent> commandEvents;
    public record CommandEvent(
            TimingDescriptor timing,
            Command command,
            MutableResource<Discrete<SequenceEngine>> engine
    ) {
        @Override
        public String toString() {
            return "%s.%s (%s)".formatted(command.stem(), timing, getName(engine, null));
        }
    }

    public Sequencing(CommandDictionary commandDictionary, Mission mission, Registrar registrar) {
        this.commandDictionary = commandDictionary;
        this.mission = mission;
        this.commandEvents = new EventDispatcher<>(new SimpleLogger("Commands", registrar.baseRegistrar));

        sequenceEngines = discreteResource(Map.of());
        spawnedSequenceEngineDaemons = discreteResource(0);

        // Spawn daemons as needed to cover all the engines that could be running
        var missingDaemons = map(sequenceEngines, spawnedSequenceEngineDaemons,
                (engines, spawnedDaemons) -> spawnedDaemons < engines.size());
        whenever(missingDaemons, () -> {
            spawnSequenceEngineDaemon(currentValue(spawnedSequenceEngineDaemons));
            increment(spawnedSequenceEngineDaemons);
        });

        registrar.discrete("loadedSequenceEngines", countEngines(engine -> !engine.available()), $int());
        registrar.discrete("activeSequenceEngines", countEngines(SequenceEngine::active), $int());
    }

    private void spawnSequenceEngineDaemon(int index) {
        var engine = engine(index);
        whenever(map(engine, SequenceEngine::active), () -> currentValue(engine).currentCommand().ifPresentOrElse(
                cmd -> {
                    // Run this command
                    commandEvents.emit(new CommandEvent(TimingDescriptor.START, cmd.base(), engine));
                    // TODO - I don't love advancing the engine inside the command itself.
                    //   I'd rather get the result, emit the "end of command" effect, and then advance the engine.
                    //   However, when I use my usual MutableObject trick to pull the result out, I keep getting nulls.
                    ActivityActions.call(mission, new CommandSpan(() -> advance(engine, cmd.behavior().run().nextCommandIndex())));
                    commandEvents.emit(new CommandEvent(TimingDescriptor.END, cmd.base(), engine));
                },
                () -> {
                    // Sequence complete, unload the engine.
                    // This also deactivates the engine, so we'll pause until something else loads and activates engine.
                    unload(engine);
                })
        );
    }

    private Resource<Discrete<Integer>> countEngines(Predicate<SequenceEngine> predicate) {
        return map(sequenceEngines, m -> (int) m.values().stream().filter(predicate).count());
    }

    public MutableResource<Discrete<SequenceEngine>> engine(int index) {
        // Define a value to return when the engine is unloaded
        SequenceEngine unloadedSequenceEngine = SequenceEngine.available(index);
        // Define an immutable view of the resource to read from.
        // Do this outside the context of the resource itself, because the map operation has some (small) overhead.
        Resource<Discrete<SequenceEngine>> immutableView =
                map(sequenceEngines, m -> m.getOrDefault(index, unloadedSequenceEngine));
        return name(new MutableResource<>() {
            @Override
            public void emit(DynamicsEffect<Discrete<SequenceEngine>> effect) {
                // Factor an effect on a single engine through the map of all engines by the given index.
                // In particular, if the effect would unload the engine, remove that engine instead.
                sequenceEngines.emit(name(allEnginesDynamics -> {
                    var engineDynamics = map(allEnginesDynamics, m -> m.getOrDefault(index, unloadedSequenceEngine));
                    var newEngineDynamics = effect.apply(engineDynamics);
                    return map(allEnginesDynamics, newEngineDynamics, (m, e) -> {
                        var m$ = new HashMap<>(m);
                        m$.put(index, e);
                        return m$;
                    });
                }, "%s on engine %s", effect, index));
            }

            @Override
            public ErrorCatching<Expiring<Discrete<SequenceEngine>>> getDynamics() {
                return immutableView.getDynamics();
            }
        }, "Engine %s", index);
    }

    public MutableResource<Discrete<SequenceEngine>> loadSequence(Sequence sequence) {
        // First, interpret the sequence according to the loaded command dictionary
        var executableSequence = commandDictionary.interpret(sequence);
        // Then, search for the first available sequence engine to load this into.
        for (int i = 0; i < MAX_ENGINES; ++i) {
            var engine = engine(i);
            // If engine is unloaded and not in error
            if (currentValue(map(engine, SequenceEngine::available), false)) {
                // Load the sequence in an inactive state
                load(engine, executableSequence);
                // Finally, return this engine so the client can manipulate it.
                return engine;
            }
        }
        // Things have gone very wrong, and we've exhausted all possible engines.
        throw new RuntimeException(("Sequencing has used all %d possible sequence engines." +
                " Please look for an infinite loop loading sequences.").formatted(MAX_ENGINES));
    }

    public void listenForCommand(String commandStem, Consumer<CommandEvent> action) {
        listenForCommand(TimingDescriptor.START, commandStem, action);
    }

    public void listenForCommand(TimingDescriptor timing, String commandStem, Consumer<CommandEvent> action) {
        commandEvents.registerEventListener(event -> {
            if (timing.equals(event.timing()) && commandStem.equals(event.command().stem())) {
                action.accept(event);
            }
        });
    }

    public enum TimingDescriptor {
        START,
        END
    }
}
