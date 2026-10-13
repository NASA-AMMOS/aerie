package gov.nasa.ammos.plandev.workspace.server;

import gov.nasa.ammos.plandev.workspace.server.WorkspaceHistory.Change;
import gov.nasa.ammos.plandev.workspace.server.exceptions.ReservedPathException;
import gov.nasa.ammos.plandev.workspace.server.exceptions.WorkspaceFileOpException;
import gov.nasa.ammos.plandev.workspace.server.types.HandlerResult;
import gov.nasa.ammos.plandev.workspace.server.types.MetadataMergeBehavior;
import io.javalin.http.UploadedFile;
import jakarta.servlet.http.Part;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Exercises workspace mutations end to end below HTTP: the binding handlers (validation, readOnly, ETag), the
 * {@link WorkspaceHistory} mutation boundary, {@link WorkspaceFileSystemService}, and a real JGit repository on disk.
 */
class WorkspaceHistoryIntegrationTest {
  private static final String USER = "alice";
  private static final int WS1 = 1;
  private static final int WS2 = 2;

  @TempDir Path tmp;
  private Path base;
  private WorkspaceRoots roots;
  private FlakyHistory history;
  private WorkspaceFileSystemService fs;
  private WorkspaceBindings bindings;

  /** Stands in for the process dying: an Error, which WorkspaceHistory never catches, so no rollback runs. */
  static final class SimulatedCrash extends Error {}

  /**
   * A WorkspaceHistory whose commit step can be made to fail for chosen workspaces, and which can "crash" at a chosen
   * checkpoint in a chosen workspace (deterministic injection).
   */
  static final class FlakyHistory extends WorkspaceHistory {
    final Set<Path> failCommitsIn = ConcurrentHashMap.newKeySet();
    volatile Checkpoint crashAt;
    volatile Path crashIn;

    FlakyHistory(final WorkspaceRoots roots) {
      super(roots);
    }

    @Override
    protected void checkpoint(final Checkpoint point, final Path root) {
      if (point == crashAt && root.equals(crashIn)) throw new SimulatedCrash();
    }

    @Override
    protected void commit(final Git git, final String message, final PersonIdent author) throws GitAPIException {
      try {
        if (failCommitsIn.contains(git.getRepository().getWorkTree().toPath().toRealPath())) {
          throw new GitAPIException("injected commit failure") {};
        }
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
      super.commit(git, message, author);
    }
  }

  @BeforeEach
  void setUp() throws IOException {
    base = tmp.toRealPath();
    Files.createDirectories(base.resolve("ws1"));
    Files.createDirectories(base.resolve("ws2"));
    roots = id -> base.resolve("ws" + id);
    restart();
  }

  /** A new server process on the same volume: no locks or caches carried over. */
  private void restart() {
    history = new FlakyHistory(roots);
    fs = new WorkspaceFileSystemService(roots, null, history);
    bindings = new WorkspaceBindings(null, fs, history, null, "");
  }

  //region Helpers
  private Path root(final int ws) {
    return base.resolve("ws" + ws);
  }

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

  private HandlerResult save(final int ws, final String path, final String content, final String ifMatch) {
    final var p = Path.of(path);
    return bindings.handleFileUpload(ws, p, upload(p.getFileName().toString(), content), ifMatch == null, ifMatch, USER);
  }

  private String saveOk(final int ws, final String path, final String content) {
    final var result = save(ws, path, content, null);
    assertInstanceOf(HandlerResult.Success.class, result, () -> result.jsonResponse().toString());
    return ((HandlerResult.Success) result).etag().orElseThrow();
  }

  private static void assertSuccess(final HandlerResult result) {
    assertInstanceOf(HandlerResult.Success.class, result, () -> result.jsonResponse().toString());
  }

  private static void assertFailure(final HandlerResult result, final int status, final String type) {
    assertInstanceOf(HandlerResult.Failure.class, result, () -> result.jsonResponse().toString());
    assertEquals(status, result.status(), () -> result.jsonResponse().toString());
    if (type != null) assertEquals(type, result.jsonResponse().asJsonObject().getString("type"));
  }

  private Git git(final int ws) throws IOException {
    return Git.open(root(ws).toFile());
  }

  private void assertClean(final int ws) throws Exception {
    try (final var git = git(ws)) {
      assertEquals(Set.of(), WorkspaceHistory.dirtyPaths(git.status().call()), "workspace " + ws + " is dirty");
    }
  }

  private List<RevCommit> log(final int ws) throws Exception {
    try (final var git = git(ws)) {
      final var commits = new ArrayList<RevCommit>();
      git.log().call().forEach(commits::add);
      return commits;
    }
  }

  private RevCommit head(final int ws) throws Exception {
    return log(ws).getFirst();
  }

  private Set<String> headTree(final int ws) throws Exception {
    try (final var git = git(ws); final var tw = new TreeWalk(git.getRepository())) {
      tw.addTree(head(ws).getTree());
      tw.setRecursive(true);
      final var paths = new TreeSet<String>();
      while (tw.next()) paths.add(tw.getPathString());
      return paths;
    }
  }

  private List<DiffEntry> headDiff(final int ws) throws Exception {
    try (final var git = git(ws); final var df = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
      df.setRepository(git.getRepository());
      df.setDetectRenames(true);
      final var headCommit = head(ws);
      return df.scan(headCommit.getParent(0).getTree(), headCommit.getTree());
    }
  }

  /** Every path the HEAD commit added, changed or removed. */
  private Set<String> headChanges(final int ws) throws Exception {
    final var paths = new TreeSet<String>();
    for (final var d : headDiff(ws)) {
      if (d.getChangeType() != DiffEntry.ChangeType.ADD) paths.add(d.getOldPath());
      if (d.getChangeType() != DiffEntry.ChangeType.DELETE) paths.add(d.getNewPath());
    }
    return paths;
  }

  private String read(final int ws, final String path) throws IOException {
    return Files.readString(root(ws).resolve(path));
  }

  private static String sidecar(final String path) {
    final var p = Path.of(path);
    final var name = "." + p.getFileName() + ".meta.seqdev";
    return p.getParent() == null ? name : p.getParent().resolve(name).toString();
  }

  private void setReadOnly(final int ws, final String path, final boolean readOnly) throws Exception {
    history.mutate(ws, USER, Change.of("lock", Path.of(sidecar(path))), () -> fs.updateMetadataKeys(
        ws, Path.of(path), new MetadataUpdates.Builder(USER).readOnly(readOnly).build(), MetadataMergeBehavior.deepMerge),
        r -> true);
  }

  private boolean isReadOnly(final int ws, final String path) throws Exception {
    return history.mutate(ws, USER, Change.of("check"), () -> fs.isReadOnly(ws, Path.of(path)), r -> true);
  }

  private static final boolean POSIX = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

