package gov.nasa.jpl.aerie.command_model.sequencing;

// TODO - I don't like the name "base" here.
//  The name should reflect that we're separating the written representation of the command
//  from our interpretation of how it behaves.
public record ExecutableCommand(Command base, CommandBehavior behavior) {
}
