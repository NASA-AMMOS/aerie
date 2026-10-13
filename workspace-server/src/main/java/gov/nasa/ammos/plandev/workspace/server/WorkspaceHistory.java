package gov.nasa.ammos.plandev.workspace.server;

import gov.nasa.ammos.plandev.workspace.server.exceptions.ReservedPathException;
import gov.nasa.ammos.plandev.workspace.server.exceptions.WorkspaceFileOpException;
import gov.nasa.ammos.plandev.workspace.server.postgres.RenderType;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonString;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

/**
 * The single owner of workspace mutations and of the Git history that records them.
 *
 * <p><b>Invariant.</b> workspace-server is the only writer of a workspace's files, and every path a successful
 * mutation changed is committed to the workspace's Git history before the request succeeds. Every mutation of
 * workspace-managed files (file and directory create, save, move, copy, delete, metadata edits, cross-workspace
 * operations) runs inside {@link #mutate}, which is told up front which path roots ({@link Change}) the operation may
 * change. The mutating methods of {@link WorkspaceFileSystemService} call {@link #requireInScope} for every path they
 * write, so a path that bypasses this class, or strays outside its declared scope, fails loudly.
 *
 * <p><b>Flow.</b> {@link #mutate} takes each involved workspace's lock (in ascending id order, so two cross-workspace
 * operations cannot deadlock; lock operations cannot nest) and runs cheap checks that read no working-tree files
 * (managed repository, branch {@code main} checked out, no Git operation in progress, valid runtime state, an index of
 * plain files whose tree is HEAD's tree). It then records the pre-operation HEAD and runtime state, makes the working
 * tree under the declared path roots match HEAD ({@linkplain #repairScope repairing} it if something outside PlanDev
 * changed it), atomically writes a {@linkplain #recordFile mutation record}, runs the mutation
 * (which performs its own validation, e.g. existence, readOnly and ETag checks, under the lock), stages the declared
 * path roots ({@code git add -A -- <roots>}), commits them if they differ from HEAD, verifies the new commit holds
 * exactly what was staged on top of the pre-operation HEAD, and clears the record. Normal mutations do not inspect or
 * hash working-tree files outside the declared roots; what remains proportional to the workspace is index work (Git
 * reads the whole index, writes it back when staging, and builds trees from it when checking and committing).
 *
 * <p><b>Drift and corruption.</b> Two kinds of damage are told apart. <i>Repository</i> problems (invalid runtime
 * state, detached HEAD or another branch, a Git operation in progress, an index that differs from HEAD, unsupported
 * tracked paths, an ambiguous mutation record) fail closed with {@link Kind#REPOSITORY_INCONSISTENT}. <i>Working-tree
 * drift</i> (files modified, deleted or added outside PlanDev, violating the sole-writer invariant) does not take the
 * workspace offline and never becomes history: it is left alone until a mutation declares a path under it, which first
 * preserves the unexpected bytes under {@code .seqdev/recovery/} and restores just that scope from HEAD. Full checks
 * ({@code git status}) run only at trust boundaries: adoption, crash recovery, the first mutation of each workspace by
 * this process, and {@link #verifyIntegrity}; they fail on repository problems and log drift. Normal mutations never
 * look for drift outside their declared roots, nor sweep it into a commit. Ignore rules are
 * fully controlled here: {@code core.excludesFile} points at {@code /dev/null}, {@code .git/info/exclude} contains
 * only {@code /.seqdev/}, and {@code .gitignore}/{@code .gitattributes} cannot exist in the tree
 * ({@link WorkspacePaths}). Empty directories are outside the definition: they are not representable in Git, and
 * creating or deleting one produces no commit.
 *
 * <p><b>Failure.</b> If the mutation throws, reports failure, or its history cannot be recorded, each workspace's
 * declared paths are restored from the pre-operation HEAD (files the operation created under them are removed, along
 * with any parent directories that leaves empty; changed and deleted files are rewritten), the index entries under
 * them are reset, the branch is moved back if the commit had landed, and {@code .seqdev/state.json} is restored. If
 * restoration itself fails, a {@link Kind#REPOSITORY_INCONSISTENT} error is raised and the mutation record is kept,
 * so the next mutation retries the recovery rather than trusting the workspace.
 *
 * <p><b>Crash.</b> A process can die between changing files and committing them. The mutation record
 * ({@code .seqdev/mutation.json}: pre-operation HEAD, declared paths, pre-operation runtime state) is written
 * atomically before anything else changes and removed only once the workspace is consistent again, so its presence
 * means "a mutation did not finish". It is unrelated to recovery incidents ({@link #recoveryDir}), which preserve
 * changes made outside PlanDev. This covers the process or container dying, not power loss: neither the record nor
 * Git's own files are fsynced. The next use of the workspace recovers it deterministically (see {@link #recover}): if HEAD is
 * still the recorded HEAD the change never committed and is rolled back as above; if HEAD is exactly one commit on
 * top of it, the commit landed and is kept. Either way a full repository check must pass before the record is
 * removed; anything else fails closed with {@link Kind#REPOSITORY_INCONSISTENT}.
 *
 * <p><b>Cross-workspace.</b> Two repositories cannot share a transaction. Commits are made in the order the caller
 * lists the workspaces (destination first for moves). If the first commit fails, every workspace is restored. If a
 * later commit fails (or the process dies before it), the workspaces already committed keep their change and the
 * remaining ones are restored, raising {@link Kind#PARTIALLY_APPLIED}: for a move this means it degraded to a copy,
 * which is preferable to losing data.
 *
 * <p><b>Adoption.</b> A workspace is <i>managed</i> once its repository config carries {@code plandev.managed=true},
 * which is written only after initialization or adoption has fully succeeded. New workspaces are initialized
 * ({@link #initializeNewWorkspace}) before their database row makes them visible. An existing unmanaged workspace
 * (pre-history, or a repository PlanDev did not finish adopting) is adopted on its first mutation: its tree must contain no reserved names and no
 * symbolic links; legacy sidecar metadata is migrated (resumably); a repository is created if missing; and the
 * current state is recorded by one baseline commit authored by {@link #systemIdent()} ("Initialize workspace
 * history" for a new repository, "Adopt workspace into history" for an existing one with uncommitted state). History
 * from before adoption is not reconstructed.
 *
 * <p><b>Maintenance.</b> Commits create loose objects. JGit only auto-GCs from fetch, push, receive, merge and rebase,
 * none of which this class uses, and {@link #configure} disables auto-GC for any other Git that opens the repository.
 * Packing is instead done by {@link #maintain}, under the workspace lock, either explicitly or in the background
 * after every {@link #MAINTENANCE_INTERVAL} commits to a workspace.
 *
 * <p><b>Deployment assumption.</b> Locks are in-process. This is correct only while a single workspace-server
 * instance owns the workspace volume and nothing else writes to it: every deployment definition runs one
 * workspace-server replica and mounts the workspace volume into no other service. Reads do not take the lock.
 */
public class WorkspaceHistory {
  private static final Logger logger = LoggerFactory.getLogger(WorkspaceHistory.class);

  static final String INIT_MESSAGE = "Initialize workspace history";
  static final String ADOPT_MESSAGE = "Adopt workspace into history";
  static final String BRANCH = "main";
  /** Commits to one workspace, made by this process, after which it is packed in the background. */
  static final int MAINTENANCE_INTERVAL = 1000;
  private static final String MANAGED_SECTION = "plandev";
  private static final String MANAGED_KEY = "managed";
  private static final int RECORD_VERSION = 1;
  private static final int INCIDENT_VERSION = 1;
  /** Recovery incidents kept per workspace; older ones are deleted when a new one is written. */
  static final int MAX_INCIDENTS = 20;
  private static final DateTimeFormatter INCIDENT_TIME =
      DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssSSS'Z'").withZone(ZoneOffset.UTC);

