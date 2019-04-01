package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands;

public interface UndoableCommand {

    void undo();
    void redo();

}
