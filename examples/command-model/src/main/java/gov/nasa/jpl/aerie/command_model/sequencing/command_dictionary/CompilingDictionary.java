package gov.nasa.jpl.aerie.command_model.sequencing.command_dictionary;

import gov.nasa.jpl.aerie.command_model.activities.commands.CommandActivity;
import gov.nasa.jpl.aerie.command_model.sequencing.Command;

/**
 * "Compiles" arbitrary command stems to a behavior that will call a {@link CommandActivity}.
 * This implements most of the intrinsic behavior of the command,
 * and puts a legible span on the Aerie UI marking which command was called.
 */
public interface CompilingDictionary {
    CommandActivity interpret(Command command);
}
