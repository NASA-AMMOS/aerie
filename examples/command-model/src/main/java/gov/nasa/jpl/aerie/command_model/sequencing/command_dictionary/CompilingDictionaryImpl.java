package gov.nasa.jpl.aerie.command_model.sequencing.command_dictionary;

import gov.nasa.jpl.aerie.command_model.activities.commands.*;
import gov.nasa.jpl.aerie.command_model.sequencing.Command;

public class CompilingDictionaryImpl implements CompilingDictionary {

    @Override
    public CommandActivity interpret(Command command) {
        // A real definition of this might want to use reflection + a read-in command dictionary file
        // to automagically match up the command arguments and named CommandActivity parameters.
        // We're going to fake that part for now.
        switch (command.stem()) {
            case "CMD_NO_OP" -> {
                return new CMD_NO_OP();
            }
            case "CMD_WAIT" -> {
                var activity = new CMD_WAIT();
                activity.seconds = Double.valueOf(command.arguments().get(0));
                return activity;
            }
            case "CMD_ECHO" -> {
                var activity = new CMD_ECHO();
                activity.message = command.arguments().get(0);
                return activity;
            }
            case "CMD_POWER_ON" -> {
                var activity = new CMD_POWER_ON();
                activity.device = command.arguments().get(0);
                return activity;
            }
            case "CMD_POWER_OFF" -> {
                var activity = new CMD_POWER_OFF();
                activity.device = command.arguments().get(0);
                return activity;
            }
            default -> throw new RuntimeException("Stem %s is not a recognized command.".formatted(command.stem()));
        }
    }
}
