package gov.nasa.ammos.plandev.workspace.server;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * The single source of truth for which names inside a workspace are internal to the workspace server.
 *
 * - .git holds the workspace's history repository (see {@link WorkspaceHistory}).
 * - .gitignore, .gitattributes and .gitmodules would change what Git tracks or how it rewrites file bytes, which would
 *   let a "clean" repository silently omit or alter user files. They are reserved in Phase 1; .gitignore may be relaxed
 *   once Git remote interoperability exists.
 * - .seqdev holds same-filesystem temporary files used for atomic replacement. It is never committed and is not part
 *   of the versioned workspace.
 * - .seqdevkeep is an empty, tracked marker for an otherwise empty directory. It is managed by the server and hidden
 *   from workspace APIs.
 *
 * Reserved names are rejected at every path segment, case-insensitively, by
 * {@link WorkspaceFileSystemService#resolveReadingPath}, so no user-facing file API can read or write them.
 * .meta.seqdev sidecars are SeqDev-managed but are not reserved here: they are addressed through the
 * metadata API, and the file API handlers reject them directly.
 */
public final class WorkspacePaths {
  private WorkspacePaths() {}

  public static final String GIT_DIR = ".git";
  public static final String INTERNAL_DIR = ".seqdev";
  public static final String DIRECTORY_MARKER = ".seqdevkeep";

  private static final Set<String> RESERVED = Set.of(
      GIT_DIR, ".gitignore", ".gitattributes", ".gitmodules", INTERNAL_DIR, DIRECTORY_MARKER);
  private static final Pattern TEMP_FILE = Pattern.compile("tmp-\\p{XDigit}{8}(-\\p{XDigit}{4}){3}-\\p{XDigit}{12}");

  public static boolean isReservedName(final String segment) {
    return RESERVED.contains(segment.toLowerCase(Locale.ROOT));
  }

  /** Exact match for the server-owned directory marker; case variants remain reserved but are not valid markers. */
  public static boolean isDirectoryMarkerName(final String name) {
    return DIRECTORY_MARKER.equals(name);
  }

  /** The first reserved segment of a workspace-relative path, or null if it has none. */
  public static String firstReservedSegment(final Path relativePath) {
    for (final var segment : relativePath) {
      if (isReservedName(segment.toString())) return segment.toString();
    }
    return null;
  }

  /** A workspace-relative Git path key. Git tree paths always use '/', regardless of the host filesystem separator. */
  public static String key(final Path root, final Path absolutePath) {
    final var relative = root.normalize().relativize(absolutePath.normalize());
    return relative.toString().replace(relative.getFileSystem().getSeparator(), "/");
  }

  /**
   * Create a directory tree and update markers from the leaf back toward the workspace root. A new leaf gets a marker;
   * any parent containing that leaf is no longer empty and loses its marker.
   */
  public static void createDirectories(final Path root, final Path directory) throws IOException {
    final var normalizedRoot = root.normalize();
    final var normalizedDirectory = directory.normalize();
    if (!normalizedDirectory.startsWith(normalizedRoot)) {
      throw new IOException("Directory " + directory + " is outside workspace " + root);
    }

    Files.createDirectories(normalizedDirectory);
    for (var current = normalizedDirectory; !current.equals(normalizedRoot); current = current.getParent()) {
      updateDirectoryMarker(normalizedRoot, current);
    }
  }

  /** Add markers to empty directories already present when a workspace is first adopted. */
  public static void initializeDirectoryMarkers(final Path root) throws IOException {
    for (final var path : walk(root, Integer.MAX_VALUE)) {
      if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) updateDirectoryMarker(root, path);
    }
  }

  /** Keep the marker in one directory exactly when it has no other entries. The workspace root needs no marker. */
  public static void updateDirectoryMarker(final Path root, final Path directory) throws IOException {
    final var normalizedRoot = root.normalize();
    final var normalizedDirectory = directory.normalize();
    if (normalizedDirectory.equals(normalizedRoot)) return;
    if (!normalizedDirectory.startsWith(normalizedRoot)) {
      throw new IOException("Directory " + directory + " is outside workspace " + root);
    }
    if (!Files.isDirectory(normalizedDirectory, LinkOption.NOFOLLOW_LINKS)) return;

    final var marker = normalizedDirectory.resolve(DIRECTORY_MARKER);
    if (Files.exists(marker, LinkOption.NOFOLLOW_LINKS)
        && (!Files.isRegularFile(marker, LinkOption.NOFOLLOW_LINKS) || Files.size(marker) != 0)) {
      throw new IOException("Invalid directory marker " + marker);
    }

    final boolean otherwiseEmpty;
    try (final var entries = Files.list(normalizedDirectory)) {
      otherwiseEmpty = entries.allMatch(path -> isDirectoryMarkerName(path.getFileName().toString()));
    }
    if (otherwiseEmpty) {
      if (!Files.exists(marker, LinkOption.NOFOLLOW_LINKS)) Files.createFile(marker);
    } else {
      Files.deleteIfExists(marker);
    }
  }

  /** Let a filesystem rename replace a marker-only directory just as it previously replaced a literally empty one. */
  public static void removeEmptyDirectoryMarker(final Path root, final Path directory) throws IOException {
    updateDirectoryMarker(root, directory);
    Files.deleteIfExists(directory.resolve(DIRECTORY_MARKER));
  }

  /**
   * A fresh path for a server-owned temp file. It lives in the internal directory on the workspace's filesystem, so it
   * can be renamed atomically into the tree; ignored by Git, so a crash cannot leave stray content in the tree; and
   * named so {@link #removeStaleTempFiles} can recognize it.
   */
  public static Path tempFile(final Path root) throws IOException {
    final var dir = root.resolve(INTERNAL_DIR);
    Files.createDirectories(dir);
    return dir.resolve("tmp-" + UUID.randomUUID());
  }

  /** Replace target with bytes so readers, and a crash, see either the old or the new content. */
  public static void writeAtomically(final Path root, final Path target, final byte[] bytes) throws IOException {
    final var tmp = tempFile(root);
    try {
      Files.write(tmp, bytes);
      Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      Files.deleteIfExists(tmp);
    }
  }

  /** Delete temp files a crashed process left behind. Only exact {@link #tempFile} names are touched. */
  public static void removeStaleTempFiles(final Path root) throws IOException {
    final var dir = root.resolve(INTERNAL_DIR);
    if (!Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) return;
    try (final var entries = Files.list(dir)) {
      for (final var p : entries.toList()) {
        if (TEMP_FILE.matcher(p.getFileName().toString()).matches() && Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)) {
          Files.delete(p);
        }
      }
    }
  }

  /**
   * Like Files.walk(dir, maxDepth) minus the starting directory, but never descends into (or returns) a
   * reserved name, so Git internals and server scratch files never leak into listings, copies or scans. Symbolic links
   * are neither followed nor returned: they are not supported workspace content.
   */
  public static List<Path> walk(final Path dir, final int maxDepth) throws IOException {
    final var out = new ArrayList<Path>();
    Files.walkFileTree(dir, EnumSet.noneOf(java.nio.file.FileVisitOption.class), maxDepth, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult preVisitDirectory(final Path d, final BasicFileAttributes attrs) {
        if (d.equals(dir)) return FileVisitResult.CONTINUE;
        if (isReservedName(d.getFileName().toString())) return FileVisitResult.SKIP_SUBTREE;
        out.add(d);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(final Path f, final BasicFileAttributes attrs) {
        // Directories at maxDepth arrive here instead of preVisitDirectory; unfollowed symlinks arrive here too
        if (!attrs.isSymbolicLink() && !isReservedName(f.getFileName().toString())) out.add(f);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(final Path f, final IOException exc) throws IOException {
        throw exc;
      }
    });
    return out;
  }
}
