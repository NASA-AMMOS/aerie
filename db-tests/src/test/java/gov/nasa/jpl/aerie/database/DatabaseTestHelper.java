package gov.nasa.jpl.aerie.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Assumptions;

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

  /**
   * Sets up the test database
   */
  public void startDatabase() throws SQLException, IOException, InterruptedException {
    // Create test database and grant privileges
    {
      final var pb = new ProcessBuilder("psql",
                                        "postgresql://postgres:postgres@localhost:5432",
                                        "-v", "ON_ERROR_STOP=1",
                                        "-c", "CREATE DATABASE " + dbName + ";",
                                        "-c", "GRANT ALL PRIVILEGES ON DATABASE " + dbName + " TO aerie;"
      );

      final var proc = pb.start();

      // Handle the case where we cannot connect to postgres by skipping the tests
      final var errors = new String(proc.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
      Assumptions.assumeFalse(
          (  errors.contains("Connection refused")
          || errors.contains("role \"postgres\" does not exist")));
      proc.waitFor();
      proc.destroy();
    }

    // Grant table privileges to aerie user for the tests
    // Apparently, the previous privileges are insufficient on their own
    {
      final var pb = new ProcessBuilder("psql",
                                        "postgresql://aerie:aerie@localhost:5432/" + dbName,
                                        "-v", "ON_ERROR_STOP=1",
                                        "-c", "ALTER DEFAULT PRIVILEGES GRANT ALL ON TABLES TO aerie;",
                                        "-c", "\\ir %s".formatted(initSqlScriptFile.getAbsolutePath())
      );

      pb.redirectError(ProcessBuilder.Redirect.INHERIT);
      final var proc = pb.start();
      proc.waitFor();
      proc.destroy();
    }

    final var hikariConfig = new HikariConfig();

    hikariConfig.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
    hikariConfig.addDataSourceProperty("serverName", "localhost");
    hikariConfig.addDataSourceProperty("portNumber", "5432");
    hikariConfig.addDataSourceProperty("databaseName", dbName);
    hikariConfig.addDataSourceProperty("applicationName", appName);
    hikariConfig.setUsername("aerie");
    hikariConfig.setPassword("aerie");

    hikariConfig.setConnectionInitSql("set time zone 'UTC'");

    final var hikariDataSource = new HikariDataSource(hikariConfig);

    connection = hikariDataSource.getConnection();
  }

  /**
   * Tears down the test database
   */
  public void stopDatabase() throws SQLException, IOException, InterruptedException {

    Assumptions.assumeTrue(connection != null);
    connection.close();

    // Clear out all data from the database on test conclusion
    // This is done WITH (FORCE) so there aren't issues with trying
    // to drop a database while there are connected sessions from
    // dev tools
    final var pb = new ProcessBuilder("psql",
                                      "postgresql://postgres:postgres@localhost:5432",
                                      "-v", "ON_ERROR_STOP=1",
                                      "-c", "DROP DATABASE IF EXISTS " + dbName + " WITH (FORCE);"
    );

    pb.redirectError(ProcessBuilder.Redirect.INHERIT);
    final var proc = pb.start();
    proc.waitFor();
    proc.destroy();
  }

  public Connection connection() {
    return connection;
  }

  public void clearTable(String table) throws SQLException {
    try (final var statement = connection.createStatement()) {
      statement.executeUpdate("TRUNCATE " + table + " CASCADE;");
    }
  }
}
