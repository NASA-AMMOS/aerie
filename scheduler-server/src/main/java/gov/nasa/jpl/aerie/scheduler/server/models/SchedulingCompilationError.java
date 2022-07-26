package gov.nasa.jpl.aerie.scheduler.server.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;

public class SchedulingCompilationError {
  public record CodeLocation(Integer line, Integer column) {}
  public record UserCodeError(String message, String stack, String sourceContext, CodeLocation location) {}

  public static class UserCodeErrorsList {
    private final List<UserCodeError> errors;
    @JsonCreator
    public UserCodeErrorsList(List<UserCodeError> errors) {
      this.errors = errors;
    }

    @JsonValue
    public List<UserCodeError> errors() {
      return this.errors;
    }
  }
}
