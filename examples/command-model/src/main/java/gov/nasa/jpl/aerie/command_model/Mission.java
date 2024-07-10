package gov.nasa.jpl.aerie.command_model;

import gov.nasa.jpl.aerie.command_model.power.Power;
import gov.nasa.jpl.aerie.command_model.sequencing.CommandBehavior.CommandResult;
import gov.nasa.jpl.aerie.command_model.sequencing.ExecutableCommand;
import gov.nasa.jpl.aerie.command_model.sequencing.ExecutableSequence;
import gov.nasa.jpl.aerie.command_model.sequencing.Sequencing;
import gov.nasa.jpl.aerie.command_model.sequencing.command_dictionary.CommandDictionary;
import gov.nasa.jpl.aerie.command_model.sequencing.command_dictionary.IntrinsicCommandDictionary;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar.ErrorBehavior;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.time.Instant;

import static gov.nasa.jpl.aerie.command_model.sequencing.command_dictionary.IntrinsicCommandDictionary.compose;
import static gov.nasa.jpl.aerie.command_model.utils.StreamUtils.enumerate;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;

public final class Mission {
    public final Sequencing sequencing;
    public final Power power;

    public Mission(gov.nasa.jpl.aerie.merlin.framework.Registrar registrar$, Instant planStart, Configuration configuration) {
        var registrar = new Registrar(registrar$, planStart, ErrorBehavior.Throw);
        this.sequencing = new Sequencing(commandDictionary(), registrar);
        this.power = new Power(sequencing, registrar);
    }

    private CommandDictionary commandDictionary() {
        // I'm writing this as just a static function on Mission itself, but in a real model, this would likely be a
        // more involved incon process that read in a command dictionary and some supplemental information.
        // This is just a way to demonstrate some approaches to how you might decompose the problem of defining commands,
        // and crucially, how that decomposition can be done independently of the sequencing logic itself.

        // For now, use a dummy control flow that just moves to the next command
        CommandDictionary controlFlow = sequence -> new ExecutableSequence(sequence.id(), enumerate(sequence.commands().stream())
                .map(indexAndCmd -> new ExecutableCommand(indexAndCmd.getRight(), () ->
                        new CommandResult(indexAndCmd.getLeft() + 1)))
                .toList());
        // We can factor commands into groups or concerns that would make sense to model together.
        IntrinsicCommandDictionary sequenceControl = cmd -> {
            if (cmd.stem().equals("WAIT")) {
                return () -> delay(Duration.roundNearest(Double.parseDouble(cmd.arguments().get(0)), SECONDS));
            } else {
                return () -> {};
            }
        };
        IntrinsicCommandDictionary debugCommands = cmd -> {
            if (cmd.stem().equals("ECHO")) {
                // TODO - this should use Aerie event logging once I figure that out better.
                return () -> System.out.println(cmd.arguments().get(0));
            } else {
                return () -> {};
            }
        };
        // We can also factor out the way the engine itself ticks over, to impose a minimum duration for commands.
        IntrinsicCommandDictionary sequenceEngineCycle = cmd -> () -> delay(Duration.SECOND);
        // The sequence engine ticks in parallel with the command behavior itself.
        // Since we know that we've structured the intrinsic command behaviors to be non-overlapping,
        // we can safely run them in sequence (which is more efficient).
        // Diagrammatically, the behaviors happen like this
        // (although at least one of debugCommands and sequenceControl will be a no-op).
        // ---+--> debugCommands ---> sequenceControl ---+--> controlFlow
        //    |                                          |
        //    +--> sequenceEngineCycle ------------------+
        // I'm using the static compose to suggest we could add as many factors as we want here.
        return controlFlow.compose(sequenceEngineCycle.composeParallel(compose(
                debugCommands,
                sequenceControl)));
    }
}
