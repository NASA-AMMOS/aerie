package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.banananation.activities.BiteBananaActivity;
import gov.nasa.jpl.aerie.banananation.generated.activities.BiteBananaActivityMapper;
import gov.nasa.jpl.aerie.merlin.framework.ActivityMapper;
import gov.nasa.jpl.aerie.merlin.framework.Result;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

public class CustomValueMappers {
  public static ValueMapper<BiteBananaActivity> biteBanana() {
    return activityValueMapper(new BiteBananaActivityMapper());
  }


  public static <T, C> ValueMapper<T> activityValueMapper(ActivityMapper<Mission, T, C> activityMapper) {
    return new ValueMapper<T>() {
      @Override
      public ValueSchema getValueSchema() {
        return activityMapper.getInputAsOutput().getSchema();
      }

      @Override
      public Result<T, String> deserializeValue(final SerializedValue serializedValue) {
          try {
              return Result.success(activityMapper.getInputType().instantiate(serializedValue.asMap().get()));
          } catch (InstantiationException e) {
              return Result.failure(e.toString());
          }
      }

      @Override
      public SerializedValue serializeValue(final T value) {
        return activityMapper.getInputAsOutput().serialize(value);
      }
    };
  }
}