  public enum Kind {
    /** The history could not be recorded; the workspace was restored to its pre-operation state. */
    HISTORY_NOT_RECORDED("WORKSPACE_HISTORY_ERROR"),
    /** A cross-workspace mutation was applied to some workspaces only; see {@link PartialMutationException}. */
    PARTIALLY_APPLIED("WORKSPACE_MUTATION_PARTIAL"),
    /** The repository is not in a state this class can vouch for, and was not (or could not be) repaired. */
    REPOSITORY_INCONSISTENT("WORKSPACE_REPOSITORY_INCONSISTENT");

    public final String errorType;

    Kind(final String errorType) {
      this.errorType = errorType;
    }
  }

  public static class WorkspaceHistoryException extends RuntimeException {
    public final Kind kind;

    WorkspaceHistoryException(final Kind kind, final String message, final Throwable cause) {
      super(message, cause);
      this.kind = kind;
    }
  }

  /** A later commit of a cross-workspace mutation failed after earlier workspaces were committed. */
  public static final class PartialMutationException extends WorkspaceHistoryException {
    public final List<Integer> committedWorkspaceIds;
    public final int restoredWorkspaceId;

    PartialMutationException(final List<Integer> committed, final int restored, final Throwable cause) {
      super(Kind.PARTIALLY_APPLIED,
            "Workspace(s) %s were updated but workspace %d could not be and was restored".formatted(committed, restored),
            cause);
      this.committedWorkspaceIds = List.copyOf(committed);
      this.restoredWorkspaceId = restored;
    }
  }

  @FunctionalInterface
  public interface Mutation<T> {
    T apply() throws Exception;
  }

  /**
   * One workspace's part of a mutation: its commit message and the workspace-relative path roots the mutation may
   * change there. A root covers the path itself and everything beneath it, so a directory root covers its subtree.
   * Only these roots are staged, committed and, on failure, restored.
   */
  public record Change(String message, List<Path> paths) {
    public Change {
      paths = List.copyOf(paths);
    }

    public static Change of(final String message, final Path... paths) {
      return new Change(message, List.of(paths));
    }
  }

  /** Points at which {@link #checkpoint} is called, for simulating a process crash in tests. */
  enum Checkpoint { BEFORE_STAGE, BEFORE_COMMIT, BEFORE_RECORD_CLEARED }

  /** A file's most recent content change, derived from history. {@code baseline} marks the history's root commit. */
  public record LastEdit(String by, Instant at, boolean baseline) {}

  /** Object-store size, for deciding when (and checking that) maintenance is needed. */
  public record RepositoryStats(long looseObjects, long looseBytes, long packedObjects, long packBytes, long packFiles) {}

  private record Before(ObjectId head, byte[] state, Set<String> scope) {}

  private record Blob(ObjectId id, FileMode mode) {}

  private record LastEditIndex(ObjectId head, Map<String, LastEdit> edits) {}

  private final WorkspaceRoots roots;
  // ponytail: one lock per workspace id, never evicted; a few bytes per workspace ever touched by this process.
  private final Map<Integer, ReentrantLock> locks = new ConcurrentHashMap<>();
  private final Map<Integer, Set<String>> scopes = new ConcurrentHashMap<>();
  private final ThreadLocal<Boolean> inLockScope = ThreadLocal.withInitial(() -> false);
  private final Map<Path, LastEditIndex> lastEditCache = new ConcurrentHashMap<>();
  /** Workspaces this process has fully verified (and configured) since it started. */
  private final Set<Path> opened = ConcurrentHashMap.newKeySet();
  // ponytail: per-process commit counts; a restart resets them, which only delays the next background pack.
  private final Map<Integer, AtomicInteger> commitsSinceMaintenance = new ConcurrentHashMap<>();
  private final ExecutorService maintenanceExecutor = Executors.newSingleThreadExecutor(r -> {
    final var thread = new Thread(r, "workspace-maintenance");
    thread.setDaemon(true);
    return thread;
  });

  public WorkspaceHistory(final WorkspaceRoots roots) {
    this.roots = roots;
  }

  //region Mutation boundary
  /** Run a single-workspace mutation. See {@link #mutate(LinkedHashMap, String, Mutation, Predicate)}. */
  public <T> T mutate(
      final int workspaceId,
      final String userId,
      final Change change,
      final Mutation<T> mutation,
      final Predicate<T> succeeded) throws Exception
  {
    final var changes = new LinkedHashMap<Integer, Change>();
    changes.put(workspaceId, change);
    return mutate(changes, userId, mutation, succeeded);
  }

  /**
   * Run a mutation across one or more workspaces under their locks, then record it in each workspace's history.
   *
   * @param changes the involved workspaces, in commit order, each with its commit message and the paths it may change
   * @param userId the PlanDev user the commits are authored by
   * @param mutation performs validation and the filesystem change; may return a failure result instead of throwing
   * @param succeeded whether a returned result is a success; a failure result is rolled back and returned as-is
   * @return the mutation's result
   * @throws WorkspaceHistoryException if history could not be recorded (the change was rolled back), was recorded in
   *     only some workspaces, or the repository could not be restored
   * @throws ReservedPathException if a declared path names a reserved name or goes through a symbolic link
   * @throws Exception anything the mutation throws, after the workspaces have been restored
   */
  public <T> T mutate(
      final LinkedHashMap<Integer, Change> changes,
      final String userId,
      final Mutation<T> mutation,
      final Predicate<T> succeeded) throws Exception
  {
    return notNested(() -> mutateUnderLocks(changes, userId, mutation, succeeded));
  }

  private <T> T mutateUnderLocks(
      final LinkedHashMap<Integer, Change> changes,
      final String userId,
      final Mutation<T> mutation,
      final Predicate<T> succeeded) throws Exception
  {
    final var ids = List.copyOf(changes.keySet());
    final var lockOrder = ids.stream().sorted().map(this::lockFor).toList();
    lockOrder.forEach(ReentrantLock::lock);
    final var committedAny = new ArrayList<Integer>();
    try {
      final var rootsById = new LinkedHashMap<Integer, Path>();
      final var before = new HashMap<Integer, Before>();
      for (final var id : ids) {
        final var root = roots.workspaceRootPath(id).normalize();
        rootsById.put(id, root);
        ensureReady(root);
        before.put(id, capture(root, scopeKeys(root, changes.get(id).paths())));
        repairScope(id, root, before.get(id), changes.get(id).message(), userId);
      }
      writeRecords(rootsById, before);
      ids.forEach(id -> scopes.put(id, before.get(id).scope()));

      final T result;
      try {
        result = mutation.apply();
      } catch (Exception e) {
        rollbackAll(ids, rootsById, before, e);
        throw e;
      }
      if (!succeeded.test(result)) {
        rollbackAll(ids, rootsById, before, null);
        return result;
      }

      final var recorded = new ArrayList<Integer>();
      for (final var id : ids) {
        final var root = rootsById.get(id);
        final var b = before.get(id);
        try (final var git = open(root)) {
          final var repo = git.getRepository();
          checkpoint(Checkpoint.BEFORE_STAGE, root);
          final var staged = stage(git, root, b.scope());
          checkpoint(Checkpoint.BEFORE_COMMIT, root);
          final boolean changed;
          try (final var walk = new RevWalk(repo)) {
            changed = !treeEntries(repo, walk.parseCommit(b.head()).getTree(), b.scope()).equals(staged);
          }
          if (changed) commit(git, changes.get(id).message(), userIdent(userId));
          verifyCommitted(repo, b, staged);
          checkpoint(Checkpoint.BEFORE_RECORD_CLEARED, root);
          Files.delete(recordFile(root));
          recorded.add(id);
          if (changed) committedAny.add(id);
        } catch (Exception e) {
          logger.error("Recording history for workspace {} failed; restoring", id, e);
          rollbackAll(ids.subList(recorded.size(), ids.size()), rootsById, before, e);
          if (recorded.isEmpty()) {
            throw new WorkspaceHistoryException(
                Kind.HISTORY_NOT_RECORDED,
                "The change could not be recorded in workspace history and was rolled back.",
                e);
          }
          throw new PartialMutationException(recorded, id, e);
        }
      }
      return result;
    } finally {
      ids.forEach(scopes::remove);
      lockOrder.reversed().forEach(ReentrantLock::unlock);
      committedAny.forEach(this::countCommit);
    }
  }

