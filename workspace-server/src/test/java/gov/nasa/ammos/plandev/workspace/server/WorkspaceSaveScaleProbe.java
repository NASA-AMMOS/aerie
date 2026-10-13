package gov.nasa.ammos.plandev.workspace.server;

import gov.nasa.ammos.plandev.workspace.server.types.HandlerResult;
import io.javalin.http.UploadedFile;
import jakarta.servlet.http.Part;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Measures what saving one tiny file costs as the number of untouched files in the workspace grows. Not a CI test and
 * asserts no timings: run with {@code ./gradlew :workspace-server:test --tests '*WorkspaceSaveScaleProbe'
 * -Dworkspace.scaleProbe=true -i} and read the printed table. {@code -Dworkspace.scaleProbe.sizes=300,1000} overrides
 * the tree sizes.
 */
@EnabledIfSystemProperty(named = "workspace.scaleProbe", matches = "true")
class WorkspaceSaveScaleProbe {
  private static final int SAVES = 40;
  private static final int WARMUP = 10;

  @TempDir Path tmp;

  private static UploadedFile upload(final String name, final String content) {
    final var bytes = content.getBytes(StandardCharsets.UTF_8);
    final var part = (Part) Proxy.newProxyInstance(Part.class.getClassLoader(), new Class<?>[]{Part.class}, (p, m, a) ->
        switch (m.getName()) {
          case "getInputStream" -> new ByteArrayInputStream(bytes);
          case "getSubmittedFileName", "getName" -> name;
          case "getSize" -> (long) bytes.length;
          case "getContentType" -> "application/octet-stream";
          default -> null;
        });
    return new UploadedFile(part);
  }

  /** A realistic small sequence file, about 1 KiB. */
  private static String sequence(final int i) {
    final var sb = new StringBuilder("# sequence ").append(i).append('\n');
    for (int line = 0; line < 24; line++) sb.append("C CMD_").append(line).append(" ").append(i * 31 + line).append(" \"arg\"\n");
    return sb.toString();
  }

  private static long save(final WorkspaceBindings bindings, final int ws, final String content) {
    final var start = System.nanoTime();
    final var result = bindings.handleFileUpload(ws, Path.of("target/edit.seq"), upload("edit.seq", content), true, null, "probe");
    final var elapsed = System.nanoTime() - start;
    assertInstanceOf(HandlerResult.Success.class, result, () -> result.jsonResponse().toString());
    return elapsed;
  }

  private static double ms(final long nanos) {
    return nanos / 1e6;
  }

  @Test
  void saveCostVersusUntouchedFiles() throws Exception {
    final var sizes = Arrays.stream(System.getProperty("workspace.scaleProbe.sizes", "300,1000,3000,10000").split(","))
                            .map(String::trim).mapToInt(Integer::parseInt).toArray();
    final var base = tmp.toRealPath();
    final WorkspaceRoots roots = id -> base.resolve("ws" + id);
    final var out = new StringBuilder("\nuntouched | adopt ms | first save after restart ms | save median ms | save p90 ms\n");

    for (final var n : sizes) {
      final var root = Files.createDirectories(base.resolve("ws" + n));
      for (int i = 0; i < n; i++) {
        final var dir = Files.createDirectories(root.resolve("dir" + (i / 100)));
        Files.writeString(dir.resolve("seq" + i + ".seq"), sequence(i));
      }

      var history = new WorkspaceHistory(roots);
      var bindings = new WorkspaceBindings(null, new WorkspaceFileSystemService(roots, null, history), history, null, "");
      final var adoptStart = System.nanoTime();
      save(bindings, n, "adopt");
      final var adopt = System.nanoTime() - adoptStart;
      for (int i = 0; i < WARMUP; i++) save(bindings, n, "warmup " + i);

      history = new WorkspaceHistory(roots); // a fresh process: no per-process caches
      bindings = new WorkspaceBindings(null, new WorkspaceFileSystemService(roots, null, history), history, null, "");
      final var first = save(bindings, n, "first after restart");

      final var samples = new ArrayList<Long>();
      for (int i = 0; i < SAVES; i++) samples.add(save(bindings, n, "edit " + i));
      Collections.sort(samples);
      out.append("%9d | %8.1f | %27.1f | %14.2f | %11.2f%n".formatted(
          n, ms(adopt), ms(first), ms(samples.get(SAVES / 2)), ms(samples.get(SAVES * 9 / 10))));
    }
    System.out.println(out);
    final var report = System.getProperty("workspace.scaleProbe.out");
    if (report != null) Files.writeString(Path.of(report), out.toString());
  }
}
