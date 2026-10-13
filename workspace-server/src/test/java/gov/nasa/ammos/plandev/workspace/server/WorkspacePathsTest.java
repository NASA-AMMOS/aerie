package gov.nasa.ammos.plandev.workspace.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspacePathsTest {
  @TempDir Path workspace;

  @Test
  void createsNormalizedGitPathKey() {
    final var root = Path.of("workspace").toAbsolutePath().normalize();
    final var file = root.resolve("one").resolve("discarded").resolve("..").resolve("two.txt");

    assertEquals("one/two.txt", WorkspacePaths.key(root, file));
  }

  @Test
  void createsEmptyKeyForWorkspaceRoot() {
    final var root = Path.of("workspace").toAbsolutePath().normalize();

    assertEquals("", WorkspacePaths.key(root.resolve("."), root));
  }

  @Test
  void markerTracksOnlyOtherwiseEmptyDirectories() throws Exception {
    final var parent = workspace.resolve("one");
    final var leaf = parent.resolve("two");
    WorkspacePaths.createDirectories(workspace, leaf);

    assertFalse(Files.exists(parent.resolve(WorkspacePaths.DIRECTORY_MARKER)));
    assertEquals(0, Files.size(leaf.resolve(WorkspacePaths.DIRECTORY_MARKER)));
    assertFalse(WorkspacePaths.walk(workspace, Integer.MAX_VALUE).stream()
        .anyMatch(path -> WorkspacePaths.isDirectoryMarkerName(path.getFileName().toString())));

    final var content = leaf.resolve("file.txt");
    Files.writeString(content, "content");
    WorkspacePaths.updateDirectoryMarker(workspace, leaf);
    assertFalse(Files.exists(leaf.resolve(WorkspacePaths.DIRECTORY_MARKER)));

    Files.delete(content);
    WorkspacePaths.updateDirectoryMarker(workspace, leaf);
    assertTrue(Files.isRegularFile(leaf.resolve(WorkspacePaths.DIRECTORY_MARKER)));
  }
}
