package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.UndoableCommand;

import java.util.UUID;

// this command removes an activity
public class RemoveActivityCommand implements Command, UndoableCommand {


    public RemoveActivityCommand() {

        // TODO: Get an instance of the plan
    }


    @Override
    public void execute() {
        // Get the right instance by ID and remove it from the plan
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