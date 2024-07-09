package gov.nasa.jpl.aerie.command_model.sequencing;

public interface CommandBehavior {
    /**
     * Run this command.
     * This method should model the time this command takes,
     * and possibly effects that are directly and solely due to this command.
     */
    CommandResult run();

    record CommandResult(int nextCommandIndex) {}
}