  private static void deleteRecursively(final Path path) throws IOException {
    if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return;
    try (final var paths = Files.walk(path)) {
      for (final var p : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
    }
  }

  private void deleteSidecar(final int ws, final String path) throws Exception {
    history.mutate(ws, USER, Change.of("drop metadata", Path.of(sidecar(path))), () -> fs.deleteMetadataFile(ws, Path.of(path)), r -> r);
  }

  private void setUserMetadata(final int ws, final String path, final String status) throws Exception {
    history.mutate(ws, USER, Change.of("meta", Path.of(sidecar(path))), () -> fs.updateMetadataKeys(
        ws, Path.of(path),
        new MetadataUpdates.Builder(USER).user(Json.createObjectBuilder().add("status", status).build()).build(),
        MetadataMergeBehavior.deepMerge), r -> true);
  }

  private JsonObject metadata(final int ws, final String path) throws Exception {
    try (final var in = fs.loadMetadataFile(ws, Path.of(path)).readingStream();
         final var reader = Json.createReader(in)) {
      return reader.readObject();
    }
  }

  /** Initialize a workspace's history (an empty directory creation is a no-op mutation). */
  private void init(final int ws) {
    assertSuccess(bindings.handleCreateDirectory(ws, Path.of("scratch"), USER));
  }
  //endregion

  @Nested
  class Save {
    @Test
    void saveCreatesACommitAndLeavesTheRepoClean() throws Exception {
      final var etag = saveOk(WS1, "seq/a.txt", "command A;");

      final var head = head(WS1);
      assertEquals("Update seq/a.txt", head.getFullMessage());
      assertEquals(USER, head.getAuthorIdent().getName());
      assertEquals(Set.of("seq/a.txt", "seq/.a.txt.meta.seqdev"), headTree(WS1));
      assertEquals(WorkspaceService.computeETag("command A;".getBytes(StandardCharsets.UTF_8)), etag);
      assertClean(WS1);
    }

    @Test
    void reSavingOnlyCommitsTheContentNotTheSidecar() throws Exception {
      saveOk(WS1, "a.txt", "one");
      saveOk(WS1, "a.txt", "two");
      assertEquals(List.of("a.txt"), headDiff(WS1).stream().map(DiffEntry::getNewPath).toList());
      assertClean(WS1);
    }

    @Test
    void identicalSaveCreatesNoCommit() throws Exception {
      saveOk(WS1, "a.txt", "same");
      final var before = log(WS1).size();
      saveOk(WS1, "a.txt", "same");
      assertEquals(before, log(WS1).size());
      assertClean(WS1);
    }

    @Test
    void etagChangesAndAStaleEtagIsRejectedBeforeAnyMutation() throws Exception {
      final var etag1 = saveOk(WS1, "a.txt", "v1");
      final var result2 = save(WS1, "a.txt", "v2", etag1);
      assertSuccess(result2);
      final var etag2 = ((HandlerResult.Success) result2).etag().orElseThrow();
      assertNotEquals(etag1, etag2);
      final var commits = log(WS1).size();

      final var stale = save(WS1, "a.txt", "v3", etag1);
      assertFailure(stale, 412, null);
      assertEquals(etag2, stale.jsonResponse().asJsonObject().getJsonObject("data").getString("currentETag"));
      assertEquals(USER, stale.jsonResponse().asJsonObject().getJsonObject("data").getString("lastEditedBy"));
      assertEquals("v2", read(WS1, "a.txt"));
      assertEquals(commits, log(WS1).size());
      assertClean(WS1);

      assertSuccess(save(WS1, "a.txt", "v4", "*")); // explicit overwrite still allowed
      assertEquals("v4", read(WS1, "a.txt"));
    }

    @Test
    void readOnlyFileIsLockedAndTheLockIsNotVersioned() throws Exception {
      saveOk(WS1, "a.txt", "v1");
      final var commits = log(WS1).size();
      setReadOnly(WS1, "a.txt", true);
      assertEquals(commits, log(WS1).size(), "readOnly is runtime state and must not create a commit");
      assertFalse(Files.readString(root(WS1).resolve(".a.txt.meta.seqdev")).contains("readOnly"));
      assertFailure(save(WS1, "a.txt", "v2", "*"), 423, "FILE_LOCKED");
      assertEquals("v1", read(WS1, "a.txt"));
      assertClean(WS1);
    }

    @Test
    void userMetadataIsVersionedAndLastEditedIsDerivedFromHistory() throws Exception {
      saveOk(WS1, "a.txt", "v1");
      final var savedAt = head(WS1).getAuthorIdent().getWhenAsInstant();
      history.mutate(WS1, "bob", Change.of("Update metadata for a.txt", Path.of(".a.txt.meta.seqdev")), () -> fs.updateMetadataKeys(
          WS1, Path.of("a.txt"),
          new MetadataUpdates.Builder("bob").user(Json.createObjectBuilder().add("status", "draft").build()).build(),
          MetadataMergeBehavior.deepMerge), r -> true);

      assertEquals(List.of(".a.txt.meta.seqdev"), headDiff(WS1).stream().map(DiffEntry::getNewPath).toList());
      final var info = fs.getLastEditInfo(WS1, Path.of("a.txt"));
      assertEquals(USER, info.lastEditedBy(), "a metadata-only change is not a content edit");
      assertEquals(savedAt.toString(), info.lastEditedAt());
      assertClean(WS1);
    }
  }

  @Nested
  class FailureSemantics {
    /**
     * Deterministic injection at the mutation boundary: every kind of file change is rolled back. Empty directories
     * are not versioned state, so the layout of empty directories is not part of what rollback guarantees.
     */
    @Test
    void injectedCommitFailureRestoresThePreOperationState() throws Exception {
      saveOk(WS1, "keep.txt", "original");
      saveOk(WS1, "gone.txt", "will be deleted");
      final var headBefore = head(WS1);
      final var stateBefore = Files.readAllBytes(WorkspaceState.file(root(WS1)));

      history.failCommitsIn.add(root(WS1));
      final var ex = assertThrows(WorkspaceHistory.WorkspaceHistoryException.class, () ->
          history.mutate(WS1, USER, Change.of("doomed", Path.of("keep.txt"), Path.of("gone.txt"), Path.of("new"), Path.of("untracked.txt")), () -> {
            Files.writeString(root(WS1).resolve("keep.txt"), "modified");
            Files.delete(root(WS1).resolve("gone.txt"));
            Files.createDirectories(root(WS1).resolve("new/nested"));
            Files.writeString(root(WS1).resolve("new/nested/file.txt"), "new");
            Files.writeString(root(WS1).resolve("untracked.txt"), "new");
            final var state = WorkspaceState.load(root(WS1));
            state.setReadOnly("keep.txt", true);
            state.save();
            return true;
          }, r -> true));
      assertEquals(WorkspaceHistory.Kind.HISTORY_NOT_RECORDED, ex.kind);

      assertEquals("original", read(WS1, "keep.txt"));
      assertEquals("will be deleted", read(WS1, "gone.txt"));
      assertFalse(Files.exists(root(WS1).resolve("new")), "folders left empty by removing created files are pruned");
      assertFalse(Files.exists(root(WS1).resolve("untracked.txt")));
      assertArrayEquals(stateBefore, Files.readAllBytes(WorkspaceState.file(root(WS1))));
      assertEquals(headBefore, head(WS1));
      assertClean(WS1);
    }

    @Test
    void aMutationThatThrowsIsRolledBackAndTheExceptionPropagates() throws Exception {
      saveOk(WS1, "a.txt", "original");
      final var thrown = assertThrows(IOException.class, () -> history.mutate(WS1, USER, Change.of("x", Path.of("a.txt")), () -> {
        Files.writeString(root(WS1).resolve("a.txt"), "half-written");
        throw new IOException("disk full");
      }, r -> true));
      assertEquals("disk full", thrown.getMessage());
      assertEquals("original", read(WS1, "a.txt"));
      assertClean(WS1);
    }

    @Test
    void aFailureResultIsRolledBack() throws Exception {
      saveOk(WS1, "a.txt", "original");
      final var result = history.mutate(WS1, USER, Change.of("x", Path.of("a.txt")), () -> {
        Files.writeString(root(WS1).resolve("a.txt"), "partial");
        return false;
      }, r -> r);
      assertFalse(result);
      assertEquals("original", read(WS1, "a.txt"));
      assertClean(WS1);
    }

    /** A real JGit failure (the branch ref is locked) takes the same path through the save handler. */
    @Test
    void realGitFailureDoesNotReportASuccessfulSave() throws Exception {
      saveOk(WS1, "a.txt", "original");
      final var headBefore = head(WS1);
      final var refLock = root(WS1).resolve(".git/refs/heads/main.lock");
      Files.writeString(refLock, "");

      assertFailure(save(WS1, "a.txt", "changed", "*"), 500, "WORKSPACE_HISTORY_ERROR");
      assertFailure(save(WS1, "b.txt", "new file", null), 500, "WORKSPACE_HISTORY_ERROR");
      assertEquals("original", read(WS1, "a.txt"));
      assertFalse(Files.exists(root(WS1).resolve("b.txt")));
      assertFalse(Files.exists(root(WS1).resolve(".b.txt.meta.seqdev")));
      assertEquals(headBefore, head(WS1));
      assertClean(WS1);
      try (final var internal = Files.list(root(WS1).resolve(".seqdev"))) {
        assertEquals(List.of("state.json"), internal.map(p -> p.getFileName().toString()).toList(), "no upload temp files left");
      }

      Files.delete(refLock);
      saveOk(WS1, "a.txt", "changed");
      assertEquals("changed", read(WS1, "a.txt"));
      assertClean(WS1);
    }
  }

  @Nested
  class FileOperations {
    @Test
    void renameIsRecordedAsARename() throws Exception {
      saveOk(WS1, "a.txt", "content");
      assertSuccess(bindings.handleMove(Path.of("a.txt"), Path.of("b.txt"), WS1, WS1, false, USER));

      final var renames = headDiff(WS1).stream().filter(d -> d.getChangeType() == DiffEntry.ChangeType.RENAME).toList();
      assertTrue(renames.stream().anyMatch(d -> d.getOldPath().equals("a.txt") && d.getNewPath().equals("b.txt")), renames::toString);
      assertEquals(Set.of("b.txt", ".b.txt.meta.seqdev"), headTree(WS1));
      assertClean(WS1);
    }

    @Test
    void renameCarriesTheLock() throws Exception {
      saveOk(WS1, "dir/a.txt", "content");
      setReadOnly(WS1, "dir/a.txt", true);
      assertSuccess(bindings.handleMove(Path.of("dir"), Path.of("renamed"), WS1, WS1, false, USER));
      assertTrue(isReadOnly(WS1, "renamed/a.txt"));
      assertClean(WS1);
    }

    @Test
    void copyLeavesTheRepoClean() throws Exception {
      saveOk(WS1, "a.txt", "content");
      assertSuccess(bindings.handleCopy(Path.of("a.txt"), Path.of("copies/c.txt"), WS1, WS1, false, USER));
      assertEquals(Set.of("a.txt", ".a.txt.meta.seqdev", "copies/c.txt", "copies/.c.txt.meta.seqdev"), headTree(WS1));
      assertEquals("Copy a.txt -> copies/c.txt", head(WS1).getFullMessage());
      assertClean(WS1);
    }

    @Test
    void deleteLeavesTheRepoClean() throws Exception {
      saveOk(WS1, "a.txt", "content");
      saveOk(WS1, "b.txt", "content");
      assertSuccess(bindings.handleDelete(WS1, Path.of("a.txt"), USER));
      assertEquals(Set.of("b.txt", ".b.txt.meta.seqdev"), headTree(WS1));
      assertClean(WS1);
    }
  }

  @Nested
  class DirectoryOperations {
    @BeforeEach
    void populate() {
      saveOk(WS1, "d/x.txt", "x");
      saveOk(WS1, "d/sub/y.txt", "y");
    }

    @Test
    void directoryMoveIsOneCommitWithEveryFile() throws Exception {
      final var commits = log(WS1).size();
      assertSuccess(bindings.handleMove(Path.of("d"), Path.of("e"), WS1, WS1, false, USER));
      assertEquals(commits + 1, log(WS1).size());
      assertEquals(Set.of("e/x.txt", "e/.x.txt.meta.seqdev", "e/sub/y.txt", "e/sub/.y.txt.meta.seqdev"), headTree(WS1));
      final var diff = headDiff(WS1);
      assertEquals(4, diff.size());
      assertTrue(diff.stream().allMatch(d -> d.getChangeType() == DiffEntry.ChangeType.RENAME), diff::toString);
      assertClean(WS1);
    }

    @Test
    void directoryCopyLeavesTheRepoClean() throws Exception {
      assertSuccess(bindings.handleCopy(Path.of("d"), Path.of("e"), WS1, WS1, false, USER));
      assertTrue(headTree(WS1).containsAll(Set.of("d/x.txt", "d/sub/y.txt", "e/x.txt", "e/sub/y.txt", "e/sub/.y.txt.meta.seqdev")));
      assertEquals(4, headDiff(WS1).size());
      assertClean(WS1);
    }

    @Test
    void directoryDeleteLeavesTheRepoClean() throws Exception {
      assertSuccess(bindings.handleDelete(WS1, Path.of("d"), USER));
      assertEquals(Set.of(), headTree(WS1));
      assertEquals(4, headDiff(WS1).size());
      assertClean(WS1);
    }

    @Test
    void theWorkspaceRootCannotBeDeletedOrCopied() throws Exception {
      // The handlers cannot address the root by name; the service still refuses it outright.
      for (final var rootPath : List.of(Path.of(""), Path.of("."), Path.of("d/.."))) {
        assertThrows(WorkspaceFileOpException.class, () -> history.mutate(WS1, USER, Change.of("x", rootPath),
            () -> fs.deleteDirectory(WS1, rootPath), r -> r));
        final var commits = new LinkedHashMap<Integer, Change>();
        commits.put(WS2, Change.of("x", Path.of("x")));
        commits.put(WS1, Change.of("x"));
        assertThrows(WorkspaceFileOpException.class, () -> history.mutate(commits, USER,
            () -> fs.copyDirectory(WS1, rootPath, WS2, Path.of("x"), USER), r -> r));
      }
      assertTrue(Files.isDirectory(root(WS1).resolve(".git")));
      assertFalse(Files.exists(root(WS2).resolve("x")));
      assertClean(WS1);
    }
  }

  @Nested
  class CrossWorkspace {
    @BeforeEach
    void initBoth() {
      init(WS1);
      init(WS2);
    }

    @Test
    void copyUpdatesOnlyTheDestinationHistory() throws Exception {
      saveOk(WS1, "a.txt", "content");
      final var sourceHead = head(WS1);
      assertSuccess(bindings.handleCopy(Path.of("a.txt"), Path.of("in/a.txt"), WS1, WS2, false, USER));
      assertEquals(Set.of("in/a.txt", "in/.a.txt.meta.seqdev"), headTree(WS2));
      assertEquals(sourceHead, head(WS1));
      assertClean(WS1);
      assertClean(WS2);
    }

    @Test
    void moveUpdatesBothHistories() throws Exception {
      saveOk(WS1, "a.txt", "content");
      assertSuccess(bindings.handleMove(Path.of("a.txt"), Path.of("a.txt"), WS1, WS2, false, USER));
      assertEquals(Set.of("a.txt", ".a.txt.meta.seqdev"), headTree(WS2));
      assertEquals(Set.of(), headTree(WS1));
      assertEquals("Move a.txt -> workspace 2:a.txt", head(WS1).getFullMessage());
      assertEquals("Move workspace 1:a.txt -> a.txt", head(WS2).getFullMessage());
      assertClean(WS1);
      assertClean(WS2);
    }

    @Test
    void directoryMoveUpdatesBothHistories() throws Exception {
      saveOk(WS1, "d/x.txt", "x");
      assertSuccess(bindings.handleMove(Path.of("d"), Path.of("d"), WS1, WS2, false, USER));
      assertEquals(Set.of("d/x.txt", "d/.x.txt.meta.seqdev"), headTree(WS2));
      assertEquals(Set.of(), headTree(WS1));
      assertClean(WS1);
      assertClean(WS2);
    }

    @Test
    void sourceCommitFailureDegradesTheMoveToACopy() throws Exception {
      saveOk(WS1, "a.txt", "content");
      final var sourceHead = head(WS1);
      history.failCommitsIn.add(root(WS1));

      final var result = bindings.handleMove(Path.of("a.txt"), Path.of("b.txt"), WS1, WS2, false, USER);
      assertFailure(result, 500, "WORKSPACE_MUTATION_PARTIAL");
      final var message = result.jsonResponse().asJsonObject().getString("message");
      assertTrue(message.contains("degraded to a copy") && message.contains("a.txt") && message.contains("b.txt"), message);

      // Destination keeps the committed copy, with its metadata; the source is restored, with its metadata.
      assertEquals(Set.of("b.txt", ".b.txt.meta.seqdev"), headTree(WS2));
      assertEquals("content", read(WS2, "b.txt"));
      assertEquals("content", read(WS1, "a.txt"));
      assertTrue(Files.exists(root(WS1).resolve(".a.txt.meta.seqdev")));
      assertEquals(sourceHead, head(WS1));
      assertClean(WS1);
      assertClean(WS2);
    }

    @Test
    void destinationCommitFailureRestoresBothWorkspaces() throws Exception {
      saveOk(WS1, "a.txt", "content");
      final var sourceHead = head(WS1);
      final var destHead = head(WS2);
      history.failCommitsIn.add(root(WS2));

      assertFailure(bindings.handleMove(Path.of("a.txt"), Path.of("b.txt"), WS1, WS2, false, USER), 500, "WORKSPACE_HISTORY_ERROR");
      assertEquals("content", read(WS1, "a.txt"));
      assertFalse(Files.exists(root(WS2).resolve("b.txt")));
      assertFalse(Files.exists(root(WS2).resolve(".b.txt.meta.seqdev")));
      assertEquals(sourceHead, head(WS1));
      assertEquals(destHead, head(WS2));
      assertClean(WS1);
      assertClean(WS2);
    }

    /** Opposite-direction cross-workspace moves would deadlock without a global lock order. */
    @Test
    void locksAreAcquiredInADeterministicOrder() throws Exception {
      for (int i = 0; i < 20; i++) {
        saveOk(WS1, "one" + i + ".txt", "1");
        saveOk(WS2, "two" + i + ".txt", "2");
      }
      final var pool = Executors.newFixedThreadPool(2);
      try {
        assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
          final var a = pool.submit(() -> {
            for (int i = 0; i < 20; i++) assertSuccess(bindings.handleMove(Path.of("one" + i + ".txt"), Path.of("one" + i + ".txt"), WS1, WS2, false, USER));
          });
          final var b = pool.submit(() -> {
            for (int i = 0; i < 20; i++) assertSuccess(bindings.handleMove(Path.of("two" + i + ".txt"), Path.of("two" + i + ".txt"), WS2, WS1, false, USER));
          });
          a.get();
          b.get();
        });
      } finally {
        pool.shutdownNow();
      }
      assertEquals(20, headTree(WS1).stream().filter(p -> p.startsWith("two")).count());
      assertEquals(20, headTree(WS2).stream().filter(p -> p.startsWith("one")).count());
      assertClean(WS1);
      assertClean(WS2);
    }
  }