  /** Run an action under a workspace's lock without recording history (e.g. deleting the whole workspace). */
  public <T> T withLock(final int workspaceId, final Mutation<T> action) throws Exception {
    final var root = roots.workspaceRootPath(workspaceId).normalize(); // before the action may delete the record
    return locked(workspaceId, () -> {
      final var result = action.apply();
      lastEditCache.remove(root);
      opened.remove(root);
      return result;
    });
  }

  private <T> T locked(final int workspaceId, final Mutation<T> action) throws Exception {
    return notNested(() -> {
      final var lock = lockFor(workspaceId);
      lock.lock();
      try {
        return action.apply();
      } finally {
        lock.unlock();
      }
    });
  }

  /**
   * Workspace lock operations cannot nest: an inner one could take a lock out of the global order (deadlock), and an
   * inner mutation of the same workspace would replace the outer one's declared scope. Callers declare every affected
   * workspace in one outer mutation.
   */
  private <T> T notNested(final Mutation<T> action) throws Exception {
    if (inLockScope.get()) {
      throw new IllegalStateException("Workspace lock operations cannot be nested; declare every workspace in one mutation.");
    }
    inLockScope.set(true);
    try {
      return action.apply();
    } finally {
      inLockScope.remove();
    }
  }

  /** Throw unless the current thread holds every given workspace's mutation lock. */
  public void requireLocked(final int... workspaceIds) {
    for (final var id : workspaceIds) {
      final var lock = locks.get(id);
      if (lock == null || !lock.isHeldByCurrentThread()) {
        throw new IllegalStateException(
            "Workspace %d was mutated outside WorkspaceHistory.mutate; every workspace mutation must go through it."
                .formatted(id));
      }
    }
  }

  /**
   * Throw unless the current thread is inside a mutation of the workspace whose declared paths cover every given
   * absolute path. A path outside them would change the workspace without being committed, or restored on failure.
   */
  public void requireInScope(final int workspaceId, final Path root, final Path... paths) {
    requireLocked(workspaceId);
    final var scope = scopes.getOrDefault(workspaceId, Set.of());
    for (final var path : paths) {
      final var key = WorkspacePaths.key(root, path);
      if (scope.stream().noneMatch(s -> isAtOrUnder(key, s))) {
        throw new IllegalStateException(
            "Workspace %d: '%s' is outside the paths this mutation declared (%s).".formatted(workspaceId, key, scope));
      }
    }
  }

  private ReentrantLock lockFor(final int workspaceId) {
    return locks.computeIfAbsent(workspaceId, k -> new ReentrantLock());
  }

  /** The Git commit step. Overridable only so tests can inject a deterministic failure. */
  protected void commit(final Git git, final String message, final PersonIdent author) throws GitAPIException {
    git.commit()
       .setMessage(message)
       .setAuthor(author)
       .setCommitter(systemIdent())
       .setAllowEmpty(true)
       .setSign(false)
       .setNoVerify(true)
       .call();
  }

  /**
   * No-op. Tests override it to stop a mutation exactly as a process crash would: by throwing an {@link Error}, which
   * nothing in {@link #mutate} catches, so no rollback runs and the on-disk state is left as it was at that point.
   */
  protected void checkpoint(final Checkpoint point, final Path root) {}

  /**
   * Normalize declared paths to workspace-relative keys, rejecting what no mutation may touch: the root itself (it
   * owns the history and runtime state), anything outside it, reserved names, and paths through a symbolic link.
   */
  private static Set<String> scopeKeys(final Path root, final List<Path> paths) throws WorkspaceFileOpException {
    final var keys = new TreeSet<String>();
    for (final var path : paths) {
      if (path.isAbsolute()) throw new SecurityException("Absolute file paths not allowed");
      final var resolved = root.resolve(path).normalize();
      if (!resolved.startsWith(root)) throw new SecurityException("Path traversal attempt detected");
      if (resolved.equals(root)) throw new WorkspaceFileOpException("Cannot modify the workspace root directory.");
      final var reserved = WorkspacePaths.firstReservedSegment(root.relativize(resolved));
      if (reserved != null) throw new ReservedPathException(path, reserved);
      for (var p = resolved; !p.equals(root); p = p.getParent()) {
        if (Files.isSymbolicLink(p)) throw ReservedPathException.symbolicLink(path, p.getFileName().toString());
      }
      keys.add(WorkspacePaths.key(root, resolved));
    }
    // A root already covered by another adds nothing
    final var all = Set.copyOf(keys);
    keys.removeIf(k -> all.stream().anyMatch(other -> !other.equals(k) && isAtOrUnder(k, other)));
    return keys;
  }

  private static boolean isAtOrUnder(final String key, final String root) {
    return key.equals(root) || key.startsWith(root + "/");
  }
  //endregion

  //region Opening, adoption and trust
  /**
   * Initialize a newly created, empty directory before its database row makes it visible as a workspace. No lock is
   * needed: no other request can resolve the directory yet. Existing workspaces are still adopted on first mutation.
   */
  void initializeNewWorkspace(final Path root) throws IOException, GitAPIException {
    final var normalizedRoot = root.normalize();
    if (!Files.isDirectory(normalizedRoot)) {
      throw new IOException("New workspace directory " + normalizedRoot + " is missing.");
    }
    try (final var entries = Files.list(normalizedRoot)) {
      if (entries.findAny().isPresent()) {
        throw new IOException("New workspace directory " + normalizedRoot + " is not empty.");
      }
    }
    adopt(normalizedRoot);
  }

  /**
   * Bring a workspace to "managed and usable": adopt it if it is not managed yet, recover an interrupted mutation,
   * or fully verify it the first time this process uses it; then run the cheap per-mutation checks. Caller holds the
   * lock.
   */
  private void ensureReady(final Path root) throws IOException, GitAPIException {
    if (!Files.isDirectory(root)) {
      throw inconsistent("Workspace directory " + root + " is missing.");
    }
    if (!isManaged(root)) {
      adopt(root);
    } else if (Files.exists(recordFile(root), LinkOption.NOFOLLOW_LINKS)) {
      recover(root);
    } else if (!opened.contains(root)) {
      checkIntegrity(root);
    }
    requireUsable(root);
    opened.add(root);
  }

  /**
   * The checks run before every mutation of a managed workspace. None of them reads working-tree files; checking the
   * index walks it in memory, so that part grows with the number of tracked files.
   */
  private static void requireUsable(final Path root) throws IOException {
    requireValidRuntimeState(root, true);
    try (final var git = open(root)) {
      final var repo = git.getRepository();
      requireExpectedGitState(repo, false);
      requireIndexMatchesHead(repo);
    }
    WorkspacePaths.removeStaleTempFiles(root); // only once trusted: a rejected workspace is left exactly as found
  }

