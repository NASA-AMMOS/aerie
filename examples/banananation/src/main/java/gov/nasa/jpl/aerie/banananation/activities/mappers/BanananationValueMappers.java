package gov.nasa.jpl.aerie.banananation.activities.mappers;

import gov.nasa.jpl.aerie.banananation.Flag;
import gov.nasa.jpl.aerie.banananation.activities.BiteBananaActivity;
import gov.nasa.jpl.aerie.merlin.framework.Result;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.Map;
import java.util.Optional;

public class BanananationValueMappers {
  public static ValueMapper<BiteBananaActivity.ComputedAttributes> biteBananaComputedAttributesValueMapper(
      ValueMapper<Boolean> booleanValueMapper,
      ValueMapper<Flag> flagValueMapper) {
    final var biteSizeWasBig = "biteSizeWasBig";
    final var newFlag = "newFlag";
    return new ValueMapper<>() {
      @Override
      public ValueSchema getValueSchema() {
        return ValueSchema.ofStruct(Map.of(
            biteSizeWasBig, booleanValueMapper.getValueSchema(),
            newFlag, flagValueMapper.getValueSchema()
        ));
      }

      @Override
      public Result<BiteBananaActivity.ComputedAttributes, String> deserializeValue(final SerializedValue serializedValue) {
        final var map$ = serializedValue.asMap();
        if (map$.isEmpty()) return Result.failure("expected struct");
        final var map = map$.get();
        Optional<Boolean> biteSizeWasBigValue = Optional.empty();
        Optional<Flag> newFlagValue = Optional.empty();
        for (final var entry : map.entrySet()) {
          if (entry.getKey().equals(biteSizeWasBig)) {
            final var biteSizeWasBigValue$ = entry.getValue().asBoolean();
            if (biteSizeWasBigValue$.isEmpty()) return Result.failure("expected boolean for field " + biteSizeWasBig);
            biteSizeWasBigValue = biteSizeWasBigValue$;
          } else if (entry.getKey().equals(newFlag)) {
            final var newFlagValue$ = flagValueMapper.deserializeValue(entry.getValue());
            if (newFlagValue$.getKind().equals(Result.Kind.Failure)) return Result.failure("expected Flag for field" + newFlag);
            newFlagValue = Optional.of(newFlagValue$.getSuccessOrThrow());
          } else {
            return Result.failure("Unexpected field " + entry.getKey());
          }
        }
        if (biteSizeWasBigValue.isEmpty()) return Result.failure("Missing required field " + biteSizeWasBigValue);
        if (newFlagValue.isEmpty()) return Result.failure("Missing required field " + newFlag);

        return Result.success(new BiteBananaActivity.ComputedAttributes(biteSizeWasBigValue.get(), newFlagValue.get()));
      }

      @Override
      public SerializedValue serializeValue(final BiteBananaActivity.ComputedAttributes value) {
        return SerializedValue.of(Map.of(
            biteSizeWasBig, booleanValueMapper.serializeValue(value.biteSizeWasBig()),
            newFlag, flagValueMapper.serializeValue(value.newFlag())
        ));
      }
    };
  }
}