  @Nested
  class InternalPaths {
    @BeforeEach
    void initWs() {
      saveOk(WS1, "a.txt", "content");
    }

    @Test
    void readsOfGitInternalsAreRejected() {
      for (final var path : List.of(".git/HEAD", ".git/config", ".git/refs/heads/main", "foo/.git/config",
                                    ".GIT/HEAD", "a/../.git/HEAD", ".seqdev/state.json", ".gitignore")) {
        assertThrows(ReservedPathException.class, () -> fs.loadFile(WS1, Path.of(path)), path);
        assertThrows(ReservedPathException.class, () -> fs.checkFileExists(WS1, Path.of(path)), path);
      }
    }

    @Test
    void writesOfGitInternalsAreRejected() throws Exception {
      final var config = Files.readString(root(WS1).resolve(".git/config"));
      for (final var path : List.of(".git/config", ".git/hooks/post-commit", "foo/.git/HEAD", ".gitattributes", ".gitignore", ".seqdev/state.json")) {
        assertFailure(save(WS1, path, "evil", "*"), 400, "RESERVED_PATH");
      }
      assertFailure(bindings.handleCreateDirectory(WS1, Path.of("x/.git"), USER), 400, "RESERVED_PATH");
      assertEquals(config, Files.readString(root(WS1).resolve(".git/config")));
      assertFalse(Files.exists(root(WS1).resolve(".gitignore")));
      assertClean(WS1);
    }

    @Test
    void moveCopyAndDeleteInvolvingGitInternalsAreRejected() throws Exception {
      final var head = head(WS1);
      assertFailure(bindings.handleMove(Path.of(".git/HEAD"), Path.of("HEAD"), WS1, WS1, false, USER), 400, "RESERVED_PATH");
      assertFailure(bindings.handleMove(Path.of("a.txt"), Path.of(".git/a.txt"), WS1, WS1, true, USER), 400, "RESERVED_PATH");
      assertFailure(bindings.handleCopy(Path.of(".git"), Path.of("leak"), WS1, WS2, false, USER), 400, "RESERVED_PATH");
      assertFailure(bindings.handleCopy(Path.of("a.txt"), Path.of(".git/objects/x"), WS1, WS1, true, USER), 400, "RESERVED_PATH");
      assertFailure(bindings.handleDelete(WS1, Path.of(".git"), USER), 400, "RESERVED_PATH");
      assertFailure(bindings.handleDelete(WS1, Path.of(".seqdev/state.json"), USER), 400, "RESERVED_PATH");
      assertEquals(head, head(WS1));
      assertTrue(Files.isRegularFile(root(WS1).resolve(".git/HEAD")));
      assertFalse(Files.exists(root(WS2).resolve("leak")));
      assertClean(WS1);
    }

    @Test
    void listingsNeverIncludeInternalDirectories() throws IOException {
      final var listed = WorkspacePaths.walk(root(WS1), Integer.MAX_VALUE).stream()
                                       .map(p -> root(WS1).relativize(p).toString()).toList();
      assertTrue(listed.contains("a.txt"));
      assertTrue(listed.stream().noneMatch(p -> p.startsWith(".git") || p.startsWith(".seqdev")), listed::toString);
    }
  }

  @Nested
  class Concurrency {
    @Test
    void nestedWorkspaceLockAcquisitionIsRejected() throws Exception {
      init(WS1);
      init(WS2);
      final var inner = Change.of("inner", Path.of("inner.txt"));

      for (final var innerWs : List.of(WS2, WS1)) { // out of lock order, and the same workspace's scope
        final var nested = assertThrows(IllegalStateException.class, () ->
            history.mutate(WS1, USER, Change.of("outer", Path.of("outer.txt")), () ->
                history.mutate(innerWs, USER, inner, () -> true, r -> r), r -> r));
        assertTrue(nested.getMessage().contains("cannot be nested"));
      }
      assertThrows(IllegalStateException.class, () -> history.withLock(WS1, () ->
          history.mutate(WS2, USER, inner, () -> true, r -> r)));
      assertClean(WS1);
      assertClean(WS2);
      saveOk(WS1, "after.txt", "the guard does not outlive the outer operation");
    }

