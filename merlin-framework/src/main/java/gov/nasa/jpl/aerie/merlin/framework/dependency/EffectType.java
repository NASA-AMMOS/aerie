package gov.nasa.jpl.aerie.merlin.framework.dependency;

public sealed interface EffectType{
  sealed interface NumericEffectType extends EffectType {
    record Increase(Numeric val) implements NumericEffectType {}
    record Decrease(Numeric val) implements NumericEffectType {}
    record ValueSet(Numeric val) implements NumericEffectType {}
  }
  sealed interface DiscreteEffectType extends EffectType {
    record ValueSet(DiscreteValue value) implements DiscreteEffectType {}
  }

  record UndefinedEffect() implements EffectType {}

  static UndefinedEffect undefinedEffect(){
    return new UndefinedEffect();
  }

  static NumericEffectType set(double value){
    return new NumericEffectType.ValueSet(Numeric.doubleValue(value));
  }

  static DiscreteEffectType set(DependencyObjects.RuntimeObject value){
    return new DiscreteEffectType.ValueSet(new DiscreteValue.ModelReference(value));
  }
  static DiscreteEffectType set(String activityType, String parameterName){
    return new DiscreteEffectType.ValueSet(new DiscreteValue.DiscreteParameter(activityType, parameterName));
  }

  static NumericEffectType.Increase increase(double val){
    return new NumericEffectType.Increase(Numeric.doubleValue(val));
  }

  static NumericEffectType.Increase increase(DependencyObjects.RuntimeObject val){
    return new NumericEffectType.Increase(new Numeric.ObjectValue(val));
  }

  static NumericEffectType.Increase increase(String activityType, String parameterValue){
    return new NumericEffectType.Increase(Numeric.parameterValue(activityType, parameterValue));
  }

  static NumericEffectType.Decrease decrease(double val){
    return new NumericEffectType.Decrease(Numeric.doubleValue(val));
  }
}

//what about complex resources ? like elements of vectors...
