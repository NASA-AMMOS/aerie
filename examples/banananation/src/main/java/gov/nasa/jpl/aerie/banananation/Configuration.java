package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;

import java.nio.file.Path;

import static gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Template;

public record Configuration(int initialPlantCount, String initialProducer, Path initialDataPath) {

  public static final int DEFAULT_PLANT_COUNT = 200;
  public static final String DEFAULT_PRODUCER = "Chiquita";
  public static final Path DEFAULT_DATA_PATH = Path.of("/etc/os-release");

  @Export.Validation("plant count must be positive")
  public boolean validateInitialPlantCount() {
    return this.initialPlantCount > 0;
  }

  @Export.Validation("data path must exist")
  public boolean validateInitialDataPath() {
    return initialDataPath.toFile().exists();
  }

  public static @Template Configuration defaultConfiguration() {
    return new Configuration(DEFAULT_PLANT_COUNT, DEFAULT_PRODUCER, DEFAULT_DATA_PATH);
  }
}