    /** A second mutation cannot run between the first one's filesystem write and its commit. */
    @Test
    void mutationsDoNotInterleaveBetweenWriteAndCommit() throws Exception {
      init(WS1);
      final var firstWrote = new CountDownLatch(1);
      final var releaseFirst = new CountDownLatch(1);
      final var secondEntered = new AtomicBoolean(false);
      final var pool = Executors.newFixedThreadPool(2);
      try {
        final var first = pool.submit(() -> history.mutate(WS1, "first", Change.of("first", Path.of("a.txt")), () -> {
          Files.writeString(root(WS1).resolve("a.txt"), "first");
          firstWrote.countDown();
          assertTrue(releaseFirst.await(10, TimeUnit.SECONDS));
          return true;
        }, r -> true));
        assertTrue(firstWrote.await(10, TimeUnit.SECONDS));
        final var second = pool.submit(() -> history.mutate(WS1, "second", Change.of("second", Path.of("a.txt")), () -> {
          secondEntered.set(true);
          Files.writeString(root(WS1).resolve("a.txt"), "second");
          return true;
        }, r -> true));

        Thread.sleep(300);
        assertFalse(secondEntered.get(), "the second mutation started before the first was committed");
        releaseFirst.countDown();
        first.get(10, TimeUnit.SECONDS);
        second.get(10, TimeUnit.SECONDS);
      } finally {
        pool.shutdownNow();
      }

      // A valid serial order: first's commit, then second's on top of it.
      final var commits = log(WS1);
      assertEquals("second", commits.get(0).getFullMessage());
      assertEquals("first", commits.get(1).getFullMessage());
      assertEquals("second", read(WS1, "a.txt"));
      assertClean(WS1);
    }

    @Test
    void concurrentSavesAllLandInHistory() throws Exception {
      init(WS1);
      final var commitsBefore = log(WS1).size();
      final var pool = Executors.newFixedThreadPool(4);
      final var tasks = new ArrayList<java.util.concurrent.Future<?>>();
      for (int t = 0; t < 4; t++) {
        final var thread = t;
        tasks.add(pool.submit(() -> {
          for (int i = 0; i < 10; i++) saveOk(WS1, "shared.txt", "t" + thread + "-" + i);
        }));
      }
      for (final var task : tasks) task.get(60, TimeUnit.SECONDS);
      pool.shutdown();

      // Every save had distinct content, so every save is one commit (the first also adds the sidecar).
      assertEquals(commitsBefore + 40, log(WS1).size());
      try (final var git = git(WS1)) {
        final var committed = new String(
            git.getRepository().open(TreeWalk.forPath(git.getRepository(), "shared.txt", head(WS1).getTree()).getObjectId(0)).getBytes(),
            StandardCharsets.UTF_8);
        assertEquals(read(WS1, "shared.txt"), committed);
      }
      assertClean(WS1);
    }
  }

  @Nested
  class Migration {
    private void writeLegacyWorkspace() throws IOException {
      Files.createDirectories(root(WS1).resolve("seq"));
      Files.writeString(root(WS1).resolve("seq/a.txt"), "legacy A");
      Files.writeString(root(WS1).resolve("seq/.a.txt.meta.seqdev"), """
          {"version":"1","createdBy":"carol","createdAt":"2025-01-01T00:00:00Z",\
          "lastEditedBy":"dave","lastEditedAt":"2025-02-02T00:00:00Z","readOnly":true,"user":{"status":"final"}}""");
      Files.writeString(root(WS1).resolve("b.txt"), "legacy B");
    }

    @Test
    void aWorkspaceWithoutGitIsInitializedOnItsFirstMutation() throws Exception {
      writeLegacyWorkspace();
      init(WS1);

      final var commits = log(WS1);
      assertEquals(1, commits.size(), "creating an empty directory adds nothing beyond the baseline");
      assertEquals(WorkspaceHistory.INIT_MESSAGE, commits.getFirst().getFullMessage());
      assertEquals("PlanDev", commits.getFirst().getAuthorIdent().getName());
      assertEquals(Set.of("b.txt", "seq/a.txt", "seq/.a.txt.meta.seqdev"), headTree(WS1));
      assertClean(WS1);
      assertTrue(history.isManaged(root(WS1)));
      try (final var git = git(WS1)) {
        assertEquals("/dev/null", git.getRepository().getConfig().getString("core", null, "attributesFile"),
                     "global gitattributes must not apply to workspaces");
      }

      // Only versioned fields stay in the sidecar; readOnly and the pre-history lastEdited move to runtime state.
      final var sidecarJson = Files.readString(root(WS1).resolve("seq/.a.txt.meta.seqdev"));
      assertFalse(sidecarJson.contains("readOnly") || sidecarJson.contains("lastEdited"), sidecarJson);
      assertTrue(sidecarJson.contains("carol") && sidecarJson.contains("final"), sidecarJson);
      assertTrue(isReadOnly(WS1, "seq/a.txt"));
      final var info = fs.getLastEditInfo(WS1, Path.of("seq/a.txt"));
      assertEquals("dave", info.lastEditedBy());
      assertEquals("2025-02-02T00:00:00Z", info.lastEditedAt());
    }

    @Test
    void initializationIsIdempotent() throws Exception {
      writeLegacyWorkspace();
      init(WS1);
      final var head = head(WS1);
      init(WS1);
      assertSuccess(bindings.handleCreateDirectory(WS1, Path.of("another"), USER));
      assertEquals(head, head(WS1));
      assertEquals(1, log(WS1).size());
      assertClean(WS1);
    }

    @Test
    void aNewWorkspaceStartsWithAnEmptyBaseline() throws Exception {
      init(WS2);
      assertEquals(1, log(WS2).size());
      assertEquals(Set.of(), headTree(WS2));
      assertClean(WS2);
    }

    @Test
    void reservedFilesInALegacyWorkspaceFailClosed() throws Exception {
      writeLegacyWorkspace();
      Files.writeString(root(WS1).resolve("seq/.gitignore"), "*.txt\n");
      assertFailure(bindings.handleCreateDirectory(WS1, Path.of("x"), USER), 500, "WORKSPACE_REPOSITORY_INCONSISTENT");
      assertFalse(Files.exists(root(WS1).resolve(".git")));
      assertFalse(Files.exists(root(WS1).resolve("x")));
    }

    /** Interrupted after the sidecars' runtime fields reached state.json but before every sidecar was rewritten. */
    @Test
    void anInterruptedMigrationIsFinished() throws Exception {
      Files.writeString(root(WS1).resolve("a.txt"), "A");
      Files.writeString(root(WS1).resolve(".a.txt.meta.seqdev"), """
          {"version":"1","createdBy":"carol","createdAt":"2025-01-01T00:00:00Z"}""");
      Files.writeString(root(WS1).resolve("b.txt"), "B");
      Files.writeString(root(WS1).resolve(".b.txt.meta.seqdev"), """
          {"version":"1","createdBy":"carol","createdAt":"2025-01-01T00:00:00Z",\
          "lastEditedBy":"erin","lastEditedAt":"2025-03-03T00:00:00Z","readOnly":true}""");
      Files.createDirectories(root(WS1).resolve(".seqdev"));
      Files.writeString(WorkspaceState.file(root(WS1)), """
          {"version":1,"files":{"a.txt":{"readOnly":true,"legacyLastEditedBy":"dave","legacyLastEditedAt":"2025-02-02T00:00:00Z"}}}""");

      init(WS1);

      final var sidecarB = Files.readString(root(WS1).resolve(".b.txt.meta.seqdev"));
      assertFalse(sidecarB.contains("readOnly") || sidecarB.contains("lastEdited"), sidecarB);
      assertTrue(isReadOnly(WS1, "a.txt"), "already-migrated state survives");
      assertTrue(isReadOnly(WS1, "b.txt"), "the remaining legacy sidecar is migrated");
      assertEquals("dave", fs.getLastEditInfo(WS1, Path.of("a.txt")).lastEditedBy());
      assertEquals("erin", fs.getLastEditInfo(WS1, Path.of("b.txt")).lastEditedBy());
      assertClean(WS1);
    }

    /**
     * A real I/O failure while replacing one sidecar (its directory is not writable): the sidecar is either fully
     * legacy or fully migrated, never partial, and the next adoption finishes the job without losing metadata.
     */
    @Test
    void aFailedSidecarReplacementIsAtomicAndResumable() throws Exception {
      assumeTrue(POSIX && !"root".equals(System.getProperty("user.name")), "needs POSIX permissions enforced");
      final var legacy = """
          {"version":"1","createdBy":"carol","createdAt":"2025-01-01T00:00:00Z",\
          "lastEditedBy":"dave","lastEditedAt":"2025-02-02T00:00:00Z","readOnly":true}""";
      for (final var dir : List.of("a", "b")) {
        Files.createDirectories(root(WS1).resolve(dir));
        Files.writeString(root(WS1).resolve(dir + "/x.txt"), dir);
        Files.writeString(root(WS1).resolve(dir + "/.x.txt.meta.seqdev"), legacy);
      }
      final var locked = root(WS1).resolve("b");
      Files.setPosixFilePermissions(locked, PosixFilePermissions.fromString("r-xr-xr-x"));
      try {
        assertFailure(bindings.handleCreateDirectory(WS1, Path.of("z"), USER), 500, null);
      } finally {
        Files.setPosixFilePermissions(locked, PosixFilePermissions.fromString("rwxr-xr-x"));
      }
      assertFalse(read(WS1, "a/.x.txt.meta.seqdev").contains("readOnly"), "a was migrated");
      assertEquals(legacy, read(WS1, "b/.x.txt.meta.seqdev"), "b is untouched, not partially written");
      assertEquals(Optional.of(true), WorkspaceState.load(root(WS1)).readOnly("b/x.txt"));
      try (final var internal = Files.list(root(WS1).resolve(".seqdev"))) {
        assertEquals(List.of("state.json"), internal.map(p -> p.getFileName().toString()).toList());
      }
      assertFalse(history.isManaged(root(WS1)));

      init(WS1);
      assertFalse(read(WS1, "b/.x.txt.meta.seqdev").contains("readOnly"));
      for (final var file : List.of("a/x.txt", "b/x.txt")) {
        assertTrue(isReadOnly(WS1, file), file);
        assertEquals("dave", fs.getLastEditInfo(WS1, Path.of(file)).lastEditedBy(), file);
      }
      assertClean(WS1);
    }