  /**
   * Fail closed unless a managed workspace's repository is as this class left it: valid runtime state, the expected
   * Git state, and an index equal to HEAD. Working-tree files that differ from HEAD are drift, not corruption: they
   * are logged, left untouched, and returned; a mutation that declares them repairs them first ({@link #repairScope}).
   * Walks the whole working tree, so it runs only at trust boundaries (see the class Javadoc).
   *
   * @return the working-tree paths that differ from HEAD
   */
  private static Set<String> checkIntegrity(final Path root) throws IOException, GitAPIException {
    requireValidRuntimeState(root, true);
    try (final var git = open(root)) {
      final var repo = git.getRepository();
      requireExpectedGitState(repo, false);
      configure(repo);
      requireValidIndex(repo);
      final var status = git.status().call();
      final var staged = new TreeSet<String>();
      staged.addAll(status.getAdded());
      staged.addAll(status.getChanged());
      staged.addAll(status.getRemoved());
      staged.addAll(status.getConflicting());
      if (!staged.isEmpty()) {
        throw inconsistent("Workspace at %s has staged changes that are not in its history: %s. They were left untouched."
                               .formatted(root, summarize(staged)));
      }
      final var drift = dirtyPaths(status);
      if (!drift.isEmpty()) {
        logger.warn("Workspace at {} has files modified outside managed history, left untouched until a change touches "
                    + "them: {}", root, summarize(drift));
      }
      return drift;
    }
  }

  /**
   * Explicitly check a workspace's integrity, recovering an interrupted mutation first if there is one. Walks the
   * whole working tree. Unmanaged workspaces are reported, not adopted.
   *
   * @return working-tree paths that differ from HEAD (drift: reported, not repaired)
   * @throws WorkspaceHistoryException ({@link Kind#REPOSITORY_INCONSISTENT}) naming what is wrong with the repository
   */
  public Set<String> verifyIntegrity(final int workspaceId) throws Exception {
    final var root = roots.workspaceRootPath(workspaceId).normalize();
    return locked(workspaceId, () -> {
      if (!isManaged(root)) throw inconsistent("Workspace at %s is not under history yet.".formatted(root));
      final var drift = Files.exists(recordFile(root), LinkOption.NOFOLLOW_LINKS) ? recover(root) : checkIntegrity(root);
      requireUsable(root);
      opened.add(root);
      return drift;
    });
  }

  /**
   * Put a workspace under history. Every step is idempotent and the managed marker is written last, so an adoption
   * interrupted at any point is simply redone by the next mutation. The only whole-tree staging there is.
   */
  private void adopt(final Path root) throws IOException, GitAPIException {
    // Validate everything before writing anything
    requireValidRuntimeState(root, false);
    requireAdoptable(root);
    final var hasRepo = Files.isDirectory(root.resolve(WorkspacePaths.GIT_DIR));
    if (hasRepo) {
      try (final var git = open(root)) {
        requireExpectedGitState(git.getRepository(), true);
      }
    }
    WorkspacePaths.removeStaleTempFiles(root);
    WorkspaceState.migrateFromSidecars(root); // validates every sidecar before writing anything
    if (!hasRepo) Git.init().setDirectory(root.toFile()).setInitialBranch(BRANCH).call().close();

    try (final var git = open(root)) {
      final var repo = git.getRepository();
      configure(repo);
      final var unborn = repo.resolve(Constants.HEAD) == null;
      if (stageAll(git) || unborn) {
        requireValidIndex(repo);
        commit(git, unborn ? INIT_MESSAGE : ADOPT_MESSAGE, systemIdent());
      }
      requireValidIndex(repo);
      requireClean(git, root);

      final var cfg = repo.getConfig();
      cfg.setBoolean(MANAGED_SECTION, null, MANAGED_KEY, true);
      cfg.save();
      logger.info("Workspace at {} is now under history", root);
    }
  }

  /**
   * Whether a workspace has been fully initialized or adopted (see the class Javadoc). Never discovers a parent
   * repository.
   */
  boolean isManaged(final Path root) throws IOException {
    if (!Files.isDirectory(root.resolve(WorkspacePaths.GIT_DIR), LinkOption.NOFOLLOW_LINKS)) return false;
    try (final var git = open(root)) {
      return git.getRepository().getConfig().getBoolean(MANAGED_SECTION, null, MANAGED_KEY, false);
    }
  }

