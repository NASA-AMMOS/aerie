package gov.nasa.jpl.aerie.command_model.sequencing.command_dictionary;

import gov.nasa.jpl.aerie.command_model.sequencing.CommandBehavior.CommandResult;
import gov.nasa.jpl.aerie.command_model.sequencing.ExecutableCommand;
import gov.nasa.jpl.aerie.command_model.sequencing.ExecutableSequence;
import gov.nasa.jpl.aerie.command_model.sequencing.Sequence;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import static gov.nasa.jpl.aerie.command_model.utils.StreamUtils.enumerate;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

/**
 * Dummy implementation of a command dictionary.
 * Real implementations should definitely validate that the command stems are real, and that the arguments are valid.
 * Real implementations may also want to add some modeling information to commands.
 * Most crucially, they may want to add more than a first approximation of the duration to each command.
 */
public class DummyCommandDictionary implements CommandDictionary {
    public final Duration defaultCommandDuration;

    public DummyCommandDictionary(Duration defaultCommandDuration) {
        this.defaultCommandDuration = defaultCommandDuration;
    }

    @Override
    public ExecutableSequence interpret(Sequence sequence) {
        return new ExecutableSequence(sequence.id(), enumerate(sequence.commands().stream())
                .map(indexAndCmd -> new ExecutableCommand(indexAndCmd.getRight(), () -> {
                    delay(defaultCommandDuration);
                    return new CommandResult(indexAndCmd.getLeft() + 1);
                }))
                .toList());
    }
}