    /** A sidecar whose runtime fields cannot be read might be a lock; adoption refuses rather than unlocking it. */
    @Test
    void unmigratableLegacySidecarsFailAdoptionClosed() throws Exception {
      writeLegacyWorkspace();
      final var goodSidecar = read(WS1, "seq/.a.txt.meta.seqdev");
      Files.writeString(root(WS1).resolve(".b.txt.meta.seqdev"), "{\"readOnly\": tru");
      Files.writeString(root(WS1).resolve("c.txt"), "C");
      Files.writeString(root(WS1).resolve(".c.txt.meta.seqdev"), "{\"version\":\"1\",\"readOnly\":\"true\"}");

      final var result = bindings.handleCreateDirectory(WS1, Path.of("x"), USER);
      assertFailure(result, 500, "WORKSPACE_REPOSITORY_INCONSISTENT");
      final var message = result.jsonResponse().asJsonObject().getString("message");
      assertTrue(message.contains(".b.txt.meta.seqdev") && message.contains(".c.txt.meta.seqdev") && message.contains("readOnly"), message);
      assertFalse(Files.exists(root(WS1).resolve(".git")), "nothing was changed");
      assertFalse(Files.exists(WorkspaceState.file(root(WS1))));
      assertEquals(goodSidecar, read(WS1, "seq/.a.txt.meta.seqdev"));

      // Once repaired explicitly, adoption succeeds and every lock survives
      Files.writeString(root(WS1).resolve(".b.txt.meta.seqdev"), "{\"readOnly\": true}");
      Files.writeString(root(WS1).resolve(".c.txt.meta.seqdev"), "{\"version\":\"1\",\"readOnly\":true}");
      init(WS1);
      for (final var file : List.of("seq/a.txt", "b.txt", "c.txt")) assertTrue(isReadOnly(WS1, file), file);
      assertClean(WS1);
    }

    @Test
    void migrationKeepsEverythingButTheRuntimeFields() throws Exception {
      Files.writeString(root(WS1).resolve("a.txt"), "A");
      Files.writeString(root(WS1).resolve(".a.txt.meta.seqdev"), """
          {"version":"1","createdBy":"carol","createdAt":"2025-01-01T00:00:00Z","user":{"status":"final"},\
          "custom":{"x":1},"readOnly":true,"lastEditedBy":"dave","lastEditedAt":"2025-02-02T00:00:00Z"}""");
      WorkspaceState.migrateFromSidecars(root(WS1));
      try (final var reader = Json.createReader(Files.newBufferedReader(root(WS1).resolve(".a.txt.meta.seqdev")))) {
        assertEquals(Json.createReader(new java.io.StringReader("""
            {"version":"1","createdBy":"carol","createdAt":"2025-01-01T00:00:00Z","user":{"status":"final"},"custom":{"x":1}}"""))
                         .readObject(), reader.readObject());
      }
    }

    @Test
    void migrationIsIdempotent() throws Exception {
      writeLegacyWorkspace();
      WorkspaceState.migrateFromSidecars(root(WS1));
      final var state = Files.readAllBytes(WorkspaceState.file(root(WS1)));
      final var sidecarA = Files.readAllBytes(root(WS1).resolve("seq/.a.txt.meta.seqdev"));
      WorkspaceState.migrateFromSidecars(root(WS1));
      assertArrayEquals(state, Files.readAllBytes(WorkspaceState.file(root(WS1))));
      assertArrayEquals(sidecarA, Files.readAllBytes(root(WS1).resolve("seq/.a.txt.meta.seqdev")));
    }
  }

  /** A managed workspace must be exactly as PlanDev left it before anything is mutated. */
  @Nested
  class Trust {
    private static final String INCONSISTENT = "WORKSPACE_REPOSITORY_INCONSISTENT";

    @BeforeEach
    void initWs() {
      saveOk(WS1, "a.txt", "v1");
    }

    /** Asserts a save is refused and changes nothing: no file, no commit. */
    private String assertRefused() throws Exception {
      final var commits = log(WS1).size();
      final var result = save(WS1, "b.txt", "v1", null);
      assertFailure(result, 500, INCONSISTENT);
      assertFalse(Files.exists(root(WS1).resolve("b.txt")));
      assertEquals(commits, log(WS1).size());
      return result.jsonResponse().asJsonObject().getString("message");
    }

    @Test
    void outOfBandChangesAreRejectedNotCommitted() throws Exception {
      Files.writeString(root(WS1).resolve("a.txt"), "edited on disk");
      Files.writeString(root(WS1).resolve("dropped-in.txt"), "new on disk");

      final var message = assertRefused();
      assertTrue(message.contains("a.txt") && message.contains("dropped-in.txt"), message);
      assertEquals("edited on disk", read(WS1, "a.txt"), "left untouched, not reset");
      assertTrue(Files.exists(root(WS1).resolve("dropped-in.txt")));
      assertTrue(log(WS1).stream().noneMatch(c -> c.getAuthorIdent().getName().equals("PlanDev") && c.getParentCount() > 0));
    }

    @Test
    void detachedHeadIsRejected() throws Exception {
      try (final var git = git(WS1)) {
        git.checkout().setName(head(WS1).name()).call();
      }
      assertTrue(assertRefused().contains("detached"));
    }

    @Test
    void anotherBranchIsRejected() throws Exception {
      try (final var git = git(WS1)) {
        git.checkout().setCreateBranch(true).setName("other").call();
      }
      assertTrue(assertRefused().contains("'other'"));
    }

    @Test
    void inProgressGitOperationsAreRejected() throws Exception {
      final var head = head(WS1).name() + "\n";
      for (final var marker : List.of("MERGE_HEAD", "CHERRY_PICK_HEAD", "REVERT_HEAD", "BISECT_LOG", "rebase-merge/head-name")) {
        final var file = root(WS1).resolve(".git").resolve(marker);
        Files.createDirectories(file.getParent());
        Files.writeString(file, head);
        assertTrue(assertRefused().contains("in progress"), marker);
        Files.delete(file);
      }
      Files.delete(root(WS1).resolve(".git/rebase-merge"));
      saveOk(WS1, "b.txt", "v1");
    }

    interface Corruption {
      void apply(Path seqdev, Path state) throws IOException;
    }

    /** Everything under .seqdev, so a test can tell the corruption was left exactly as it was. */
    private static String fingerprint(final Path seqdev) throws IOException {
      if (!Files.exists(seqdev, LinkOption.NOFOLLOW_LINKS)) return "absent";
      try (final var paths = Files.walk(seqdev)) {
        final var out = new StringBuilder();
        for (final var p : paths.sorted().toList()) {
          out.append(seqdev.relativize(p)).append(Files.isDirectory(p) ? "/" : "=" + Files.readString(p)).append('\n');
        }
        return out.toString();
      }
    }

    @Test
    void invalidRuntimeStateIsRejectedAndLeftUntouched() throws Exception {
      final var seqdev = root(WS1).resolve(".seqdev");
      final var state = WorkspaceState.file(root(WS1));
      final var good = Files.readAllBytes(state);
      final var cases = new LinkedHashMap<String, Corruption>();
      cases.put("missing .seqdev", (d, f) -> deleteRecursively(d));
      cases.put(".seqdev is a file", (d, f) -> { deleteRecursively(d); Files.writeString(d, "x"); });
      cases.put("missing state.json", (d, f) -> Files.delete(f));
      cases.put("state.json is a directory", (d, f) -> { Files.delete(f); Files.createDirectory(f); });
      cases.put("malformed JSON", (d, f) -> Files.writeString(f, "{\"version\":1,"));
      cases.put("unsupported version", (d, f) -> Files.writeString(f, "{\"version\":2,\"files\":{}}"));
      cases.put("missing version", (d, f) -> Files.writeString(f, "{\"files\":{}}"));
      cases.put("files is not an object", (d, f) -> Files.writeString(f, "{\"version\":1,\"files\":[]}"));
      cases.put("readOnly is not a boolean", (d, f) ->
          Files.writeString(f, "{\"version\":1,\"files\":{\"a.txt\":{\"readOnly\":\"no\"}}}"));

      for (final var c : cases.entrySet()) {
        c.getValue().apply(seqdev, state);
        final var corrupted = fingerprint(seqdev);
        assertTrue(assertRefused().contains("invalid runtime state"), c.getKey());
        assertEquals(corrupted, fingerprint(seqdev), c.getKey() + ": must not be repaired");

        deleteRecursively(seqdev);
        Files.createDirectories(seqdev);
        Files.write(state, good);
      }
      saveOk(WS1, "b.txt", "v1");
    }

    @Test
    void staleTempFilesAreRemovedAndNothingElseIs() throws Exception {
      final var seqdev = root(WS1).resolve(".seqdev");
      final var stale = seqdev.resolve("tmp-" + java.util.UUID.randomUUID());
      Files.writeString(stale, "left by a crash");
      final var others = List.of(seqdev.resolve("tmp-notauuid"), seqdev.resolve("notes.tmp"));
      for (final var other : others) Files.writeString(other, "not ours");

      saveOk(WS1, "b.txt", "v1");
      assertFalse(Files.exists(stale));
      others.forEach(o -> assertTrue(Files.exists(o), o::toString));
      assertClean(WS1);
    }

    @Test
    void anAdoptionThatDidNotFinishIsRedoneNotRejected() throws Exception {
      try (final var git = git(WS1)) {
        final var cfg = git.getRepository().getConfig();
        cfg.unset("plandev", null, "managed");
        cfg.save();
      }
      Files.writeString(root(WS1).resolve("a.txt"), "written before the crash");
      saveOk(WS1, "b.txt", "v1");
      assertEquals(WorkspaceHistory.ADOPT_MESSAGE, log(WS1).get(1).getFullMessage());
      assertTrue(history.isManaged(root(WS1)));
      assertClean(WS1);
    }
  }

  @Nested
  class WorkspaceInitialization {
    @Test
    void newWorkspaceGetsManagedHistoryBeforeItsFirstMutation() throws Exception {
      final var newRoot = Files.createDirectory(base.resolve("new"));

      history.initializeNewWorkspace(newRoot);

      assertTrue(history.isManaged(newRoot));
      try (final var git = Git.open(newRoot.toFile())) {
        final var commits = new ArrayList<RevCommit>();
        git.log().call().forEach(commits::add);
        assertEquals(1, commits.size());
        assertEquals(WorkspaceHistory.INIT_MESSAGE, commits.getFirst().getFullMessage());
        assertEquals(Set.of(), WorkspaceHistory.dirtyPaths(git.status().call()));
      }
    }

