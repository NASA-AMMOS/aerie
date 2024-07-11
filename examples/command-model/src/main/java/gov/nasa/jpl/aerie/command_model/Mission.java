package gov.nasa.jpl.aerie.command_model;

import gov.nasa.jpl.aerie.command_model.power.Power;
import gov.nasa.jpl.aerie.command_model.sequencing.CommandBehavior;
import gov.nasa.jpl.aerie.command_model.sequencing.CommandBehavior.CommandResult;
import gov.nasa.jpl.aerie.command_model.sequencing.ExecutableCommand;
import gov.nasa.jpl.aerie.command_model.sequencing.ExecutableSequence;
import gov.nasa.jpl.aerie.command_model.sequencing.Sequencing;
import gov.nasa.jpl.aerie.command_model.sequencing.command_dictionary.CommandDictionary;
import gov.nasa.jpl.aerie.command_model.sequencing.command_dictionary.CompilingDictionaryImpl;
import gov.nasa.jpl.aerie.command_model.sequencing.command_dictionary.ControlFlowDictionary;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar.ErrorBehavior;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.time.Instant;

import static gov.nasa.jpl.aerie.command_model.utils.StreamUtils.enumerate;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

public final class Mission {
    public final Sequencing sequencing;
    public final Power power;

    public final Instant planStart;

    public Mission(gov.nasa.jpl.aerie.merlin.framework.Registrar registrar$, Instant planStart, Configuration configuration) {
        this.planStart = planStart;
        var registrar = new Registrar(registrar$, planStart, ErrorBehavior.Throw);
        this.sequencing = new Sequencing(commandDictionary(), this, registrar);
        this.power = new Power(sequencing, registrar);
    }

    private CommandDictionary commandDictionary() {
        // For now, use a dummy control flow that just moves to the next command
        // In theory, this is where we would model things like "if" and "while"
        ControlFlowDictionary controlFlow = sequence -> enumerate(sequence.commands().stream())
                .<CommandBehavior>map(indexAndCmd -> () -> new CommandResult(indexAndCmd.getLeft() + 1))
                .toList();

        // We'll model most intrinsic behavior through this compiling dictionary, that hands off to the command activities.
        var compilingDictionary = new CompilingDictionaryImpl();

        // Finally, we'll combine all of these by saying putting the engine itself and the command activities in parallel.
        // Since the control flow doesn't take any simulation time, it's most efficient to put it serially, and last.
        return CommandDictionary.combine(this, controlFlow, compilingDictionary);
    }
}
