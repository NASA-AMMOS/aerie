package gov.nasa.jpl.aerie.merlin.server.remotes;

import com.impossibl.postgres.jdbc.PGDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresRepository;

public class PostgresRepositoryActionTest {

  public static void main(final String[] args) {
    final var pgDataSource = new PGDataSource();
    pgDataSource.setServerName("localhost");
    pgDataSource.setPortNumber(5432);
    pgDataSource.setDatabaseName("aerie");
    pgDataSource.setApplicationName("Merlin Server");

    final var hikariConfig = new HikariConfig();
    hikariConfig.setUsername("aerie");
    hikariConfig.setPassword("aerie");
    hikariConfig.setSchema("merlin");
    hikariConfig.setDataSource(pgDataSource);

    final var hikariDataSource = new HikariDataSource(hikariConfig);

    final var plans = new PostgresRepository(hikariDataSource);

    System.out.println(plans.getAllPlans());
  }
}
