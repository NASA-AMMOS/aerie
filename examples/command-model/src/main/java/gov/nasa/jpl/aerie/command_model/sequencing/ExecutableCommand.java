package gov.nasa.jpl.aerie.command_model.sequencing;

import gov.nasa.jpl.aerie.command_model.activities.commands.CommandActivity;

// TODO - I don't like the name "base" here.
//  The name should reflect that we're separating the written representation of the command
//  from its implementation
public record ExecutableCommand(Command base, CommandActivity activity, CommandBehavior behavior) {
}