    @Test
    void newWorkspaceInitializationDoesNotAdoptExistingFiles() throws Exception {
      final var newRoot = Files.createDirectory(base.resolve("not-new"));
      Files.writeString(newRoot.resolve("existing.txt"), "existing");

      assertThrows(IOException.class, () -> history.initializeNewWorkspace(newRoot));

      assertFalse(Files.exists(newRoot.resolve(WorkspacePaths.GIT_DIR)));
      assertEquals("existing", Files.readString(newRoot.resolve("existing.txt")));
    }

    @Test
    void failedInitializationDoesNotMarkTheWorkspaceManaged() throws Exception {
      final var newRoot = Files.createDirectory(base.resolve("failed"));
      history.failCommitsIn.add(newRoot.toRealPath());

      assertThrows(GitAPIException.class, () -> history.initializeNewWorkspace(newRoot));

      assertFalse(history.isManaged(newRoot));
      try (final var git = Git.open(newRoot.toFile())) {
        assertNull(git.getRepository().resolve("HEAD"));
      }
    }
  }

  @Nested
  class Adoption {
    private Git foreignRepo(final int ws, final String branch) throws GitAPIException {
      return Git.init().setDirectory(root(ws).toFile()).setInitialBranch(branch).call();
    }

    private static void commitAll(final Git git) throws GitAPIException {
      git.add().addFilepattern(".").call();
      final var someone = new PersonIdent("someone", "");
      git.commit().setMessage("external").setAuthor(someone).setCommitter(someone).setSign(false).call();
    }

    @Test
    void aCleanExistingRepoWithGitControlFilesIsRejected() throws Exception {
      var ws = 10;
      for (final var name : List.of(".gitignore", ".gitattributes", ".gitmodules", "sub/.gitignore")) {
        ws++;
        Files.createDirectories(root(ws).resolve(name).getParent());
        try (final var git = foreignRepo(ws, "main")) {
          Files.writeString(root(ws).resolve("a.txt"), "A");
          Files.writeString(root(ws).resolve(name), "x\n");
          commitAll(git);
        }
        assertClean(ws);
        final var commits = log(ws).size();

        assertFailure(bindings.handleCreateDirectory(ws, Path.of("x"), USER), 500, "WORKSPACE_REPOSITORY_INCONSISTENT");
        assertEquals(commits, log(ws).size(), name);
        assertFalse(history.isManaged(root(ws)), name);
      }
    }

    @Test
    void anExistingRepoIsAdoptedWithItsUncommittedState() throws Exception {
      try (final var git = foreignRepo(WS1, "main")) {
        Files.writeString(root(WS1).resolve("a.txt"), "committed");
        commitAll(git);
      }
      Files.writeString(root(WS1).resolve("a.txt"), "uncommitted");
      saveOk(WS1, "b.txt", "B");

      final var commits = log(WS1);
      assertEquals(List.of("Update b.txt", WorkspaceHistory.ADOPT_MESSAGE, "external"),
                   commits.stream().map(RevCommit::getFullMessage).toList());
      assertEquals("uncommitted", read(WS1, "a.txt"));
      assertClean(WS1);
    }

    @Test
    void invalidExistingRuntimeStateBlocksAdoption() throws Exception {
      Files.writeString(root(WS1).resolve("a.txt"), "A");
      Files.createDirectories(root(WS1).resolve(".seqdev"));
      Files.writeString(WorkspaceState.file(root(WS1)), "{\"version\":7,\"files\":{}}");
      assertFailure(bindings.handleCreateDirectory(WS1, Path.of("x"), USER), 500, "WORKSPACE_REPOSITORY_INCONSISTENT");
      assertFalse(Files.exists(root(WS1).resolve(".git")));
      assertEquals("{\"version\":7,\"files\":{}}", Files.readString(WorkspaceState.file(root(WS1))));

      Files.delete(WorkspaceState.file(root(WS1)));
      Files.delete(root(WS1).resolve(".seqdev"));
      Files.writeString(root(WS1).resolve(".seqdev"), "not a directory");
      assertFailure(bindings.handleCreateDirectory(WS1, Path.of("x"), USER), 500, "WORKSPACE_REPOSITORY_INCONSISTENT");
      assertFalse(Files.exists(root(WS1).resolve(".git")));
    }

    @Test
    void anExistingRepoOnAnotherBranchIsRejected() throws Exception {
      final var legacySidecar = """
          {"version":"1","createdBy":"carol","createdAt":"2025-01-01T00:00:00Z","readOnly":true}""";
      try (final var git = foreignRepo(WS1, "master")) {
        Files.writeString(root(WS1).resolve("a.txt"), "A");
        Files.writeString(root(WS1).resolve(".a.txt.meta.seqdev"), legacySidecar);
        commitAll(git);
      }
      assertFailure(save(WS1, "b.txt", "B", null), 500, "WORKSPACE_REPOSITORY_INCONSISTENT");
      assertFalse(Files.exists(root(WS1).resolve("b.txt")));
      assertFalse(history.isManaged(root(WS1)));
      assertEquals(legacySidecar, read(WS1, ".a.txt.meta.seqdev"), "nothing is migrated before validation passes");
      assertFalse(Files.exists(WorkspaceState.file(root(WS1))));
      assertClean(WS1);
    }
  }

  @Nested
  class Symlinks {
    private Path outside;

    @BeforeEach
    void setUpLinks() throws IOException {
      outside = Files.createDirectories(base.resolve("outside"));
      Files.writeString(outside.resolve("secret.txt"), "secret");
    }

    @Test
    void anUnmanagedWorkspaceWithASymlinkIsNotAdopted() throws Exception {
      Files.writeString(root(WS1).resolve("a.txt"), "A");
      Files.createSymbolicLink(root(WS1).resolve("link"), outside);
      final var result = bindings.handleCreateDirectory(WS1, Path.of("x"), USER);
      assertFailure(result, 500, "WORKSPACE_REPOSITORY_INCONSISTENT");
      assertTrue(result.jsonResponse().asJsonObject().getString("message").contains("link (symbolic link)"));
      assertFalse(Files.exists(root(WS1).resolve(".git")));
    }

    @Test
    void pathsThroughASymlinkCannotBeReadOrWritten() throws Exception {
      saveOk(WS1, "d/x.txt", "x");
      final var head = head(WS1);
      Files.createSymbolicLink(root(WS1).resolve("toGit"), Path.of(".git"));
      Files.createSymbolicLink(root(WS1).resolve("toOutside"), outside);
      Files.createSymbolicLink(root(WS1).resolve("toInside"), Path.of("d"));

      for (final var path : List.of("toGit/config", "toOutside/secret.txt", "toInside/x.txt", "toGit", "toInside")) {
        assertThrows(ReservedPathException.class, () -> fs.loadFile(WS1, Path.of(path)), path);
        assertThrows(ReservedPathException.class, () -> fs.checkFileExists(WS1, Path.of(path)), path);
      }
      // A mutation's declared paths may not go through a link, so nothing is written anywhere, inside or outside it.
      assertFailure(save(WS1, "toOutside/secret.txt", "overwritten", "*"), 400, "RESERVED_PATH");
      assertFailure(save(WS1, "toGit/config", "overwritten", "*"), 400, "RESERVED_PATH");
      assertFailure(bindings.handleDelete(WS1, Path.of("toInside/x.txt"), USER), 400, "RESERVED_PATH");
      assertEquals("x", read(WS1, "d/x.txt"));
      assertEquals("secret", Files.readString(outside.resolve("secret.txt")));
      assertEquals(head, head(WS1));
    }

    @Test
    void aSymlinkCommittedOutOfBandIsRejected() throws Exception {
      saveOk(WS1, "a.txt", "A");
      Files.createSymbolicLink(root(WS1).resolve("link"), outside);
      try (final var git = git(WS1)) {
        git.add().addFilepattern("link").call();
        git.commit().setMessage("sneak").setSign(false).call();
      }
      assertClean(WS1);
      final var result = save(WS1, "b.txt", "B", null);
      assertFailure(result, 500, "WORKSPACE_REPOSITORY_INCONSISTENT");
      assertTrue(result.jsonResponse().asJsonObject().getString("message").contains("link"));
    }

    @Test
    void scansDoNotFollowOrReturnSymlinks() throws IOException {
      Files.createDirectories(root(WS1).resolve("d"));
      Files.writeString(root(WS1).resolve("d/x.txt"), "x");
      Files.createSymbolicLink(root(WS1).resolve("toOutside"), outside);
      Files.createSymbolicLink(root(WS1).resolve("toInside"), Path.of("d"));
      final var listed = WorkspacePaths.walk(root(WS1), Integer.MAX_VALUE).stream()
                                       .map(p -> WorkspacePaths.key(root(WS1), p)).toList();
      assertEquals(List.of("d", "d/x.txt"), listed.stream().sorted().toList());
    }
  }

  /**
   * A copied file is a new file, whether it is copied on its own or inside a directory; a copied or moved file never
   * inherits metadata from a file it overwrites.
   */
  @Nested
  class CopySemantics {
    private JsonObject metadata(final String path) throws Exception {
      return WorkspaceHistoryIntegrationTest.this.metadata(WS1, path);
    }

    /** Destination has carol's sidecar with user metadata; source has no sidecar at all. */
    private void sourceWithoutSidecarAndDestinationWithOne() throws Exception {
      assertSuccess(bindings.handleFileUpload(WS1, Path.of("dest.txt"), upload("dest.txt", "old"), true, null, "carol"));
      setUserMetadata(WS1, "dest.txt", "final");
      saveOk(WS1, "src.txt", "new");
      deleteSidecar(WS1, "src.txt");
      assertFalse(Files.exists(root(WS1).resolve(".src.txt.meta.seqdev")));
    }

    @Test
    void aCopyDoesNotInheritTheOverwrittenFilesMetadata() throws Exception {
      sourceWithoutSidecarAndDestinationWithOne();
      assertSuccess(bindings.handleCopy(Path.of("src.txt"), Path.of("dest.txt"), WS1, WS1, true, USER));
      final var meta = metadata("dest.txt");
      assertFalse(meta.containsKey("user"), meta::toString);
      assertEquals(USER, meta.getString("createdBy"));
      assertEquals("new", read(WS1, "dest.txt"));
      assertClean(WS1);
    }

