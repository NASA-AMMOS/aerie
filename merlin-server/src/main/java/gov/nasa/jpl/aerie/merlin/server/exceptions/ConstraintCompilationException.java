package gov.nasa.jpl.aerie.merlin.server.exceptions;

import gov.nasa.jpl.aerie.merlin.server.services.ConstraintsDSLCompilationService;

public class ConstraintCompilationException extends  Exception {
  String constraintName;
  ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Error error;
    public ConstraintCompilationException(String constraintName, ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Error error) {
      super("Constraint "+constraintName+" compilation failed:\n"+error.toString());
      this.constraintName = constraintName;
      this.error = error;
    }

  public String getConstraintName() {
    return constraintName;
  }

  public ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Error getErrors() {
    return error;
  }
}
