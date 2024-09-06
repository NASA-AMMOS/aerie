package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.banananation.Flag;
import gov.nasa.jpl.aerie.banananation.Mission;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.waitUntil;

/**
 * Await a change in producer, then toggle flag
 *
 * The purpose of this activity is to demonstrate waitUntil
 */
@ActivityType("ToggleFlagWhenProducerChanges")
public final class ToggleFlagWhenProducerChanges {
  @EffectModel
  public void run(final Mission mission) {
    final var currentProducer = mission.producer.get();
    waitUntil(mission.producer.is(currentProducer).not());
    final var currentFlag = mission.flag.get();
    mission.flag.set(
        switch (currentFlag) {
          case A -> Flag.B;
          case B -> Flag.A;
        }
    );
  }
}

//record Command(String name, Map<String, Object> parameters);
//
//new Command("SET_GLOBAL", List.of(Pair.of("globalName", String.class), Pair.of("value", Object.class));
//
//
//// Range checks etc?
//triggerOnStart("SET_GLOBAL", args, mission => {
//  mission.globals.get(args.get("globalName")).set(args.get("value"));
//});
//
//triggerOnStart("SET_GLOBAL", args, mission => {
//  mission.globals.get(args.get("globalName")).set(args.get("value"));
//});
//
//triggerOnStart("SET_GLOBAL", args, mission => {
//  mission.globals.get(args.get("globalName")).set(args.get("value"));
//});
//
//triggerOnStart(DoEdl) {
//  // Power model
//}
