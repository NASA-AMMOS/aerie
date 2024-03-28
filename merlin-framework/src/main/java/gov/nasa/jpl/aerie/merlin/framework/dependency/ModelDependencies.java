package gov.nasa.jpl.aerie.merlin.framework.dependency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ModelDependencies {
  private ModelDependencies(){}
  private final static List<Dependency> DEPENDENCIES = new ArrayList<>();
  public static void add(final Dependency... dependencies){
    DEPENDENCIES.addAll(Arrays.stream(dependencies).toList());
  }

  public static void add(final List<Dependency> dependencies){
    DEPENDENCIES.addAll(dependencies);
  }
}
