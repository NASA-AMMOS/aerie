package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.model.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

record TaskKey(Class<?> klass, List<Object> fields) implements Task.Key {
  static TaskKey of(final Supplier<?> task) {
    final var klass = task.getClass();
    final var fields = new ArrayList<>();
    for (final var field : task.getClass().getDeclaredFields()) {
      field.setAccessible(true);
      try {
        fields.add(field.get(task));
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
    return new TaskKey(klass, fields);
  }

  @Override
  public boolean match(final Task.Key other) {
    if (!(other instanceof TaskKey tk)) return false;
    return Objects.equals(this.klass(), tk.klass()) && Objects.equals(this.fields(), tk.fields());
  }
}
