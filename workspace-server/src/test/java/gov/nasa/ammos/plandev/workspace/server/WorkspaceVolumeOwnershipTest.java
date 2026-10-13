package gov.nasa.ammos.plandev.workspace.server;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link WorkspaceHistory} relies on workspace-server being the only writer of the workspace volume, with one
 * instance per volume (its locks are in-process). This keeps every deployment definition honest about that: adding
 * another service that mounts the volume, or a second workspace-server replica, fails here and needs a decision.
 */
class WorkspaceVolumeOwnershipTest {
  private static final Path REPO = Path.of("..");
  private static final List<String> COMPOSE_FILES = List.of(
      "docker-compose.yml",
      "deployment/docker-compose.yml",
      "e2e-tests/docker-compose-test.yml",
      "e2e-tests/docker-compose-many-workers.yml");
  private static final Pattern SERVICE = Pattern.compile("^  ([A-Za-z0-9_.-]+):\\s*$");

  @Test
  void onlyWorkspaceServerMountsTheWorkspaceVolumeInCompose() throws IOException {
    for (final var file : COMPOSE_FILES) {
      final var mounters = new ArrayList<String>();
      String section = null;
      String service = null;
      for (final var line : Files.readAllLines(REPO.resolve(file))) {
        if (!line.startsWith(" ") && line.endsWith(":")) section = line;
        final var m = SERVICE.matcher(line);
        if (m.matches()) service = m.group(1);
        if ("services:".equals(section) && line.trim().startsWith("- workspace_file_store:")) mounters.add(service);
      }
      assertEquals(List.of("plandev_workspace"), mounters, file);
    }
  }

  @Test
  void onlyOneWorkspaceServerReplicaMountsTheWorkspaceVolumeInKubernetes() throws IOException {
    final var mounters = new ArrayList<String>();
    try (final var manifests = Files.list(REPO.resolve("deployment/kubernetes"))) {
      for (final var manifest : manifests.toList()) {
        if (Files.readString(manifest).contains("claimName: workspace_file_store")) mounters.add(manifest.getFileName().toString());
      }
    }
    assertEquals(List.of("plandev-workspace-deployment.yaml"), mounters);
    final var deployment = Files.readString(REPO.resolve("deployment/kubernetes/plandev-workspace-deployment.yaml"));
    assertTrue(deployment.contains("replicas: 1"), "workspace-server locks are in-process: one replica per volume");
  }
}
