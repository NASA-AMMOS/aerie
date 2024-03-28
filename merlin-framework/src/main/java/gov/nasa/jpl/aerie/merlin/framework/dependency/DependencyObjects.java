package gov.nasa.jpl.aerie.merlin.framework.dependency;

public sealed interface DependencyObjects {
  record RuntimeObject(Object object) implements DependencyObjects {}
  record ActivityType(String name) implements DependencyObjects {}
  record AnonymousTask(Runnable runnable) implements DependencyObjects {}


  static AnonymousTask anonymousTask(final Runnable object){
    return new AnonymousTask(object);
  }

  static ActivityType activityType(final String name){
    return new ActivityType(name);
  }

  static RuntimeObject object(final Object object){
    return new RuntimeObject(object);
  }
}
