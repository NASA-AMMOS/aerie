package gov.nasa.jpl.aerie.banananation;

import java.nio.file.Path;

public final record Configuration (int initialPlantCount, String initialProducer, Path initialDataPath) {

  public static final int DEFAULT_PLANT_COUNT = 200;
  public static final String DEFAULT_PRODUCER = "Chiquita";
  public static final Path DEFAULT_DATA_PATH = Path.of("/etc/os-release");

  public static Configuration defaultConfiguration() {
    return new Configuration(DEFAULT_PLANT_COUNT, DEFAULT_PRODUCER, DEFAULT_DATA_PATH);
  }
}
