package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.constraints.resources.ExternalState;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Class mocking the behavior of an externally defined state and implementing ExternalState interface
 *
 * @param <T> the type of the variable managed by the state
 */
public class MockState<T extends Comparable<T>> implements ExternalState
{

  enum SupportedTypes {
    REAL,
    STRING,
    LONG,
    BOOLEAN,
  }

  protected Map<Window, T> values;
  protected SupportedTypes type;

  public void initFromStateFile(SupportedTypes type, Map<Duration, T> fileValues) {
    this.type = type;
    values = new TreeMap<>();
    Duration start = null;
    T val = null;
    for (Map.Entry<Duration, T> entry : fileValues.entrySet()) {
      if (start != null && val != null) {
        values.put(Window.betweenClosedOpen(start, entry.getKey()), val);
      }
      start = entry.getKey();
      val = entry.getValue();
    }
  }


  public SerializedValue getValueAtTime(Duration t) {
    for (Map.Entry<Window, T> intv : values.entrySet()) {
      if (intv.getKey().contains(t)) {
        return serialize(intv.getValue());
      }
    }
    return null;
  }



  public Windows whenValue(Windows windows, Predicate<T> pred) {
    Windows returnWindows = new Windows();

    for (Map.Entry<Window, T> intv : values.entrySet()) {
      if (pred.test(intv.getValue())){
        Window stateRange = intv.getKey();
        for (Window range : windows) {
          var inter = Window.intersect(range, stateRange);
          if (!inter.isEmpty()) {
            returnWindows.add(inter);
          }
        }
      }
    }
    return returnWindows;
  }

  final static Supplier<RuntimeException> exceptionSupplier =
      () -> new UnsupportedOperationException("Type not supported by MockState");

  @SuppressWarnings("unchecked")
  public T deserialize(SerializedValue val){
    switch (type){
      case LONG -> {
        return (T) val.asInt().orElseThrow(exceptionSupplier);
      }
      case BOOLEAN -> {
        return (T) val.asBoolean().orElseThrow(exceptionSupplier);
      }
      case STRING -> {
        return (T) val.asString().orElseThrow(exceptionSupplier);
      }
      case REAL -> {
        return (T) val.asReal().orElseThrow(exceptionSupplier);
      }
    }
    throw exceptionSupplier.get();
  }

  public SerializedValue serialize(T val){
    switch (type){
      case LONG -> {
        return SerializedValue.of((Long) val);
      }
      case BOOLEAN -> {
        return SerializedValue.of((Boolean) val);
      }
      case STRING -> {
        return SerializedValue.of((String) val);
      }
      case REAL -> {
        return SerializedValue.of((Double) val);
      }
    }
    throw exceptionSupplier.get();
  }

  public Windows whenValueBetween(SerializedValue inf, SerializedValue sup, Windows windows) {
   return whenValue(windows,(x) -> x.compareTo(deserialize(inf)) >= 0 && x.compareTo(deserialize(sup)) <= 0);
  }
  public Windows whenValueBelow(SerializedValue val, Windows windows) {
    return whenValue(windows,(x) -> x.compareTo(deserialize(val)) < 0);
  }
  public Windows whenValueAbove(SerializedValue val, Windows windows) {
    return whenValue(windows,(x) -> x.compareTo(deserialize(val)) > 0);
  }

  public Windows whenValueEqual(SerializedValue val, Windows windows) {
    return whenValue(windows,(x) -> x.compareTo(deserialize(val)) == 0);
  }

  public Windows whenValueNotEqual(SerializedValue val, Windows windows) {
    return whenValue(windows,(x) -> x.compareTo(deserialize(val)) != 0);
  }
  @Override
  public Map<Window, SerializedValue> getTimeline(Windows timeDomain) {
    var serialized = new HashMap<Window, SerializedValue>();
    values.forEach((win, val)-> serialized.put(win, serialize(val)));
    return serialized;
  }


  public void draw() {
    for (Map.Entry<Window, T> v : values.entrySet()) {
      if (v.getValue() instanceof Boolean val) {
        String toPrint = "";
        if (val) {
          toPrint = "X";
        } else {
          toPrint = "-";
        }
        int max = (int) v.getKey().end.in(Duration.SECONDS);
        int min = (int) v.getKey().start.in(Duration.SECONDS);

        for (int i = min; i < max; i++) {
          System.out.print(toPrint + "  ");

        }

      }
    }
  }

}
