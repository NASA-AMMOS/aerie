package gov.nasa.jpl.mpsa.cli.commands;

public interface UndoableCommand {

    void undo();
    void redo();

}
