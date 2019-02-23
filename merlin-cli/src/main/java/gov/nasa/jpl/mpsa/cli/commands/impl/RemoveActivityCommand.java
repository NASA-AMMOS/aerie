package gov.nasa.jpl.mpsa.cli.commands.impl;

import gov.nasa.jpl.mpsa.activities.ActivityType;
import gov.nasa.jpl.mpsa.cli.commands.Command;
import gov.nasa.jpl.mpsa.cli.commands.UndoableCommand;
import gov.nasa.jpl.mpsa.plan.Plan;

import java.util.UUID;

// this command removes an activity
public class RemoveActivityCommand implements Command, UndoableCommand {

    private ActivityType activityType;
    private Plan plan;

    public RemoveActivityCommand() {
        this.plan = Plan.getInstance();
    }

    public RemoveActivityCommand(ActivityType activityType) {
        this.activityType = activityType;
    }


    @Override
    public void execute() {
        // Get the plan
        removeActivityFromPlan(plan.getId(), activityType.getId());
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