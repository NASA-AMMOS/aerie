package gov.nasa.jpl.aerie.banananation.models;

import java.util.Arrays;
import java.util.List;

public record Sequence<Model>(String name, List<Command<Model>> commands) {
  @SafeVarargs
  public static <Model> Sequence<Model> of(String name, Command<Model>... commmands) {
    return new Sequence<>(name, Arrays.asList(commmands));
  }
}
