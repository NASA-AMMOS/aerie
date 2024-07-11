package gov.nasa.jpl.aerie.command_model.sequencing.command_dictionary;

import gov.nasa.jpl.aerie.command_model.Mission;
import gov.nasa.jpl.aerie.command_model.sequencing.CommandBehavior;
import gov.nasa.jpl.aerie.command_model.sequencing.ExecutableCommand;
import gov.nasa.jpl.aerie.command_model.sequencing.ExecutableSequence;
import gov.nasa.jpl.aerie.command_model.sequencing.Sequence;

import java.util.ArrayList;
import java.util.List;

public interface CommandDictionary {
    /**
     * Interpret this sequence according to this command dictionary,
     * returning a sequence with a concrete set of behaviors attached to it.
     */
    ExecutableSequence interpret(Sequence sequence);

    static CommandDictionary combine(
            Mission mission,
            ControlFlowDictionary controlFlowDictionary,
            CompilingDictionary compilingDictionary) {
        return sequence -> {
            List<CommandBehavior> controlFlowBehaviors = controlFlowDictionary.interpret(sequence);
            List<ExecutableCommand> executableCommands = new ArrayList<>(sequence.commands().size());
            for (int i = 0; i < sequence.commands().size(); ++i) {
                var cmd = sequence.commands().get(i);
                var activity = compilingDictionary.interpret(cmd);
                var controlFlow = controlFlowBehaviors.get(i);
                executableCommands.add(new ExecutableCommand(
                        cmd,
                        activity,
                        controlFlow.compose(() -> activity.call(mission))));
            }
            return new ExecutableSequence(sequence.id(), executableCommands);
        };
    }
}
