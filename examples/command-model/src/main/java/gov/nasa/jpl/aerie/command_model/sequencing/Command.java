package gov.nasa.jpl.aerie.command_model.sequencing;

import java.util.List;
import java.util.function.Supplier;

public final class Command {
    public final String stem;
    public final List<Object> arguments;

    private final Supplier<CommandResult> runBehavior;
    public CommandResult run() {
        return runBehavior.get();
    }

    public Command(String stem, List<Object> arguments, Supplier<CommandResult> runBehavior) {
        this.stem = stem;
        this.arguments = arguments;
        this.runBehavior = runBehavior;
    }

    // Making this result a record lets us do two things:
    // 1. We can give the result value a meaningful name to document the code.
    // 2. We can add more information to this result later without refactoring the code in many places.
    record CommandResult(int nextCommandIndex) {}

    // TODO - Easily support a minimum duration
}
