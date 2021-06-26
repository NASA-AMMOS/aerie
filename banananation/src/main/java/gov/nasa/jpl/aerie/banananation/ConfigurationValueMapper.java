package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.IntegerValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.PathValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.StringValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.Result;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.Map;

public final class ConfigurationValueMapper implements ValueMapper<Configuration> {
  @Override
  public ValueSchema getValueSchema() {
    return ValueSchema.ofStruct(Map.of(
        "initialPlantCount", ValueSchema.INT,
        "initialProducer", ValueSchema.STRING,
        "initialDataPath", ValueSchema.PATH));
  }

  @Override
  public Result<Configuration, String> deserializeValue(final SerializedValue serializedValue) {
    final var map$ = serializedValue.asMap();
    if (map$.isEmpty()) return Result.failure("Expected map, got " + serializedValue);
    final var map = map$.orElseThrow();

    // Return a default configuration when deserializing a null serialized value
    if (map.isEmpty()) return Result.success(Configuration.defaultConfiguration());

    if (!map.containsKey("initialPlantCount")) return Result.failure("Expected field \"initialPlantCount\", but not found: " + serializedValue);
    final var plantCount$ = new IntegerValueMapper().deserializeValue(map.get("initialPlantCount"));
    if (plantCount$.getKind() == Result.Kind.Failure) return Result.failure(plantCount$.getFailureOrThrow());
    final var plantCount = plantCount$.getSuccessOrThrow();

    if (!map.containsKey("initialProducer")) return Result.failure("Expected field \"initialProducer\", but not found: " + serializedValue);
    final var producer$ = new StringValueMapper().deserializeValue(map.get("initialProducer"));
    if (producer$.getKind() == Result.Kind.Failure) return Result.failure(producer$.getFailureOrThrow());
    final var producer = producer$.getSuccessOrThrow();

    // Use a default data path parameter if this parameter is not present within the serialized value being deserialized
    final var dataPath$ = new PathValueMapper().deserializeValue(map.getOrDefault("initialDataPath", new PathValueMapper().serializeValue(Configuration.DEFAULT_DATA_PATH)));
    if (dataPath$.getKind() == Result.Kind.Failure) return Result.failure(dataPath$.getFailureOrThrow());
    final var dataPath = dataPath$.getSuccessOrThrow();

    return Result.success(new Configuration(
        plantCount,
        producer,
        dataPath));
  }

  @Override
  public SerializedValue serializeValue(final Configuration value) {
    return SerializedValue.of(Map.of(
        "initialPlantCount", SerializedValue.of(value.initialPlantCount()),
        "initialProducer", SerializedValue.of(value.initialProducer()),
        "initialDataPath", new PathValueMapper().serializeValue(value.initialDataPath())));
  }
}
