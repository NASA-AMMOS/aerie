package gov.nasa.jpl.aerie.merlin.framework.dependency;

import java.util.List;

public sealed interface DependencyType {
  record Unconditional() implements DependencyType{}
  record Conditional(List<Dependency.ModelDependency.ResourceRead> resourceReads, List<Dependency.ActivityDependency.ParameterRead> parameterReads) implements DependencyType{}

  static Unconditional unconditional(){
    return new Unconditional();
  }

  static Conditional conditional(List<Dependency.ModelDependency.ResourceRead> resourceReads, List<Dependency.ActivityDependency.ParameterRead> parameterReads){
    return new Conditional(resourceReads, parameterReads);
  }
}
