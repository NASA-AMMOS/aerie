package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions;

public abstract class ActionFailureException extends Exception {
    private final String actionType;
    private final String reason;

    public ActionFailureException(String actionType, String reason) {
        super(String.format("Action %s failed: %s", actionType, reason));
        this.actionType = actionType;
        this.reason = reason;
    }

    public String getActionType() {
        return actionType;
    }

    public String getReason() {
        return reason;
    }
}
