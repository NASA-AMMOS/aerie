package gov.nasa.jpl.aerie.merlin.server.remotes;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresPlanRepository;

public class PostgresRepositoryActionTest {

  public static void main(final String[] args) {

    final var hikariConfig = new HikariConfig();
    hikariConfig.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
    hikariConfig.addDataSourceProperty("serverName", "localhost");
    hikariConfig.addDataSourceProperty("portNumber", 5432);
    hikariConfig.addDataSourceProperty("databaseName", "aerie_merlin");
    hikariConfig.addDataSourceProperty("applicationName", "Merlin Server");
    hikariConfig.setUsername("aerie");
    hikariConfig.setPassword("aerie");

    final var hikariDataSource = new HikariDataSource(hikariConfig);

    final var plans = new PostgresPlanRepository(hikariDataSource);

    System.out.println(plans.getAllPlans());
  }
}
