package gov.nasa.jpl.aerie.command_model.sequencing.command_dictionary;

import gov.nasa.jpl.aerie.command_model.sequencing.Command;

import java.util.Arrays;

import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Context.contextualized;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.*;

/**
 * Contains "intrinsic" command behaviors.
 * Intrinsic command behaviors are those that don't depend on the context of the command.
 * Crucially, this means they do not specify the next command to execute.
 */
public interface IntrinsicCommandDictionary {
    Runnable interpret(Command command);

    /**
     * Compose the intrinsic behaviors provided by this and other.
     * Behaviors provided by other will run first.
     */
    default IntrinsicCommandDictionary compose(IntrinsicCommandDictionary other) {
        return compose(other, this);
    }

    /**
     * Compose the intrinsic behaviors provided by each dictionary.
     * Behaviors run in the order listed.
     */
    static IntrinsicCommandDictionary compose(IntrinsicCommandDictionary... dictionaries) {
        return cmd -> {
            var behaviors = Arrays.stream(dictionaries).map(d -> d.interpret(cmd)).toList();
            return () -> behaviors.forEach(Runnable::run);
        };
    }

    /**
     * Compose the intrinsic behaviors provided by this and other.
     * Behaviors provided by other will run in parallel.
     */
    default IntrinsicCommandDictionary composeParallel(IntrinsicCommandDictionary other) {
        return composeParallel(other, this);
    }

    /**
     * Compose the intrinsic behaviors provided by each dictionary.
     * Behaviors run in parallel.
     */
    static IntrinsicCommandDictionary composeParallel(IntrinsicCommandDictionary... dictionaries) {
        return cmd -> {
            var behaviors = Arrays.stream(dictionaries).map(d -> d.interpret(cmd)).toList();
            // Spawn all tasks to start them exactly in parallel.
            // Then wrap the whole thing in a call so it returns when all branches return.
            return () -> call(replaying(contextualized(() ->
                    behaviors.forEach(b -> spawn(replaying(contextualized(b)))))));
        };
    }
}
