package gov.nasa.jpl.aerie.command_model.sequencing.command_dictionary;

import gov.nasa.jpl.aerie.command_model.sequencing.CommandBehavior;
import gov.nasa.jpl.aerie.command_model.sequencing.Sequence;

import java.util.List;

/**
 * Specifies only the control flow, and no other part of the command behavior.
 */
public interface ControlFlowDictionary {
    List<CommandBehavior> interpret(Sequence sequence);
}
