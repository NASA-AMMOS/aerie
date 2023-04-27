package gov.nasa.jpl.aerie.merlin.server.models;

import static gov.nasa.jpl.aerie.json.BasicParsers.*;
import static gov.nasa.jpl.aerie.json.Uncurry.tuple;
import static gov.nasa.jpl.aerie.json.Uncurry.untuple;

import gov.nasa.jpl.aerie.json.JsonParser;
import java.util.List;

public class ConstraintsCompilationError {
  private static final JsonParser<CodeLocation> codeLocationP =
      productP
          .field("line", intP)
          .field("column", intP)
          .map(untuple(CodeLocation::new), $ -> tuple($.line, $.column));

  private static final JsonParser<UserCodeError> userCodeErrorP =
      productP
          .field("message", stringP)
          .field("stack", stringP)
          .field("location", codeLocationP)
          .field("completeStack", stringP)
          .map(
              untuple(UserCodeError::new),
              $ -> tuple($.message, $.stack, $.location, $.completeStack));

  public static final JsonParser<List<UserCodeError>> constraintsErrorJsonP = listP(userCodeErrorP);

  public record CodeLocation(Integer line, Integer column) {}

  public record UserCodeError(
      String message, String stack, CodeLocation location, String completeStack) {}
}
