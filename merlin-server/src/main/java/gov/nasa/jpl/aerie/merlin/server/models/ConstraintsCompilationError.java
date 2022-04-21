package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.json.Iso;
import gov.nasa.jpl.aerie.json.JsonParser;

import java.util.List;

import static gov.nasa.jpl.aerie.json.BasicParsers.*;
import static gov.nasa.jpl.aerie.json.Uncurry.tuple;
import static gov.nasa.jpl.aerie.json.Uncurry.untuple;

public class ConstraintsCompilationError {
  private static final JsonParser<CodeLocation> codeLocationP =
      productP
          .field("line", intP)
          .field("column", intP)
          .map(Iso.of(
              untuple(CodeLocation::new),
              $ -> tuple($.line, $.column)));

  private static final JsonParser<UserCodeError> userCodeErrorP =
      productP
          .field("message", stringP)
          .field("stack", stringP)
          .field("sourceContext", stringP)
          .field("location", codeLocationP)
          .map(Iso.of(
              untuple(UserCodeError::new),
              $ -> tuple($.message, $.stack, $.sourceContext, $.location)));

  public static final JsonParser<List<UserCodeError>> constraintsErrorJsonP = listP(userCodeErrorP);

  public record CodeLocation(Integer line, Integer column) {}

  public record UserCodeError(String message, String stack, String sourceContext, CodeLocation location) {}
}