    @Test
    void aMoveDoesNotInheritTheOverwrittenFilesMetadata() throws Exception {
      sourceWithoutSidecarAndDestinationWithOne();
      assertSuccess(bindings.handleMove(Path.of("src.txt"), Path.of("dest.txt"), WS1, WS1, true, USER));
      final var meta = metadata("dest.txt");
      assertFalse(meta.containsKey("user"), meta::toString);
      assertEquals(USER, meta.getString("createdBy"));
      assertEquals("new", read(WS1, "dest.txt"));
      assertClean(WS1);
    }

    @Test
    void theExecutableBitSurvivesSavesAndCopies() throws Exception {
      assumeTrue(POSIX, "needs POSIX permissions");
      saveOk(WS1, "run.sh", "v1");
      Files.setPosixFilePermissions(root(WS1).resolve("run.sh"), PosixFilePermissions.fromString("rwxr-xr-x"));
      try (final var git = git(WS1)) { // record the mode as PlanDev would find it in a managed workspace
        git.add().addFilepattern("run.sh").call();
        git.commit().setMessage("chmod").setSign(false).call();
      }

      saveOk(WS1, "run.sh", "v2");
      assertTrue(Files.isExecutable(root(WS1).resolve("run.sh")));
      final var diff = headDiff(WS1);
      assertEquals(1, diff.size(), diff::toString);
      assertEquals(FileMode.EXECUTABLE_FILE, diff.getFirst().getOldMode());
      assertEquals(FileMode.EXECUTABLE_FILE, diff.getFirst().getNewMode(), "a save changes content, not mode");

      assertSuccess(bindings.handleCopy(Path.of("run.sh"), Path.of("copy.sh"), WS1, WS1, false, USER));
      assertTrue(Files.isExecutable(root(WS1).resolve("copy.sh")));
      assertClean(WS1);
    }

    @Test
    void fileAndDirectoryCopiesProduceTheSameMetadata() throws Exception {
      assertSuccess(bindings.handleFileUpload(WS1, Path.of("d/x.txt"), upload("x.txt", "x"), true, null, "carol"));
      setUserMetadata(WS1, "d/x.txt", "final");
      setReadOnly(WS1, "d/x.txt", true);

      assertSuccess(bindings.handleCopy(Path.of("d/x.txt"), Path.of("single.txt"), WS1, WS1, false, USER));
      assertSuccess(bindings.handleCopy(Path.of("d"), Path.of("e"), WS1, WS1, false, USER));

      final var single = metadata("single.txt");
      final var inDir = metadata("e/x.txt");
      for (final var copy : List.of(single, inDir)) {
        assertEquals(USER, copy.getString("createdBy"), copy::toString);
        assertEquals(USER, copy.getString("lastEditedBy"), copy::toString);
        assertEquals("final", copy.getJsonObject("user").getString("status"), copy::toString);
        assertFalse(copy.getBoolean("readOnly"), copy::toString);
      }
      final var ignoringTimes = (java.util.function.Function<JsonObject, JsonObject>) o ->
          Json.createObjectBuilder(o).remove("createdAt").remove("lastEditedAt").build();
      assertEquals(ignoringTimes.apply(single), ignoringTimes.apply(inDir));
      assertTrue(isReadOnly(WS1, "d/x.txt"), "the source keeps its lock");
      assertFalse(isReadOnly(WS1, "e/x.txt"));
      assertEquals("carol", metadata("d/x.txt").getString("createdBy"));
      assertClean(WS1);
    }
  }

  /** Every write a mutation makes must fall within the paths it declared. */
  @Nested
  class DeclaredScopes {
    @Test
    void fileOperationsRefuseToWriteOutsideTheDeclaredPaths() throws Exception {
      init(WS1);
      final var head = head(WS1);
      assertThrows(IllegalStateException.class, () -> history.mutate(WS1, USER, Change.of("x", Path.of("a.txt")),
          () -> fs.createDirectory(WS1, Path.of("elsewhere")), r -> r));
      // Saving a file also writes its sidecar, which must be declared too
      assertThrows(IllegalStateException.class, () -> history.mutate(WS1, USER, Change.of("x", Path.of("a.txt")),
          () -> fs.saveFile(WS1, Path.of("a.txt"), upload("a.txt", "x"), USER), Optional::isPresent));
      assertFalse(Files.exists(root(WS1).resolve("elsewhere")));
      assertFalse(Files.exists(root(WS1).resolve("a.txt")));
      assertEquals(head, head(WS1));
      assertClean(WS1);
    }

    @Test
    void declaredPathsAreValidated() throws Exception {
      init(WS1);
      final var head = head(WS1);
      for (final var path : List.of(".git/config", "d/.seqdev/x", ".gitignore", ".GIT")) {
        assertThrows(ReservedPathException.class,
            () -> history.mutate(WS1, USER, Change.of("x", Path.of(path)), () -> true, r -> r), path);
      }
      assertThrows(SecurityException.class,
          () -> history.mutate(WS1, USER, Change.of("x", Path.of("../ws2/a.txt")), () -> true, r -> r));
      for (final var path : List.of("", ".", "d/..")) {
        assertThrows(WorkspaceFileOpException.class,
            () -> history.mutate(WS1, USER, Change.of("x", Path.of(path)), () -> true, r -> r), path);
      }
      assertEquals(head, head(WS1));
    }

    /** Staging is scoped, so a write that bypasses the scope guard would be left uncommitted: it fails instead. */
    @Test
    void anUndeclaredWriteFailsTheMutationAndIsRolledBack() throws Exception {
      saveOk(WS1, "a.txt", "v1");
      final var head = head(WS1);
      final var ex = assertThrows(WorkspaceHistory.WorkspaceHistoryException.class, () ->
          history.mutate(WS1, USER, Change.of("x", Path.of("a.txt")), () -> {
            Files.writeString(root(WS1).resolve("a.txt"), "v2");
            Files.writeString(root(WS1).resolve("undeclared.txt"), "x");
            return true;
          }, r -> true));
      assertEquals(WorkspaceHistory.Kind.HISTORY_NOT_RECORDED, ex.kind);
      assertEquals(head, head(WS1));
      assertEquals("v1", read(WS1, "a.txt"));
      assertFalse(Files.exists(root(WS1).resolve("undeclared.txt")));
      assertClean(WS1);
    }

    @Test
    void eachOperationCommitsExactlyItsDeclaredPaths() throws Exception {
      saveOk(WS1, "a.txt", "content");
      saveOk(WS1, "b.txt", "content");
      saveOk(WS1, "d/x.txt", "content");
      init(WS2);

      assertSuccess(bindings.handleDelete(WS1, Path.of("a.txt"), USER));
      assertEquals(Set.of("a.txt", ".a.txt.meta.seqdev"), headChanges(WS1));
      assertTrue(headDiff(WS1).stream().allMatch(d -> d.getChangeType() == DiffEntry.ChangeType.DELETE));
      assertSuccess(bindings.handleMove(Path.of("d"), Path.of("e"), WS1, WS1, false, USER));
      assertEquals(Set.of("d/x.txt", "d/.x.txt.meta.seqdev", "e/x.txt", "e/.x.txt.meta.seqdev"), headChanges(WS1));
      assertSuccess(bindings.handleMove(Path.of("b.txt"), Path.of("moved.txt"), WS1, WS2, false, USER));
      assertEquals(Set.of("b.txt", ".b.txt.meta.seqdev"), headChanges(WS1));
      assertEquals(Set.of("moved.txt", ".moved.txt.meta.seqdev"), headChanges(WS2));
      final var ws1Head = head(WS1);
      assertSuccess(bindings.handleCopy(Path.of("e"), Path.of("copied"), WS1, WS2, false, USER));
      assertEquals(ws1Head, head(WS1), "a copy's source records nothing");
      assertEquals(Set.of("copied/x.txt", "copied/.x.txt.meta.seqdev"), headChanges(WS2));
      assertClean(WS1);
      assertClean(WS2);
    }

    /** A directory root covers its subtree only: not siblings that merely share its name as a prefix. */
    @Test
    void aDirectoryRootDoesNotCoverPrefixSiblings() throws Exception {
      init(WS1);
      for (final var path : List.of("dx.txt", "d2/z.txt")) {
        assertThrows(IllegalStateException.class, () -> history.mutate(WS1, USER, Change.of("x", Path.of("d")),
            () -> fs.saveFile(WS1, Path.of(path), upload("x", "x"), USER), Optional::isPresent), path);
      }
      assertTrue(history.mutate(WS1, USER, Change.of("x", Path.of("d")),
          () -> fs.saveFile(WS1, Path.of("d/sub/y.txt"), upload("y.txt", "y"), USER), Optional::isPresent).isPresent());
      assertEquals(Set.of("d/sub/y.txt", "d/sub/.y.txt.meta.seqdev"), headChanges(WS1));
    }
  }

  /**
   * A process can die anywhere between changing files and committing them. Each test stops a mutation at a checkpoint
   * exactly as a crash would (no rollback runs), then starts a new process on the same volume, whose next use of the
   * workspace must resolve the interrupted change to either the old or the new committed state.
   */
  @Nested
  class CrashRecovery {
    private void crash(final WorkspaceHistory.Checkpoint at, final int ws, final Runnable operation) {
      history.crashAt = at;
      history.crashIn = root(ws);
      assertThrows(SimulatedCrash.class, operation::run);
      assertTrue(Files.exists(WorkspaceHistory.recordFile(root(ws))), "the crash left its record behind");
      restart();
    }

    /** The next use of a workspace recovers it; a mutation that declares no paths records nothing itself. */
    private void use(final int ws) throws Exception {
      history.mutate(ws, USER, Change.of("use"), () -> true, r -> r);
    }

    private void assertRecovered(final int ws) throws Exception {
      assertFalse(Files.exists(WorkspaceHistory.recordFile(root(ws))));
      assertClean(ws);
    }

