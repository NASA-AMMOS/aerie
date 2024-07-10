package gov.nasa.jpl.aerie.command_model.sequencing.command_dictionary;

import gov.nasa.jpl.aerie.command_model.sequencing.ExecutableCommand;
import gov.nasa.jpl.aerie.command_model.sequencing.ExecutableSequence;
import gov.nasa.jpl.aerie.command_model.sequencing.Sequence;

public interface CommandDictionary {
    /**
     * Interpret this sequence according to this command dictionary,
     * returning a sequence with a concrete set of behaviors attached to it.
     */
    ExecutableSequence interpret(Sequence sequence);

    /**
     * Compose the behaviors provided with this dictionary with those provided by supplement.
     * Behaviors provided by supplement will run first.
     */
    default CommandDictionary compose(IntrinsicCommandDictionary supplement) {
        return sequence -> {
            var execSequence = interpret(sequence);
            return new ExecutableSequence(execSequence.id(), execSequence.commands().stream()
                    .map(cmd -> new ExecutableCommand(cmd.base(), cmd.behavior().compose(supplement.interpret(cmd.base()))))
                    .toList());
        };
    }

    /**
     * Compose the behaviors provided with this dictionary with those provided by supplement.
     * Behaviors provided by supplement will run in parallel.
     */
    default CommandDictionary composeParallel(IntrinsicCommandDictionary supplement) {
        return sequence -> {
            var execSequence = interpret(sequence);
            return new ExecutableSequence(execSequence.id(), execSequence.commands().stream()
                    .map(cmd -> new ExecutableCommand(cmd.base(), cmd.behavior().composeParallel(supplement.interpret(cmd.base()))))
                    .toList());
        };
    }
}
