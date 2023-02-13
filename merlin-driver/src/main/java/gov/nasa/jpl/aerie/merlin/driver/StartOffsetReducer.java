package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RecursiveTask;


public class StartOffsetReducer extends RecursiveTask<HashMap<ActivityDirectiveId, List<Pair<ActivityDirectiveId, Duration>>>> {
  private final Duration planDuration;
  private final Map<ActivityDirectiveId, ActivityDirective> completeMapOfDirectives;
  private final Map<ActivityDirectiveId, ActivityDirective> activityDirectivesToProcess;

  public StartOffsetReducer(Duration planDuration, Map<ActivityDirectiveId, ActivityDirective> activityDirectives){
    this.planDuration = planDuration;
    this.completeMapOfDirectives = activityDirectives;
    this.activityDirectivesToProcess = activityDirectives;
  }

  private StartOffsetReducer(
      Duration planDuration,
      Map<ActivityDirectiveId, ActivityDirective> activityDirectives,
      Map<ActivityDirectiveId, ActivityDirective> allActivityDirectives){
    this.planDuration = planDuration;
    this.activityDirectivesToProcess = activityDirectives;
    this.completeMapOfDirectives = allActivityDirectives;
  }

  /**
   * The complexity of compute() is ~O(NL), where N is the number of activities and L is the length of the longest chain
   * In general, we expect L to be small.
   */
  @Override
  public HashMap<ActivityDirectiveId, List<Pair<ActivityDirectiveId, Duration>>> compute() {
    final var toReturn = new HashMap<ActivityDirectiveId, List<Pair<ActivityDirectiveId, Duration>>>();
    // If we have 400 or fewer activities to process, process them directly
    if(activityDirectivesToProcess.size() <= 400) {
      for (final var entry : activityDirectivesToProcess.entrySet()){
        final var dependingActivity = getNetOffset(entry.getValue());
        toReturn.putIfAbsent(dependingActivity.getLeft(), new ArrayList<>());
        toReturn.get(dependingActivity.getLeft()).add(Pair.of(entry.getKey(), dependingActivity.getValue()));
      }
      return toReturn;
    }
    // else split the map in half and process each side in parallel
    final var leftDirectivesToProcess = new HashMap<ActivityDirectiveId, ActivityDirective>(activityDirectivesToProcess.size()/2);
    final var rightDirectivesToProcess = new HashMap<ActivityDirectiveId, ActivityDirective>(activityDirectivesToProcess.size()/2);
    int count=0;
    for(var entry : activityDirectivesToProcess.entrySet()) {
      (count<(activityDirectivesToProcess.size()/2) ? leftDirectivesToProcess : rightDirectivesToProcess).put(entry.getKey(), entry.getValue());
      count++;
    }
    final var left = new StartOffsetReducer(planDuration, leftDirectivesToProcess, completeMapOfDirectives);
    final var right = new StartOffsetReducer(planDuration, rightDirectivesToProcess, completeMapOfDirectives);
    right.fork();
    // join step
    final var leftReturn = left.compute();
    final var rightReturn = right.join();

    leftReturn.forEach((key , value) -> {
      final var list = toReturn.get(key);
      if (list == null) { toReturn.put(key,value); }
      else {
        toReturn.get(key).addAll(value); // There are no duplicate entries in the lists to be merged.
      }
    });

    rightReturn.forEach((key , value) -> {
      final var list = toReturn.get(key);
      if (list == null) { toReturn.put(key,value); }
      else {
        toReturn.get(key).addAll(value); // There are no duplicate entries in the lists to be merged.
      }
    });

    return toReturn;
  }


  /**
   * Gets the greatest net offset of a given ActivityDirective
   * Base cases:
   *    1) Activity is anchored to plan
   *    2) Activity is anchored to the end time of another activity
   * @param ad The ActivityDirective currently under consideration
   * @return A Pair containing:
   *   ActivityDirectiveID: the ID of the activity that must finish being simulated before we can simulate the specified activity
   *   Duration: the net start offset from that ID
   */
  private Pair<ActivityDirectiveId, Duration> getNetOffset(ActivityDirective ad){
    ActivityDirective currentActivityDirective = ad;
    ActivityDirectiveId currentAnchorId = ad.anchorId();
    boolean anchoredToStart = ad.anchoredToStart();
    Duration netOffset = ad.startOffset();

    while(currentAnchorId != null && anchoredToStart){
      currentActivityDirective = completeMapOfDirectives.get(currentAnchorId);
      currentAnchorId = currentActivityDirective.anchorId();
      anchoredToStart = currentActivityDirective.anchoredToStart();
      netOffset = netOffset.plus(currentActivityDirective.startOffset());
    }

    if(currentAnchorId == null && !anchoredToStart) {
      return Pair.of(null, planDuration.plus(netOffset)); // Add plan duration if anchored to plan end for net
    }
    return Pair.of(currentAnchorId, netOffset);
  }
}

