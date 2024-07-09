package gov.nasa.jpl.aerie.command_model.sequencing.command_dictionary;

import gov.nasa.jpl.aerie.command_model.sequencing.ExecutableSequence;
import gov.nasa.jpl.aerie.command_model.sequencing.Sequence;

public interface CommandDictionary {
    /**
     * Interpret this sequence according to this command dictionary,
     * returning a sequence with a concrete set of behaviors attached to it.
     */
    ExecutableSequence interpret(Sequence sequence);
}
