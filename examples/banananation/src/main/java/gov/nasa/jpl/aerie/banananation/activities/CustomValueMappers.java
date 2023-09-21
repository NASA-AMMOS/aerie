package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.banananation.Range;
import gov.nasa.jpl.aerie.banananation.Unit;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.RecordValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.MetadataValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;

import java.lang.annotation.Annotation;
import java.util.List;

@SuppressWarnings("unchecked")
public class CustomValueMappers {
  record gov_nasa_jpl_aerie_banananation_Unit(String value) implements Unit {
    @Override
    public Class<? extends Annotation> annotationType() {
      return Unit.class;
    }
  }

  // TODO Generate this
  public static ValueMapper<Unit> gov_nasa_jpl_aerie_banananation_Unit(
      final ValueMapper<String> java_lang_StringValueMapper) {
    return (ValueMapper<Unit>) (Object) new RecordValueMapper<>(
        gov_nasa_jpl_aerie_banananation_Unit.class,
        List.of(
            new RecordValueMapper.Component<>("value", gov_nasa_jpl_aerie_banananation_Unit::value, java_lang_StringValueMapper)
        )
    );
  }

  public static <T> ValueMapper<@Unit("") T> $taggedUnit(ValueMapper<Unit> unitValueMapper, ValueMapper<T> TValueMapper, Unit annotation) {
    return new MetadataValueMapper<>("unit", unitValueMapper.serializeValue(annotation), TValueMapper);
  }

  record gov_nasa_jpl_aerie_banananation_Range(int min, int max) implements Range {
    @Override
    public Class<? extends Annotation> annotationType() {
      return Range.class;
    }
  }

  // TODO Generate this
  public static ValueMapper<Range> gov_nasa_jpl_aerie_banananation_Range(
      final ValueMapper<Integer> java_lang_IntegerValueMapper) {
    return (ValueMapper<Range>) (Object) new RecordValueMapper<>(
        gov_nasa_jpl_aerie_banananation_Range.class,
        List.of(
            new RecordValueMapper.Component<>("min", gov_nasa_jpl_aerie_banananation_Range::min, java_lang_IntegerValueMapper),
            new RecordValueMapper.Component<>("max", gov_nasa_jpl_aerie_banananation_Range::max, java_lang_IntegerValueMapper)
        )
    );
  }

  public static <T> ValueMapper<@Range(min=0, max=1) T> $taggedRange(ValueMapper<Range> rangeValueMapper, ValueMapper<T> TValueMapper, Range annotation) {
    return new MetadataValueMapper<>("unit", rangeValueMapper.serializeValue(annotation), TValueMapper);
  }
}
