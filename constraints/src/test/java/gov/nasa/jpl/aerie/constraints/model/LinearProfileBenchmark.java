package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Window;

import java.util.ArrayList;
import java.util.List;

import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Inclusive;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;

public class LinearProfileBenchmark {

  private static List<LinearProfilePiece> getSubSequenceP1(final long start){
    return List.of(
        new LinearProfilePiece(Window.between(start, Inclusive, start + 4, Exclusive, SECONDS), 0, 1),
        new LinearProfilePiece(Window.between( start + 4, Inclusive,  start + 8, Exclusive, SECONDS),  4,  0),
        new LinearProfilePiece(Window.between( start + 8, Inclusive, start + 12, Exclusive, SECONDS),  4, -1),
        new LinearProfilePiece(Window.between(start + 12, Inclusive, start + 16, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Window.between(start + 16, Inclusive, start + 20, Inclusive, SECONDS),  0,  0)
    );
  }

  private static List<LinearProfilePiece> getSubSequenceP2(final long start){
    return List.of(
        new LinearProfilePiece(Window.between(start, Inclusive, start + 2, Exclusive, SECONDS), 0, 1),
        new LinearProfilePiece(Window.between( start + 2, Inclusive,  start + 4, Exclusive, SECONDS),  2,  0),
        new LinearProfilePiece(Window.between( start + 4, Inclusive,  start + 6, Exclusive, SECONDS),  2,  1),
        new LinearProfilePiece(Window.between( start + 6, Inclusive, start + 12, Exclusive, SECONDS),  4,  0),
        new LinearProfilePiece(Window.between(start + 12, Inclusive, start + 16, Exclusive, SECONDS),  4, -1),
        new LinearProfilePiece(Window.between(start + 16, Inclusive, start + 20, Inclusive, SECONDS),  0,  0)
    );
  }

  public static void firstMethod(LinearProfile profile1, LinearProfile profile2, int times, long durationSequence){
    //test at the middle of the profile
    final var testAt = times / 2;
    profile1.greaterThan(profile2, Window.between(testAt * durationSequence, (testAt + 1) * durationSequence, SECONDS));
  }

  public static void secondMethod(final LinearProfile profile1, final LinearProfile profile2, final long start, final int times, final long durationSequence){
    profile1.greaterThan(profile2, Window.between(start, times * durationSequence, SECONDS));
  }


  public static void main(String[] args){
    //number of times the sub-sequences are repeated
    final var times = 1000;
    final var start = 0L;
    //duration of a sub-sequence
    final var durationSequence = 20;
    //number of runs
    final var nbRuns = 30;

    final var list1 = new ArrayList<LinearProfilePiece>();
    final var list2 = new ArrayList<LinearProfilePiece>();

    for(var i = 0; i < times; i++){
      long startsub = start + (i * durationSequence);
      list1.addAll(getSubSequenceP1(startsub));
      list2.addAll(getSubSequenceP2(startsub));
    }
    final var profile1 = new LinearProfile(list1);
    final var profile2 = new LinearProfile(list2);

    var totalTimeFirstMethod = 0L;
    for(int i = 0; i < nbRuns ; i++) {
      final var before = System.nanoTime();
      firstMethod(profile1, profile2, times, durationSequence);
      totalTimeFirstMethod += (System.nanoTime() - before);
    }

    var totalTimeSecondMethod = 0L;

    for(int i = 0; i < nbRuns ; i++) {
      final var before2 = System.nanoTime();
      secondMethod(profile1, profile2, start, times, durationSequence);
      totalTimeSecondMethod += (System.nanoTime() - before2);
    }

    final var timePerRun1 = (float) totalTimeFirstMethod/nbRuns;
    final var timePerRun2 = (float) totalTimeSecondMethod/nbRuns;

    //When first comparing, testing on restricted bounds was about 100 times faster than without bounds
    System.out.println( timePerRun1+ " "  + timePerRun2  + " ratio = " + (timePerRun1/timePerRun2));
  }

}