    @Test
    void aCreateInterruptedBeforeStagingIsRolledBack() throws Exception {
      saveOk(WS1, "a.txt", "v1");
      final var before = head(WS1);
      crash(WorkspaceHistory.Checkpoint.BEFORE_STAGE, WS1, () -> save(WS1, "dir/new.txt", "x", null));
      assertTrue(Files.exists(root(WS1).resolve("dir/new.txt")), "written before the crash");

      saveOk(WS1, "other.txt", "next");
      assertFalse(Files.exists(root(WS1).resolve("dir")), "the created file, its sidecar and its new folder are gone");
      assertEquals(before, head(WS1).getParent(0));
      assertEquals(Set.of("other.txt", ".other.txt.meta.seqdev"), headChanges(WS1));
      assertRecovered(WS1);
    }

    @Test
    void aSaveInterruptedAfterStagingIsRolledBack() throws Exception {
      saveOk(WS1, "a.txt", "v1");
      final var before = head(WS1);
      crash(WorkspaceHistory.Checkpoint.BEFORE_COMMIT, WS1, () -> save(WS1, "a.txt", "v2", "*"));
      try (final var git = git(WS1)) {
        assertEquals(Set.of("a.txt"), git.status().call().getChanged(), "staged, not committed");
      }

      use(WS1);
      assertEquals("v1", read(WS1, "a.txt"));
      assertEquals(before, head(WS1));
      assertRecovered(WS1);
    }

    @Test
    void aSaveInterruptedAfterItsCommitIsKept() throws Exception {
      saveOk(WS1, "a.txt", "v1");
      final var before = head(WS1);
      crash(WorkspaceHistory.Checkpoint.BEFORE_RECORD_CLEARED, WS1, () -> save(WS1, "a.txt", "v2", "*"));

      saveOk(WS1, "b.txt", "next");
      assertEquals("v2", read(WS1, "a.txt"));
      final var commits = log(WS1);
      assertEquals("Update b.txt", commits.get(0).getFullMessage());
      assertEquals("Update a.txt", commits.get(1).getFullMessage());
      assertEquals(before, commits.get(2));
      assertRecovered(WS1);
    }

    @Test
    void aDeleteInterruptedBeforeStagingIsRolledBack() throws Exception {
      saveOk(WS1, "a.txt", "content");
      setUserMetadata(WS1, "a.txt", "final");
      setReadOnly(WS1, "a.txt", false); // an explicit flag, so runtime state has an entry the delete removes
      final var sidecar = read(WS1, ".a.txt.meta.seqdev");
      final var state = Files.readAllBytes(WorkspaceState.file(root(WS1)));
      crash(WorkspaceHistory.Checkpoint.BEFORE_STAGE, WS1, () -> bindings.handleDelete(WS1, Path.of("a.txt"), USER));
      assertFalse(Files.exists(root(WS1).resolve("a.txt")));

      use(WS1);
      assertEquals("content", read(WS1, "a.txt"));
      assertEquals(sidecar, read(WS1, ".a.txt.meta.seqdev"));
      assertArrayEquals(state, Files.readAllBytes(WorkspaceState.file(root(WS1))));
      assertRecovered(WS1);
    }

    @Test
    void aMoveInterruptedAfterStagingIsRolledBackWithItsLock() throws Exception {
      saveOk(WS1, "d/a.txt", "content");
      setReadOnly(WS1, "d/a.txt", true);
      final var before = head(WS1);
      crash(WorkspaceHistory.Checkpoint.BEFORE_COMMIT, WS1,
            () -> bindings.handleMove(Path.of("d"), Path.of("e"), WS1, WS1, false, USER));
      assertTrue(Files.exists(root(WS1).resolve("e/a.txt")));

      use(WS1);
      assertEquals("content", read(WS1, "d/a.txt"));
      assertFalse(Files.exists(root(WS1).resolve("e")));
      assertEquals(before, head(WS1));
      assertTrue(isReadOnly(WS1, "d/a.txt"), "the lock is back where the file is");
      assertEquals(List.of("d/a.txt"), WorkspaceState.load(root(WS1)).readOnlyAtOrUnder(""));
      assertRecovered(WS1);
    }

    /** The destination committed first; the source's change is rolled back, so the move degrades to a copy. */
    @Test
    void aCrossWorkspaceMoveInterruptedBetweenCommitsDegradesToACopy() throws Exception {
      saveOk(WS1, "a.txt", "content");
      init(WS2);
      final var sourceHead = head(WS1);
      crash(WorkspaceHistory.Checkpoint.BEFORE_STAGE, WS1,
            () -> bindings.handleMove(Path.of("a.txt"), Path.of("a.txt"), WS1, WS2, false, USER));
      assertFalse(Files.exists(WorkspaceHistory.recordFile(root(WS2))), "the destination had finished");

      use(WS1);
      assertEquals("content", read(WS1, "a.txt"));
      assertEquals(sourceHead, head(WS1));
      assertEquals("content", read(WS2, "a.txt"));
      assertEquals(Set.of("a.txt", ".a.txt.meta.seqdev"), headTree(WS2));
      assertRecovered(WS1);
      assertRecovered(WS2);
    }

    @Test
    void gitLockFilesLeftByTheCrashedProcessAreRemoved() throws Exception {
      saveOk(WS1, "a.txt", "v1");
      crash(WorkspaceHistory.Checkpoint.BEFORE_COMMIT, WS1, () -> save(WS1, "a.txt", "v2", "*"));
      Files.writeString(root(WS1).resolve(".git/index.lock"), "");
      Files.writeString(root(WS1).resolve(".git/refs/heads/main.lock"), "");

      saveOk(WS1, "a.txt", "v3");
      assertEquals("v3", read(WS1, "a.txt"));
      assertFalse(Files.exists(root(WS1).resolve(".git/index.lock")));
      assertFalse(Files.exists(root(WS1).resolve(".git/refs/heads/main.lock")));
      assertRecovered(WS1);
    }

    /**
     * The rollback restores the whole tree to HEAD, which is right only for what the interrupted mutation changed.
     * Anything else that changed while no server owned the volume blocks recovery and is never discarded; the record
     * is kept, and recovery is retried once it is resolved.
     */
    @Test
    void aRecoveryBlockedByOtherChangesKeepsItsRecordAndIsRetried() throws Exception {
      saveOk(WS1, "a.txt", "v1");
      crash(WorkspaceHistory.Checkpoint.BEFORE_STAGE, WS1, () -> save(WS1, "new.txt", "x", null));
      Files.writeString(root(WS1).resolve("stray.txt"), "out of band");

      final var result = save(WS1, "b.txt", "v1", null);
      assertFailure(result, 500, "WORKSPACE_REPOSITORY_INCONSISTENT");
      assertTrue(result.jsonResponse().asJsonObject().getString("message").contains("stray.txt"));
      assertTrue(Files.exists(WorkspaceHistory.recordFile(root(WS1))));
      assertEquals("out of band", read(WS1, "stray.txt"), "never deleted: it is outside the interrupted change");
      assertEquals("x", read(WS1, "new.txt"));

      Files.delete(root(WS1).resolve("stray.txt"));
      saveOk(WS1, "b.txt", "v1");
      assertFalse(Files.exists(root(WS1).resolve("new.txt")));
      assertRecovered(WS1);
    }

    @Test
    void anAmbiguousRecordFailsClosed() throws Exception {
      saveOk(WS1, "a.txt", "v1");
      crash(WorkspaceHistory.Checkpoint.BEFORE_STAGE, WS1, () -> save(WS1, "new.txt", "x", null));
      try (final var git = git(WS1)) { // HEAD is now neither the recorded commit nor one commit on top of it
        for (int i = 0; i < 2; i++) git.commit().setMessage("elsewhere").setAllowEmpty(true).setSign(false).call();
      }
      final var head = head(WS1);

      final var result = save(WS1, "b.txt", "v1", null);
      assertFailure(result, 500, "WORKSPACE_REPOSITORY_INCONSISTENT");
      assertTrue(result.jsonResponse().asJsonObject().getString("message").contains("interrupted"));
      assertEquals("x", read(WS1, "new.txt"), "left untouched");
      assertEquals(head, head(WS1));
      assertTrue(Files.exists(WorkspaceHistory.recordFile(root(WS1))));
    }

    @Test
    void anUnreadableRecordFailsClosed() throws Exception {
      saveOk(WS1, "a.txt", "v1");
      Files.writeString(WorkspaceHistory.recordFile(root(WS1)), "{\"version\":1,");
      assertFailure(save(WS1, "b.txt", "v1", null), 500, "WORKSPACE_REPOSITORY_INCONSISTENT");
      assertFalse(Files.exists(root(WS1).resolve("b.txt")));
    }
  }

  @Nested
  class Maintenance {
    @Test
    void noGitStartsAnAutomaticGcAndMaintenancePacksUnderTheLock() throws Exception {
      for (int i = 0; i < 20; i++) saveOk(WS1, "a.txt", "v" + i);
      try (final var git = git(WS1)) {
        assertEquals(0, git.getRepository().getConfig().getInt("gc", "auto", -1));
        assertEquals(0, git.getRepository().getConfig().getInt("gc", "autoPackLimit", -1));
      }
      assertTrue(history.statistics(WS1).looseObjects() > 20, history.statistics(WS1)::toString);

      final var after = history.maintain(WS1);
      assertEquals(0, after.looseObjects(), after::toString);
      assertTrue(after.packedObjects() > 20 && after.packFiles() == 1, after::toString);
      saveOk(WS1, "a.txt", "after maintenance");
      assertClean(WS1);
    }
  }

  @Test
  void everyFileSystemMutationRequiresTheLock() {
    assertThrows(IllegalStateException.class, () -> fs.createDirectory(WS1, Path.of("x")));
    assertThrows(IllegalStateException.class, () -> fs.deleteFile(WS1, Path.of("x")));
    assertThrows(IllegalStateException.class, () -> fs.saveFile(WS1, Path.of("x"), upload("x", "x"), USER));
    assertThrows(IllegalStateException.class, () -> fs.copyDirectory(WS1, Path.of("x"), WS2, Path.of("y"), USER));
    final var commits = new LinkedHashMap<Integer, Change>();
    commits.put(WS1, Change.of("only ws1", Path.of("a"), Path.of("b")));
    // Holding one workspace's lock does not authorize mutating another
    assertThrows(IllegalStateException.class, () -> history.mutate(commits, USER,
        () -> fs.moveDirectory(WS1, Path.of("a"), WS2, Path.of("b")), r -> true));
    assertFalse(Files.exists(root(WS1).resolve("x")));
  }
}
