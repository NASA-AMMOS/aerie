package gov.nasa.jpl.aerie.database;

import com.impossibl.postgres.jdbc.PGDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Manages the test database.
 */
public class DatabaseTestHelper {
  private Connection connection;

  private final String dbName;
  private final String appName;
  private final File initSqlScriptFile;
  private final Optional<Path> migrationsDirectory;

  public DatabaseTestHelper(String dbName, String appName, File initSqlScriptFile) {
    this(dbName, appName, initSqlScriptFile, Optional.empty());
  }

  public DatabaseTestHelper(String dbName, String appName, File initSqlScriptFile, Path migrationsDirectory) {
    this(dbName, appName, initSqlScriptFile, Optional.of(migrationsDirectory));
  }

  private DatabaseTestHelper(String dbName, String appName, File initSqlScriptFile, Optional<Path> migrationsDirectory) {
    this.dbName = dbName;
    this.appName = appName;
    this.initSqlScriptFile = initSqlScriptFile;
    this.migrationsDirectory = migrationsDirectory;
  }

  public void startDatabaseWithLatestSchema() throws SQLException, IOException, InterruptedException {
    createDatabase();
    if (migrationsDirectory.isPresent()) {
      applyAllMigrations();
    } else {
      runInitSql();
    }
    establishConnection();
  }

  public void startDatabaseBeforeMigration(final String migrationName) throws SQLException, IOException, InterruptedException {
    createDatabase();
    applyMigrationsBefore(migrationName);
    establishConnection();
  }

  /**
   * Sets up the test database
   */
  private void createDatabase() throws IOException, InterruptedException {
    // Create test database and grant privileges
    runSubprocess(
      "psql",
      "postgresql://postgres:postgres@localhost:5432",
      "-v", "ON_ERROR_STOP=1",
      "-c", "CREATE DATABASE " + dbName + ";",
      "-c", "GRANT ALL PRIVILEGES ON DATABASE " + dbName + " TO aerie;"
    );

    // Grant table privileges to aerie user for the tests
    // Apparently, the previous privileges are insufficient on their own
    runSubprocess(
      "psql",
      "postgresql://postgres:postgres@localhost:5432/" + dbName,
      "-v", "ON_ERROR_STOP=1",
      "-c", "ALTER DEFAULT PRIVILEGES GRANT ALL ON TABLES TO aerie;"
    );
  }

  private void runInitSql() throws IOException, InterruptedException, SQLException {
    runSqlFile(initSqlScriptFile.getAbsolutePath());
  }

  private void establishConnection() throws SQLException {
    final var pgDataSource = new PGDataSource();

    pgDataSource.setServerName("localhost");
    pgDataSource.setPortNumber(5432);
    pgDataSource.setDatabaseName(dbName);
    pgDataSource.setApplicationName(appName);

    final var hikariConfig = new HikariConfig();
    hikariConfig.setUsername("aerie");
    hikariConfig.setPassword("aerie");
    hikariConfig.setDataSource(pgDataSource);

    final var hikariDataSource = new HikariDataSource(hikariConfig);

    connection = hikariDataSource.getConnection();
  }

  /**
   * Runs the given sql file
   */
  public void runSqlFile(final String filePath) throws IOException, InterruptedException, SQLException {
    final var errors = runSubprocess(
        "psql",
       "postgresql://postgres:postgres@localhost:5432/" + dbName,
       "-v", "ON_ERROR_STOP=1",
       "-f", filePath);
    if (errors.toLowerCase().contains("error:")) {
      throw new SQLException(errors);
    }
  }

  /**
   * Tears down the test database
   */
  public void stopDatabase() throws SQLException, IOException, InterruptedException {

    if (connection != null) {
      connection.close();
    }

    // Clear out all data from the database on test conclusion
    // This is done WITH (FORCE) so there aren't issues with trying
    // to drop a database while there are connected sessions from
    // dev tools
    runSubprocess(
        "psql",
        "postgresql://postgres:postgres@localhost:5432",
        "-v", "ON_ERROR_STOP=1",
        "-c", "DROP DATABASE IF EXISTS " + dbName + " WITH (FORCE);"
    );
  }

  /**
   * Run a subprocess and return its stderr output
   */
  private static String runSubprocess(final String ... args) throws IOException, InterruptedException {
    final var pb = new ProcessBuilder(args);
    final var proc = pb.start();
    try {
      proc.waitFor();
      final var stderr = new String(proc.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
      System.err.println(stderr);
      return stderr;
    } finally {
      proc.destroy();
    }
  }

  public Connection connection() {
    return connection;
  }

  void applyMigration(final String migrationName) throws IOException, InterruptedException, SQLException {
    assertMigrationExists(migrationName);
    runSqlFile(migrationsDirectory.get().resolve(migrationName).resolve("up.sql").toString());
  }

  void rollbackMigration(final String migrationName) throws IOException, InterruptedException, SQLException {
    assertMigrationExists(migrationName);
    runSqlFile(migrationsDirectory.get().resolve(migrationName).resolve("down.sql").toString());
  }

  /**
   * Applies all migrations alphabetically up to but not including the given migration
   */
  void applyMigrationsBefore(final String migrationName) throws IOException, InterruptedException, SQLException {
    assertMigrationExists(migrationName);
    Integer previousVersion = null;
    for (final var migration : getSortedMigrations()) {
      if (previousVersion == null) {
        previousVersion = migration.version();
      } else {
        assertTrue(
            migration.version() > previousVersion,
            "Duplicate migration version found: %d is not greater than %d".formatted(
                migration.version(),
                previousVersion));
      }
      if (migration.name().equals(migrationName)) {
        return;
      }
      applyMigration(migration.name());
    }
  }

  /**
   * Applies all migrations alphabetically
   */
  void applyAllMigrations() throws IOException, InterruptedException, SQLException {
    Integer previousVersion = null;
    for (final var migration : getSortedMigrations()) {
      if (previousVersion == null) {
        previousVersion = migration.version();
      } else {
        assertTrue(
            migration.version() > previousVersion,
            "Duplicate migration version found: %d is not greater than %d".formatted(
                migration.version(),
                previousVersion));
      }
      applyMigration(migration.name());
    }
  }

  private List<Migration> getSortedMigrations() {
    return Arrays
        .stream(Objects.requireNonNull(migrationsDirectory.get().toFile().listFiles()))
        .map(Migration::of)
        .sorted(Comparator.comparing(Migration::name))
        .toList();
  }

  void assertMigrationExists(final String migrationName) {
    if (!new File(migrationsDirectory.get().resolve(migrationName).toString()).isDirectory()) {
      throw new AssertionError("No such migration found: %s".formatted(migrationName));
    }
  }

  void executeUpdate(final String sql, final Object... args) throws SQLException {
    try (final var statement = connection.createStatement()) {
      statement.executeUpdate(sql.formatted(args));
    }
  }

  String dumpSchema() throws IOException, InterruptedException {
    final var pb = new ProcessBuilder(
        "pg_dump",
        "--schema-only",
        "postgresql://postgres:postgres@localhost:5432/" + dbName);

    final var proc = pb.start();
    try {
      proc.waitFor();
      return new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    } finally {
      proc.destroy();
    }
  }

  record Migration(int version, String name) {
    static Migration of(final String migrationName) {
      return new Migration(Integer.parseInt(migrationName.split("_")[0]), migrationName);
    }
    static Migration of(final File file) {
      return Migration.of(file.getName());
    }
  }
}
