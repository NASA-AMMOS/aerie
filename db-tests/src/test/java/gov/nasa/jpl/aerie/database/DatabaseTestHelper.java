package gov.nasa.jpl.aerie.database;

import com.impossibl.postgres.jdbc.PGDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Manages the test database.
 */
public class DatabaseTestHelper {
  private Connection connection;

  private final String dbName;
  private final String appName;
  private final File initSqlScriptFile;

  public DatabaseTestHelper(String dbName, String appName, File initSqlScriptFile) {
    this.dbName = dbName;
    this.appName = appName;
    this.initSqlScriptFile = initSqlScriptFile;
  }

  public void startDatabase() throws SQLException, IOException, InterruptedException {
    createDatabase();
    runInitSql();
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
}
