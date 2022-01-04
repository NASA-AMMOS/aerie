import com.impossibl.postgres.jdbc.PGDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Assume;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatabaseTests {
  private static final File initSqlScriptFile = new File("../deployment/postgres-init-db/sql/merlin/init.sql");
  private java.sql.Connection connection;

  // Setup test database
  @BeforeAll
  void beforeAll() throws SQLException, IOException, InterruptedException {

    // Create test database and grant privileges
    {
      final var pb = new ProcessBuilder("psql",
                                             "postgresql://postgres:postgres@localhost:5432",
                                             "-v", "ON_ERROR_STOP=1",
                                             "-c", "CREATE DATABASE aerie_merlin_test;",
                                             "-c", "GRANT ALL PRIVILEGES ON DATABASE aerie_merlin_test TO aerie;"
      );

      final var proc = pb.start();

      // Handle the case where we cannot connect to postgres by skipping the tests
      final var errors = new String(proc.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
      Assume.assumeFalse(errors.contains("could not connect to server: Connection refused"));
      proc.waitFor();
      proc.destroy();
    }

    // Grant table privileges to aerie user for the tests
    // Apparently, the previous privileges are insufficient on their own
    {
      final var pb = new ProcessBuilder("psql",
                              "postgresql://postgres:postgres@localhost:5432/aerie_merlin_test",
                              "-v", "ON_ERROR_STOP=1",
                              "-c", "ALTER DEFAULT PRIVILEGES GRANT ALL ON TABLES TO aerie;",
                              "-c", "\\ir %s".formatted(initSqlScriptFile.getAbsolutePath())
      );

      pb.redirectError(ProcessBuilder.Redirect.INHERIT);
      final var proc = pb.start();
      proc.waitFor();
      proc.destroy();
    }

    final var pgDataSource = new PGDataSource();

    pgDataSource.setServerName("localhost");
    pgDataSource.setPortNumber(5432);
    pgDataSource.setDatabaseName("aerie_merlin_test");
    pgDataSource.setApplicationName("Merlin Database Tests");

    final var hikariConfig = new HikariConfig();
    hikariConfig.setUsername("aerie");
    hikariConfig.setPassword("aerie");
    hikariConfig.setDataSource(pgDataSource);

    final var hikariDataSource = new HikariDataSource(hikariConfig);

    connection = hikariDataSource.getConnection();
  }

  // Teardown test database
  @AfterAll
  void afterall() throws SQLException, IOException, InterruptedException {
    Assume.assumeNotNull(connection);
    connection.close();

    // Clear out all data from the database on test conclusion
    // This is done WITH (FORCE) so there aren't issues with trying
    // to drop a database while there are connected sessions from
    // dev tools
    final var pb = new ProcessBuilder("psql",
                            "postgresql://postgres:postgres@localhost:5432",
                            "-v", "ON_ERROR_STOP=1",
                            "-c", "DROP DATABASE IF EXISTS aerie_merlin_test WITH (FORCE);"
    );

    pb.redirectError(ProcessBuilder.Redirect.INHERIT);
    final var proc = pb.start();
    proc.waitFor();
    proc.destroy();
  }
}
