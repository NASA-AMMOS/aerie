package gov.nasa.jpl.aerie.merlin.driver.retracing.tracing;

public class TaskTracePrinter {
  private static String indent(final String s) {
    return joinLines(s.lines().map(line -> "  " + line).toList());
  }

  private static String joinLines(final Iterable<String> result) {
    return String.join("\n", result);
  }

  static String render(TaskTrace trace) {
    return "";
  }

  static String render(TaskTrace.End<?> trace) {
    switch (trace) {
      case TaskTrace.End.Read<?> t -> {
        final var result = new StringBuilder();
        result.append("read(");
        result.append(t.query().toString());
        result.append("){\n");
        for (final var readRecord : t.entries()) {
          result.append(indent(readRecord.value().toString() + "->[") + "\n");
          result.append(indent(indent(render(readRecord.rest()))));
          result.append("\n" + indent("]") + "\n");
        }
        result.append("}");
        return result.toString();
      }
      case TaskTrace.End.Exit<?> t -> {
        return "exit(" + t.returnValue() + ");\n";
      }
      case TaskTrace.End.Unfinished t -> {
        return "unfinished...";
      }
    }
  }
}
