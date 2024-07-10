package gov.nasa.jpl.aerie.command_model.sequencing;

import org.apache.commons.lang3.mutable.MutableObject;

import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Context.contextualized;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.*;

public interface CommandBehavior {
    /**
     * Run this command.
     * This method should model the time this command takes,
     * and possibly effects that are directly and solely due to this command.
     */
    CommandResult run();

    record CommandResult(int nextCommandIndex) {}

    /**
     * Return a behavior that runs prefix, then runs this behavior.
     */
    default CommandBehavior compose(Runnable prefix) {
        return () -> {
            prefix.run();
            return this.run();
        };
    }

    /**
     * Return a behavior that runs backgroundTask in parallel with this behavior.
     */
    default CommandBehavior composeParallel(Runnable backgroundTask) {
        return () -> {
            // Spawn each branch, so that they stay synchronized and parallel in their branch structure.
            // Then wrap both spawns in a call, so that the resulting object returns exactly when both branches return.
            MutableObject<CommandResult> result = new MutableObject<>();
            call(replaying(contextualized(() -> {
                spawn(replaying(contextualized(backgroundTask)));
                spawn(replaying(contextualized(() -> result.setValue(this.run()))));
            })));
            return result.getValue();
        };
    }
}