  /**
   * {@code .seqdev} must be a real directory holding a regular {@code state.json} in the current schema. Required for
   * a managed workspace; for one being adopted, whatever already exists must be valid. Never repaired automatically:
   * runtime state holds locks, so "unreadable" must not turn into "unlocked".
   */
  private static void requireValidRuntimeState(final Path root, final boolean required) {
    final var dir = root.resolve(WorkspacePaths.STATE_DIR);
    final var file = WorkspaceState.file(root);
    final String problem;
    if (!Files.exists(dir, LinkOption.NOFOLLOW_LINKS)) {
      problem = required ? dir + " is missing" : null;
    } else if (!Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) {
      problem = dir + " is not a directory";
    } else if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
      problem = required ? file + " is missing" : null;
    } else {
      String loadProblem = null;
      try {
        WorkspaceState.load(root);
      } catch (IOException e) {
        loadProblem = e.getMessage();
      }
      problem = loadProblem;
    }
    if (problem != null) {
      throw inconsistent("Workspace at %s has invalid runtime state: %s. It was left untouched.".formatted(root, problem));
    }
  }

  /**
   * PlanDev uses a deliberately small subset of Git: one branch, always checked out, no merges or other multi-step
   * operations. Refuse to touch a repository outside that subset rather than switching branches or aborting anything.
   */
  private static void requireExpectedGitState(final Repository repo, final boolean allowUnborn) throws IOException {
    final var root = repo.getWorkTree();
    final var state = repo.getRepositoryState();
    if (state != RepositoryState.SAFE) {
      throw inconsistent("Workspace at %s has a Git operation in progress (%s); finish or abort it outside PlanDev."
                             .formatted(root, state.getDescription()));
    }
    final var head = repo.exactRef(Constants.HEAD);
    if (head == null || !head.isSymbolic()) {
      throw inconsistent("Workspace at %s has a detached HEAD; check out branch '%s' outside PlanDev.".formatted(root, BRANCH));
    }
    if (!head.getTarget().getName().equals(Constants.R_HEADS + BRANCH)) {
      throw inconsistent("Workspace at %s has '%s' checked out; PlanDev only uses branch '%s'."
                             .formatted(root, Repository.shortenRefName(head.getTarget().getName()), BRANCH));
    }
    if (!allowUnborn && head.getObjectId() == null) {
      throw inconsistent("Workspace at %s has no commits on branch '%s'.".formatted(root, BRANCH));
    }
  }

  /**
   * Make every repository behavior that could change file bytes, what is tracked, or when objects are repacked
   * explicit at repository level, rather than inheriting it from the server's $HOME or system Git config. Applied at
   * adoption and at each process's first use of a workspace.
   */
  private static void configure(final Repository repo) throws IOException {
    final var cfg = repo.getConfig();
    cfg.setBoolean("core", null, "autocrlf", false);
    cfg.setBoolean("core", null, "safecrlf", false);
    cfg.setString("core", null, "excludesFile", "/dev/null");
    cfg.setString("core", null, "attributesFile", "/dev/null");
    cfg.setString("core", null, "hooksPath", "/dev/null");
    cfg.setBoolean("commit", null, "gpgSign", false);
    cfg.setBoolean("tag", null, "gpgSign", false);
    // No Git may start an unlocked background GC; packing is done by maintain(), under the workspace lock
    cfg.setInt("gc", null, "auto", 0);
    cfg.setInt("gc", null, "autoPackLimit", 0);
    cfg.setBoolean("gc", null, "autoDetach", false);
    cfg.save();

    final var info = repo.getDirectory().toPath().resolve("info");
    Files.createDirectories(info);
    Files.writeString(info.resolve("exclude"), "/" + WorkspacePaths.STATE_DIR + "/\n", StandardCharsets.UTF_8);
    Files.deleteIfExists(info.resolve("attributes"));
  }

  /**
   * Fail closed if the tree contains reserved names that would make Git skip or rewrite user files, or symbolic links
   * (unsupported workspace content: one could point into {@code .git} or outside the workspace). Only possible for
   * workspaces created or edited outside the file API, which rejects both. Links are never followed.
   */
  private static void requireAdoptable(final Path root) throws IOException {
    final var found = new ArrayList<String>();
    Files.walkFileTree(root, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
        if (dir.equals(root)) return FileVisitResult.CONTINUE;
        final var name = dir.getFileName().toString();
        if (dir.getParent().equals(root) && (name.equals(WorkspacePaths.GIT_DIR) || name.equals(WorkspacePaths.STATE_DIR))) {
          return FileVisitResult.SKIP_SUBTREE;
        }
        if (WorkspacePaths.isReservedName(name)) found.add(WorkspacePaths.key(root, dir));
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
        if (attrs.isSymbolicLink()) {
          found.add(WorkspacePaths.key(root, file) + " (symbolic link)");
        } else if (WorkspacePaths.isReservedName(file.getFileName().toString())) {
          found.add(WorkspacePaths.key(root, file));
        }
        return FileVisitResult.CONTINUE;
      }
    });
    if (!found.isEmpty()) {
      throw inconsistent("Workspace at %s contains unsupported paths %s; remove them before it can be modified."
                             .formatted(root, summarize(found)));
    }
  }

  /** The index (== HEAD once clean) holds only regular files outside reserved names: no symlinks or submodules. */
  private static void requireValidIndex(final Repository repo) throws IOException {
    requireValidIndex(repo, repo.readDirCache());
  }

  private static void requireValidIndex(final Repository repo, final DirCache index) {
    final var bad = new ArrayList<String>();
    for (int i = 0; i < index.getEntryCount(); i++) {
      final var entry = index.getEntry(i);
      final var path = entry.getPathString();
      final var mode = entry.getRawMode();
      if (!FileMode.REGULAR_FILE.equals(mode) && !FileMode.EXECUTABLE_FILE.equals(mode)) {
        bad.add(path + " (" + entry.getFileMode() + ")");
      } else if (Arrays.stream(path.split("/")).anyMatch(WorkspacePaths::isReservedName)) {
        bad.add(path);
      }
    }
    if (!bad.isEmpty()) {
      throw inconsistent("Workspace at %s tracks unsupported paths %s.".formatted(repo.getWorkTree(), summarize(bad)));
    }
  }

  /**
   * The index holds only plain files and describes exactly HEAD's tree, so committing it can only ever record what a
   * mutation staged. Computed in memory from the index; the working tree is not read. The tree ids are computed
   * without being stored, so the DirCache used here is never written back.
   */
  private static void requireIndexMatchesHead(final Repository repo) throws IOException {
    final var head = repo.resolve(Constants.HEAD + "^{tree}");
    final var dirCache = repo.readDirCache();
    requireValidIndex(repo, dirCache);
    final ObjectId index;
    try (final var formatter = new ObjectInserter.Formatter() {
      @Override
      public ObjectId insert(final int type, final byte[] data, final int off, final int len) {
        return idFor(type, data, off, len); // hash only; nothing is written to the object store
      }
    }) {
      index = dirCache.writeTree(formatter);
    }
    if (!index.equals(head)) {
      throw inconsistent("Workspace at %s has staged changes that are not in its history.".formatted(repo.getWorkTree()));
    }
  }

  static WorkspaceHistoryException inconsistent(final String message) {
    return new WorkspaceHistoryException(Kind.REPOSITORY_INCONSISTENT, message, null);
  }

  static String summarize(final Collection<String> paths) {
    final var shown = paths.stream().limit(20).toList();
    return paths.size() > shown.size() ? shown + " and " + (paths.size() - shown.size()) + " more" : shown.toString();
  }
  //endregion

  //region Scoped staging and verification
  private static Git open(final Path root) throws IOException {
    // Opens <root>/.git only; never discovers a repository in a parent directory. Closing the Git closes the repo.
    return Git.open(root.toFile());
  }

  /**
   * Regular files at or under a workspace-relative path root, keyed by path. Fails closed on anything a workspace may
   * not contain (symbolic links, anywhere from the workspace root down; reserved names; special files): such a path
   * is never followed, staged or deleted.
   */
  private static Map<String, Path> filesUnder(final Path root, final String scopeKey) throws IOException {
    final var start = root.resolve(scopeKey);
    final var found = new TreeMap<String, Path>();
    final var bad = new ArrayList<String>();
    for (var p = start; !p.equals(root); p = p.getParent()) {
      if (Files.isSymbolicLink(p)) bad.add(WorkspacePaths.key(root, p) + " (symbolic link)");
    }
    if (bad.isEmpty() && Files.exists(start, LinkOption.NOFOLLOW_LINKS)) {
      Files.walkFileTree(start, new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
          if (WorkspacePaths.isReservedName(dir.getFileName().toString())) {
            bad.add(WorkspacePaths.key(root, dir));
            return FileVisitResult.SKIP_SUBTREE;
          }
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
          final var key = WorkspacePaths.key(root, file);
          if (attrs.isSymbolicLink()) bad.add(key + " (symbolic link)");
          else if (!attrs.isRegularFile() || WorkspacePaths.isReservedName(file.getFileName().toString())) bad.add(key);
          else found.put(key, file);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
          throw exc;
        }
      });
    }
    if (!bad.isEmpty()) {
      throw inconsistent("Workspace at %s contains unsupported paths %s where a change was being recorded."
                             .formatted(root, summarize(bad)));
    }
    return found;
  }

  private static FileMode modeOf(final Repository repo, final Path file) {
    final var fs = repo.getFS();
    return fs.supportsExecute() && fs.canExecute(file.toFile()) ? FileMode.EXECUTABLE_FILE : FileMode.REGULAR_FILE;
  }

  /**
   * {@code git add -A -- <scope>}: make the index match the working tree under the scope, and only there. Paths Git
   * must not see (symbolic links, reserved names, special files) are rejected first. Returns the staged entries.
   */
  private static Map<String, Blob> stage(final Git git, final Path root, final Set<String> scope)
  throws IOException, GitAPIException
  {
    for (final var key : scope) filesUnder(root, key);
    if (!scope.isEmpty()) {
      final var add = git.add();
      final var update = git.add().setUpdate(true); // the only way AddCommand stages a tracked file's deletion
      scope.forEach(key -> {
        add.addFilepattern(key);
        update.addFilepattern(key);
      });
      add.call();
      update.call();
    }
    return indexEntries(git.getRepository(), scope);
  }

  /** The index entries under the scope. */
  private static Map<String, Blob> indexEntries(final Repository repo, final Set<String> scope) throws IOException {
    final var entries = new TreeMap<String, Blob>();
    final var index = repo.readDirCache();
    for (final var key : scope) {
      final var exact = index.getEntry(key);
      if (exact != null) entries.put(key, new Blob(exact.getObjectId(), exact.getFileMode()));
      for (final var e : index.getEntriesWithin(key)) entries.put(e.getPathString(), new Blob(e.getObjectId(), e.getFileMode()));
    }
    return entries;
  }

  /**
   * Make the working tree under a mutation's scope match HEAD before the mutation writes anything. If it does not,
   * something outside PlanDev changed files PlanDev is about to write (the sole-writer invariant was violated). Those
   * bytes must neither become history nor be destroyed, and the workspace must stay usable: they are first preserved
   * in a {@linkplain #recoveryDir recovery incident}, then only the scope is restored from HEAD and checked again.
   * Runtime state is not touched; this repairs versioned files, it does not undo a mutation. If preserving fails,
   * nothing is changed; if restoring fails, the incident is kept. Either way the request fails.
   */
  private static void repairScope(
      final int workspaceId,
      final Path root,
      final Before before,
      final String operation,
      final String userId) throws IOException, GitAPIException
  {
    try (final var git = open(root); final var walk = new RevWalk(git.getRepository())) {
      final var repo = git.getRepository();
      final var expected = treeEntries(repo, walk.parseCommit(before.head()).getTree(), before.scope());
      final var drift = scopeDrift(repo, root, expected, before.scope());
      if (drift.isEmpty()) return;
      final var incident = preserve(workspaceId, root, before, expected, drift, operation, userId);
      try {
        restoreScope(git, root, expected, before.scope());
        final var remaining = scopeDrift(repo, root, expected, before.scope());
        if (!remaining.isEmpty()) throw new IOException("still differs from history: " + summarize(remaining.keySet()));
      } catch (IOException | GitAPIException | WorkspaceHistoryException e) {
        logger.error("Workspace {}: could not restore {} from history after preserving unexpected changes; the request "
                     + "was refused. incident={} head={} operation=\"{}\" recovery={}",
                     workspaceId, summarize(drift.keySet()), incident.getFileName(), before.head().name(), operation,
                     incident, e);
        throw new WorkspaceHistoryException(
            Kind.REPOSITORY_INCONSISTENT,
            "Workspace at %s has changes that are not in its history: %s. They were preserved in %s, but could not be "
                .formatted(root, summarize(drift.keySet()), incident)
            + "restored from history.",
            e);
      }
      logger.warn("Workspace {}: files were modified outside managed history; PlanDev preserved them for investigation, "
                  + "restored them from history and continued the request. incident={} head={} paths={} "
                  + "operation=\"{}\" recovery={}",
                  workspaceId, incident.getFileName(), before.head().name(), summarize(drift.keySet()), operation,
                  incident);
    }
  }

  /**
   * How the working tree under the scope differs from HEAD's entries there, by path: {@code modified},
   * {@code mode-changed}, {@code deleted} or {@code untracked}. Only the scope is read and hashed; a path absent from
   * both HEAD and disk is fine.
   */
  private static Map<String, String> scopeDrift(
      final Repository repo,
      final Path root,
      final Map<String, Blob> expected,
      final Set<String> scope) throws IOException
  {
    final var drift = new TreeMap<String, String>();
    expected.keySet().forEach(key -> drift.put(key, "deleted")); // until found on disk
    for (final var key : scope) {
      for (final var file : filesUnder(root, key).entrySet()) {
        final var blob = expected.get(file.getKey());
        if (blob == null) drift.put(file.getKey(), "untracked");
        else if (matches(repo, file.getValue(), blob)) drift.remove(file.getKey());
        else drift.put(file.getKey(), contentId(file.getValue()).equals(blob.id()) ? "mode-changed" : "modified");
      }
    }
    return drift;
  }

  /**
   * Where unexpected working-tree bytes are preserved before a scope is repaired: one directory per incident, named
   * by time and a UUID, holding {@code incident.json} and the files under {@code files/} at their workspace-relative
   * paths. Runtime state: ignored by Git and reserved from the file API, like the mutation record.
   */
  static Path recoveryDir(final Path root) {
    return root.resolve(WorkspacePaths.STATE_DIR).resolve("recovery");
  }

  /**
   * Copy the drifted files into a new incident and verify the copies, then write its metadata atomically. Reads the
   * workspace only; on failure the partial incident is removed and the request fails with the workspace untouched.
   */
  private static Path preserve(
      final int workspaceId,
      final Path root,
      final Before before,
      final Map<String, Blob> expected,
      final Map<String, String> drift,
      final String operation,
      final String userId)
  {
    final var now = Instant.now();
    final var id = INCIDENT_TIME.format(now) + "-" + UUID.randomUUID();
    final var dir = recoveryDir(root).resolve(id);
    try {
      final var parent = recoveryDir(root);
      if (Files.exists(parent, LinkOption.NOFOLLOW_LINKS) && !Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)) {
        throw new IOException(parent + " is not a directory");
      }
      Files.createDirectories(parent);
      Files.createDirectory(dir); // never an earlier incident's
      final var paths = Json.createArrayBuilder();
      for (final var e : drift.entrySet()) {
        final var preserved = !e.getValue().equals("deleted");
        if (preserved) {
          final var source = root.resolve(e.getKey());
          final var copy = dir.resolve("files").resolve(e.getKey());
          Files.createDirectories(copy.getParent());
          Files.copy(source, copy, LinkOption.NOFOLLOW_LINKS);
          if (Files.mismatch(source, copy) != -1) throw new IOException("the preserved copy of " + e.getKey() + " differs");
        }
        final var path = Json.createObjectBuilder().add("path", e.getKey()).add("change", e.getValue()).add("preserved", preserved);
        final var blob = expected.get(e.getKey());
        if (blob != null) path.add("expectedBlob", blob.id().name()).add("expectedMode", blob.mode().toString());
        paths.add(path);
      }
      final var scope = Json.createArrayBuilder();
      before.scope().forEach(scope::add);
      final var json = Json.createObjectBuilder()
                           .add("version", INCIDENT_VERSION)
                           .add("id", id)
                           .add("workspaceId", workspaceId)
                           .add("createdAt", now.toString())
                           .add("head", before.head().name())
                           .add("operation", operation)
                           .add("scope", scope)
                           .add("paths", paths);
      if (userId != null) json.add("userId", userId);
      WorkspacePaths.writeAtomically(root, dir.resolve("incident.json"), json.build().toString().getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      try {
        deleteTree(dir);
      } catch (IOException suppressed) {
        e.addSuppressed(suppressed);
      }
      logger.error("Workspace {}: could not preserve files modified outside managed history; the request was refused "
                   + "and they were left untouched. head={} paths={} operation=\"{}\"",
                   workspaceId, before.head().name(), summarize(drift.keySet()), operation, e);
      throw new WorkspaceHistoryException(
          Kind.REPOSITORY_INCONSISTENT,
          "Workspace at %s has changes that are not in its history: %s. They could not be preserved, so they were left untouched."
              .formatted(root, summarize(drift.keySet())),
          e);
    }
    pruneIncidents(root);
    return dir;
  }

  /** Keep the newest {@link #MAX_INCIDENTS} incidents, so a misbehaving external writer cannot fill the volume. */
  private static void pruneIncidents(final Path root) {
    try (final var entries = Files.list(recoveryDir(root))) {
      final var newestFirst = entries.filter(p -> Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS))
                                     .sorted(Comparator.reverseOrder()) // names start with the time
                                     .toList();
      for (final var old : newestFirst.subList(Math.min(MAX_INCIDENTS, newestFirst.size()), newestFirst.size())) {
        deleteTree(old);
      }
    } catch (IOException e) {
      logger.warn("Could not prune recovery incidents in {}", recoveryDir(root), e);
    }
  }

  /** Delete a directory tree without following symbolic links. */
  private static void deleteTree(final Path dir) throws IOException {
    if (!Files.exists(dir, LinkOption.NOFOLLOW_LINKS)) return;
    try (final var paths = Files.walk(dir)) {
      for (final var p : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
    }
  }

  /** The blobs a tree holds under the scope. Only the scope's subtrees are read. */
  private static Map<String, Blob> treeEntries(final Repository repo, final RevTree tree, final Set<String> scope)
  throws IOException
  {
    final var entries = new TreeMap<String, Blob>();
    if (scope.isEmpty()) return entries;
    try (final var walk = new TreeWalk(repo)) {
      walk.addTree(tree);
      walk.setRecursive(true);
      walk.setFilter(PathFilterGroup.createFromStrings(scope));
      while (walk.next()) entries.put(walk.getPathString(), new Blob(walk.getObjectId(0), walk.getFileMode(0)));
    }
    return entries;
  }

  /**
   * HEAD is the pre-operation commit or exactly one commit on top of it, and it records exactly what was staged under
   * the scope; the index still describes HEAD, so nothing outside the scope changed either.
   */
  private static void verifyCommitted(final Repository repo, final Before before, final Map<String, Blob> staged)
  throws IOException
  {
    try (final var walk = new RevWalk(repo)) {
      final var head = walk.parseCommit(repo.resolve(Constants.HEAD));
      final var onTop = head.getParentCount() == 1 && head.getParent(0).equals(before.head());
      if (!head.equals(before.head()) && !onTop) {
        throw inconsistent("Workspace at %s: HEAD moved unexpectedly during a change.".formatted(repo.getWorkTree()));
      }
      if (!treeEntries(repo, head.getTree(), before.scope()).equals(staged)) {
        throw inconsistent("Workspace at %s: the commit does not record the change.".formatted(repo.getWorkTree()));
      }
    }
    requireIndexMatchesHead(repo);
  }

  /** {@code git add -A}, for adoption only. Returns whether anything is staged relative to HEAD. */
  private static boolean stageAll(final Git git) throws GitAPIException {
    git.add().addFilepattern(".").call();
    git.add().addFilepattern(".").setUpdate(true).call();
    final var status = git.status().call();
    return !(status.getAdded().isEmpty() && status.getChanged().isEmpty() && status.getRemoved().isEmpty());
  }

  /** Every path that makes the repository not clean, per the definition in the class Javadoc. */
  static Set<String> dirtyPaths(final Status status) {
    final var dirty = new TreeSet<String>();
    dirty.addAll(status.getAdded());
    dirty.addAll(status.getChanged());
    dirty.addAll(status.getRemoved());
    dirty.addAll(status.getMissing());
    dirty.addAll(status.getModified());
    dirty.addAll(status.getConflicting());
    // Untracked files are listed individually; untracked *folders* add nothing beyond them except empty
    // directories, which are deliberately outside the definition.
    dirty.addAll(status.getUntracked());
    for (final var ignored : status.getIgnoredNotInIndex()) {
      if (!ignored.equals(WorkspacePaths.STATE_DIR) && !ignored.startsWith(WorkspacePaths.STATE_DIR + "/")) {
        dirty.add(ignored);
      }
    }
    return dirty;
  }

  private static void requireClean(final Git git, final Path root) throws GitAPIException {
    final var dirty = dirtyPaths(git.status().call());
    if (!dirty.isEmpty()) throw inconsistent("Workspace at %s is not clean: %s".formatted(root, summarize(dirty)));
  }

  static PersonIdent systemIdent() {
    return new PersonIdent("PlanDev", "");
  }

  private static PersonIdent userIdent(final String userId) {
    return userId == null || userId.isBlank() ? systemIdent() : new PersonIdent(userId, "");
  }
  //endregion

  //region Mutation records, rollback and recovery
  /** Where an unfinished mutation is recorded. Runtime state: never versioned, reserved from the file API. */
  static Path recordFile(final Path root) {
    return root.resolve(WorkspacePaths.STATE_DIR).resolve("mutation.json");
  }

  private static Before capture(final Path root, final Set<String> scope) throws IOException {
    try (final var git = open(root)) {
      final var head = git.getRepository().resolve(Constants.HEAD);
      return new Before(head, Files.readAllBytes(WorkspaceState.file(root)), scope);
    }
  }

  /** Persist every workspace's record before anything changes; if one cannot be written, none is left behind. */
  private static void writeRecords(final Map<Integer, Path> rootsById, final Map<Integer, Before> before) throws IOException {
    final var written = new ArrayList<Path>();
    try {
      for (final var e : rootsById.entrySet()) {
        final var b = before.get(e.getKey());
        final var paths = Json.createArrayBuilder();
        b.scope().forEach(paths::add);
        final var json = Json.createObjectBuilder()
                             .add("version", RECORD_VERSION)
                             .add("head", b.head().name())
                             .add("paths", paths)
                             .add("state", new String(b.state(), StandardCharsets.UTF_8))
                             .build();
        WorkspacePaths.writeAtomically(e.getValue(), recordFile(e.getValue()), json.toString().getBytes(StandardCharsets.UTF_8));
        written.add(e.getValue());
      }
    } catch (IOException e) {
      for (final var root : written) Files.deleteIfExists(recordFile(root));
      throw e;
    }
  }

  private static Before readRecord(final Path root) {
    final var file = recordFile(root);
    try (final var reader = Json.createReader(new StringReader(Files.readString(file)))) {
      final var json = reader.readObject();
      if (json.getInt("version") != RECORD_VERSION) throw new IllegalArgumentException("unsupported version");
      final var scope = new TreeSet<String>();
      json.getJsonArray("paths").getValuesAs(JsonString.class).forEach(s -> scope.add(s.getString()));
      return new Before(
          ObjectId.fromString(json.getString("head")),
          json.getString("state").getBytes(StandardCharsets.UTF_8),
          scope);
    } catch (IOException | JsonException | ClassCastException | NullPointerException | IllegalArgumentException e) {
      throw new WorkspaceHistoryException(
          Kind.REPOSITORY_INCONSISTENT,
          "Workspace at %s has an unreadable record of an interrupted change (%s); it was left untouched."
              .formatted(root, file),
          e);
    }
  }

  /**
   * Resolve a mutation this or an earlier process did not finish. If HEAD is the recorded pre-operation HEAD, the
   * change never committed: its declared paths and runtime state are restored. If HEAD is exactly one commit on top
   * of it, the commit landed and only the record was left: the change is kept. Then the repository must pass a full
   * check before the record is removed (drift elsewhere is only logged). Anything else is ambiguous and fails closed, keeping the record. Caller holds
   * the lock.
   */
  private Set<String> recover(final Path root) throws IOException, GitAPIException {
    final var record = readRecord(root);
    try (final var git = open(root); final var walk = new RevWalk(git.getRepository())) {
      final var repo = git.getRepository();
      requireExpectedGitState(repo, false);
      configure(repo);
      // Our in-process lock was held, so Git lock files left now were the dead process's, mid index write or commit
      for (final var lock : List.of("index.lock", Constants.R_HEADS + BRANCH + ".lock", "HEAD.lock")) {
        if (Files.deleteIfExists(repo.getDirectory().toPath().resolve(lock))) logger.warn("Removed stale {} in {}", lock, root);
      }
      final var head = walk.parseCommit(repo.resolve(Constants.HEAD));
      if (head.equals(record.head())) {
        logger.warn("Rolling back an interrupted change to {} in workspace {}", record.scope(), root);
        rollbackMutation(root, record);
      } else if (head.getParentCount() == 1 && head.getParent(0).equals(record.head())) {
        logger.warn("An interrupted change to {} in workspace {} had been committed; keeping it", record.scope(), root);
      } else {
        throw inconsistent("Workspace at %s has an interrupted change recorded on top of %s, but HEAD is %s; it cannot "
                               .formatted(root, record.head().name(), head.name())
                           + "be recovered automatically and was left untouched.");
      }
    }
    final var drift = checkIntegrity(root);
    Files.delete(recordFile(root));
    return drift;
  }

  /** Restore each workspace; if any cannot be restored, fail with REPOSITORY_INCONSISTENT, keeping its record. */
  private void rollbackAll(
      final List<Integer> ids,
      final Map<Integer, Path> rootsById,
      final Map<Integer, Before> before,
      final Exception cause)
  {
    final var failed = new ArrayList<Integer>();
    final var errors = new ArrayList<Exception>();
    for (final var id : ids) {
      try {
        final var root = rootsById.get(id);
        rollbackMutation(root, before.get(id));
        try (final var git = open(root)) {
          requireIndexMatchesHead(git.getRepository());
        }
        Files.delete(recordFile(root));
      } catch (Exception e) {
        logger.error("Could not restore workspace {} after a failed mutation", id, e);
        failed.add(id);
        errors.add(e);
      }
    }
    if (!errors.isEmpty()) {
      final var ex = new WorkspaceHistoryException(
          Kind.REPOSITORY_INCONSISTENT,
          "A failed change could not be fully rolled back in workspace(s) %s; the workspace may not match its history."
              .formatted(failed),
          errors.getFirst());
      errors.stream().skip(1).forEach(ex::addSuppressed);
      if (cause != null) ex.addSuppressed(cause);
      throw ex;
    }
  }

  /**
   * Put a workspace's declared paths, their index entries and its runtime state back to the pre-operation state.
   * Only working-tree files under the declared paths are read or written. Undoes an unfinished mutation; unrelated to
   * restoring a file's earlier revision, which is an ordinary forward mutation.
   */
  private static void rollbackMutation(final Path root, final Before before) throws IOException, GitAPIException {
    try (final var git = open(root); final var walk = new RevWalk(git.getRepository())) {
      final var repo = git.getRepository();

      // Move the branch back only if the operation's commit actually landed.
      if (!before.head().equals(repo.resolve(Constants.HEAD))) {
        final var update = repo.updateRef(Constants.HEAD);
        update.setNewObjectId(before.head());
        final var result = update.forceUpdate();
        if (result != RefUpdate.Result.FORCED && result != RefUpdate.Result.NO_CHANGE) {
          throw new IOException("Could not move HEAD back to " + before.head().name() + ": " + result);
        }
      }
      restoreScope(git, root, treeEntries(repo, walk.parseCommit(before.head()).getTree(), before.scope()), before.scope());
      WorkspacePaths.writeAtomically(root, WorkspaceState.file(root), before.state());
    }
  }

  /**
   * Make the working tree and index under the scope exactly {@code expected} (a commit's entries there). Only
   * working-tree files under the scope are read or written.
   */
  private static void restoreScope(
      final Git git,
      final Path root,
      final Map<String, Blob> expected,
      final Set<String> scope) throws IOException, GitAPIException
  {
    final var repo = git.getRepository();
    // Remove files HEAD does not have, then any parent directory that leaves empty (empty directories are not
    // versioned state; this only avoids leaving a failed save's new folders behind).
    for (final var key : scope) {
      for (final var file : filesUnder(root, key).entrySet()) {
        if (expected.containsKey(file.getKey())) continue;
        Files.delete(file.getValue());
        for (var p = file.getValue().getParent(); !p.equals(root) && isEmptyDirectory(p); p = p.getParent()) Files.delete(p);
      }
    }
    // Rewrite files that differ or are missing, each atomically
    for (final var e : expected.entrySet()) {
      final var file = root.resolve(e.getKey());
      if (matches(repo, file, e.getValue())) continue;
      Files.createDirectories(file.getParent());
      final var tmp = WorkspacePaths.tempFile(root);
      try {
        try (final var out = Files.newOutputStream(tmp)) {
          repo.open(e.getValue().id(), Constants.OBJ_BLOB).copyTo(out);
        }
        if (repo.getFS().supportsExecute()) repo.getFS().setExecute(tmp.toFile(), e.getValue().mode() == FileMode.EXECUTABLE_FILE);
        Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } finally {
        Files.deleteIfExists(tmp);
      }
    }
    stage(git, root, scope);
  }

  /** Whether a working file already has a blob's content and mode. */
  private static boolean matches(final Repository repo, final Path file, final Blob blob) throws IOException {
    if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || modeOf(repo, file) != blob.mode()) return false;
    return contentId(file).equals(blob.id());
  }

  private static ObjectId contentId(final Path file) throws IOException {
    try (final var in = Files.newInputStream(file); final var formatter = new ObjectInserter.Formatter()) {
      return formatter.idFor(Constants.OBJ_BLOB, Files.size(file), in);
    }
  }

  private static boolean isEmptyDirectory(final Path dir) throws IOException {
    if (!Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) return false;
    try (final var entries = Files.list(dir)) {
      return entries.findAny().isEmpty();
    }
  }
  //endregion

  //region Maintenance
  /** Object counts and sizes of a workspace's repository. Lock-free; reads only the object store's directories. */
  public RepositoryStats statistics(final int workspaceId) throws Exception {
    try (final var git = open(roots.workspaceRootPath(workspaceId).normalize())) {
      final var p = git.gc().getStatistics();
      return new RepositoryStats(
          ((Number) p.get("numberOfLooseObjects")).longValue(),
          ((Number) p.get("sizeOfLooseObjects")).longValue(),
          ((Number) p.get("numberOfPackedObjects")).longValue(),
          ((Number) p.get("sizeOfPackedObjects")).longValue(),
          ((Number) p.get("numberOfPackFiles")).longValue());
    }
  }

  /**
   * Pack a workspace's loose objects (and prune unreachable ones past Git's default expiry) under its lock, so no
   * mutation runs concurrently. Only for managed workspaces with no unfinished mutation.
   */
  public RepositoryStats maintain(final int workspaceId) throws Exception {
    final var root = roots.workspaceRootPath(workspaceId).normalize();
    locked(workspaceId, () -> {
      if (!isManaged(root) || Files.exists(recordFile(root), LinkOption.NOFOLLOW_LINKS)) return null;
      try (final var git = open(root)) {
        git.gc().setAggressive(false).call();
      }
      return null;
    });
    return statistics(workspaceId);
  }

  private void countCommit(final int workspaceId) {
    final var count = commitsSinceMaintenance.computeIfAbsent(workspaceId, k -> new AtomicInteger());
    if (count.incrementAndGet() < MAINTENANCE_INTERVAL) return;
    count.set(0);
    maintenanceExecutor.execute(() -> {
      try {
        logger.info("Workspace {} maintenance: {}", workspaceId, maintain(workspaceId));
      } catch (Exception e) {
        logger.warn("Background maintenance of workspace {} failed", workspaceId, e);
      }
    });
  }
  //endregion

  //region Reads (lock-free)
  /**
   * The most recent commit that changed each content path (sidecars excluded), keyed by '/'-separated
   * workspace-relative path. Cached per workspace by HEAD; when HEAD advances only the new commits are walked.
   */
  public Map<String, LastEdit> lastEdits(final Path root) throws IOException {
    if (!Files.isDirectory(root.resolve(WorkspacePaths.GIT_DIR))) return Map.of();
    try (final var git = open(root); final var walk = new RevWalk(git.getRepository())) {
      final var repo = git.getRepository();
      final var head = repo.resolve(Constants.HEAD);
      if (head == null) return Map.of();
      final var cached = lastEditCache.get(root);
      if (cached != null && cached.head().equals(head)) return cached.edits();

      final var edits = new HashMap<String, LastEdit>();
      var reachedCache = false;
      walk.markStart(walk.parseCommit(head));
      for (final var commit : walk) {
        if (cached != null && commit.equals(cached.head())) {
          reachedCache = true;
          break;
        }
        try (final var tw = new TreeWalk(repo)) {
          tw.setRecursive(true);
          tw.setFilter(TreeFilter.ANY_DIFF);
          if (commit.getParentCount() > 0) {
            tw.addTree(walk.parseCommit(commit.getParent(0)).getTree());
          } else {
            tw.addTree(new EmptyTreeIterator());
          }
          tw.addTree(commit.getTree());
          final var author = commit.getAuthorIdent();
          final var edit = new LastEdit(author.getName(), author.getWhenAsInstant(), commit.getParentCount() == 0);
          while (tw.next()) {
            if (tw.getRawMode(1) == 0) continue; // deleted in this commit
            final var path = tw.getPathString();
            if (RenderType.isAerieMetadataFile(path.substring(path.lastIndexOf('/') + 1))) continue;
            edits.putIfAbsent(path, edit);
          }
        }
      }
      if (reachedCache) cached.edits().forEach(edits::putIfAbsent);

      final var result = Map.copyOf(edits);
      lastEditCache.put(root, new LastEditIndex(head.copy(), result));
      return result;
    }
  }
  //endregion
}
