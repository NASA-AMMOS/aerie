package gov.nasa.jpl.ammos.mpsa.aerie.banananation;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.spice.SpiceLoader;
import org.apache.commons.lang3.tuple.Pair;

import spice.basic.CSPICE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Predicate;

public final class Main {
  private static Optional<MerlinAdaptation> getAdaptation(final String name, final String version) {
    final Predicate<Adaptation> predicate = a -> a.name().equals(name) && a.version().equals(version);

    return ServiceLoader
        .load(MerlinAdaptation.class)
        .stream()
        .filter(p -> predicate.test(p.type().getAnnotation(Adaptation.class)))
        .map(ServiceLoader.Provider::get)
        .findFirst();
  }

  /* Mechanically derived from the schedule: */
  private static List<Pair<Double, SerializedActivity>> getSchedule() {
    /* Schedule:
      [
        {time: 0.0, type: "PeelBanana", parameters: {"peelDirection": "fromStem"}},
        {time: 0.5, type: "BiteBanana", parameters: {"biteSize": 0.5}},
        {time: 1.5, type: "BiteBanana", parameters: {}},
      ]
    */
    final List<Pair<Double, SerializedActivity>> schedule = new ArrayList<>();

    {
      final Map<String, SerializedParameter> parameters = new HashMap<>();
      parameters.put("peelDirection", SerializedParameter.of("fromStem"));
      schedule.add(Pair.of(0.0, new SerializedActivity("PeelBanana", parameters)));
    }
    {
      final Map<String, SerializedParameter> parameters = new HashMap<>();
      parameters.put("biteSize", SerializedParameter.of(0.5));
      schedule.add(Pair.of(0.5, new SerializedActivity("BiteBanana", parameters)));
    }
    {
      final Map<String, SerializedParameter> parameters = new HashMap<>();
      schedule.add(Pair.of(1.5, new SerializedActivity("BiteBanana", parameters)));
    }

    return schedule;
  }

  public static void main(final String[] args) {
    final MerlinAdaptation adaptation = getAdaptation("Banananation", "0.0.1")
        .orElseThrow(() -> new RuntimeException("Unable to find adaptation"));

    System.out.println("adaptation name: " + adaptation.getClass().getAnnotation(Adaptation.class).name());
    System.out.println("adaptation version: " + adaptation.getClass().getAnnotation(Adaptation.class).version());
    System.out.println("speed of light: " + CSPICE.clight());

    final ActivityMapper activityMapper = adaptation.getActivityMapper();
    System.out.println(activityMapper.getActivitySchemas());

    final List<Pair<Double, SerializedActivity>> schedule = getSchedule();

    // Reusable code; part of Merlin SDK.
    final List<Pair<Double, Activity>> activities = new ArrayList<>();
    for (final Pair<Double, SerializedActivity> spec : schedule) {
      final double time = spec.getLeft();
      final SerializedActivity serializedActivity = spec.getRight();

      final Activity activity = activityMapper
          .deserializeActivity(serializedActivity)
          .orElseThrow(() -> new RuntimeException("No deserializer for activity type `" + serializedActivity.getTypeName() + "`"));

      activities.add(Pair.of(time, activity));
    }

    System.out.println(activities);

    // Reusable code; part of Merlin SDK.
    final List<Pair<Double, SerializedActivity>> newSchedule = new ArrayList<>();
    for (final Pair<Double, Activity> entry : activities) {
      final double time = entry.getLeft();
      final Activity activity = entry.getRight();

      final SerializedActivity serializedActivity = activityMapper
          .serializeActivity(activity)
          .orElseThrow(() -> new RuntimeException("No serializer for activity with class `" + activity.getClass().getSimpleName() + "`"));

      newSchedule.add(Pair.of(time, serializedActivity));
    }

    System.out.println(newSchedule);
  }
}
