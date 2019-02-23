package gov.nasa.jpl.mpsa.cli.commands.impl;

import gov.nasa.jpl.mpsa.cli.commands.Command;
import gov.nasa.jpl.mpsa.cli.commands.UndoableCommand;

import java.util.UUID;

// this command removes an activity
public class RemoveActivityCommand implements Command, UndoableCommand {

    public RemoveActivityCommand() {
        // TODO: get an instance of the plan with the ID
    }

    @Override
    public void execute() {
        // Get the plan and remove the specific instance via an ID
    }

    private void removeActivityFromPlan(String planId, UUID activityId) {
        // TODO: call the plan service and remove the activity
    }

    @Override
    public void undo() {

    }

    @Override
    public void redo() {

    }

}