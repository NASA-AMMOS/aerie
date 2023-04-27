package gov.nasa.jpl.aerie.scheduler.solver.stn;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Adapter for TaskNetwork for use with Interval and Duration
 */
public class TaskNetworkAdapter {

  private final TaskNetwork tw;

  public TaskNetworkAdapter(TaskNetwork tw) {
    this.tw = tw;
  }

  public record TNActData(Interval start, Interval end, Interval duration) {}

  public void addDurationInterval(String nameAct, Duration lb, Duration ub) {
    tw.addDurationInterval(nameAct, toDouble(lb), toDouble(ub));
  }

  public Interval getStartInterval(String actName) {
    var st = tw.getStartInterval(actName);
    return toWin(st.getLeft(), st.getRight());
  }

  public TNActData getAllData(String nameAct) {
    var data = tw.getAllData(nameAct);
    return new TNActData(toWin(data.start()), toWin(data.end()), toWin(data.duration()));
  }

  public Interval getEndInterval(String actName) {
    return toWin(tw.getEndInterval(actName));
  }

  public Interval getDurationInterval(String actName) {
    return toWin(tw.getDurationInterval(actName));
  }

  /**
   * Adds an enveloppe at absolute times t1, t2 for activity nameAct
   */
  public void addEnveloppe(String nameAct, String envName, Duration t1, Duration t2) {
    tw.addEnveloppe(nameAct, envName, toDouble(t1), toDouble(t2));
  }

  public void addStartInterval(String actName, Duration t1, Duration t2) {
    tw.addStartInterval(actName, toDouble(t1), toDouble(t2));
  }

  /**
   * Adds an absolute time interval for activity
   */
  public void addEndInterval(String actName, Duration lb, Duration ub) {
    tw.addEndInterval(actName, toDouble(lb), toDouble(ub));
  }

  public void startsAfterEnd(String actBefore, String actAfter) {
    tw.startsAfterEnd(actBefore, actAfter);
  }

  public void addAct(String name) {
    tw.addAct(name);
  }

  public void print() {
    tw.print();
  }

  public boolean solveConstraints() {
    return tw.propagate();
  }

  private Duration toDur(double d) {
    return Duration.of(Math.round(d), Duration.MICROSECOND);
  }

  private double toDouble(Duration dur) {
    return (double) dur.in(Duration.MICROSECOND);
  }

  private Interval toWin(double d1, double d2) {
    return Interval.between(toDur(d1), toDur(d2));
  }

  private Interval toWin(Pair<Double, Double> pair) {
    return toWin(pair.getLeft(), pair.getRight());
  }

  public static TNActData solve(Interval start, Interval end, Interval dur, Interval enveloppe) {
    TaskNetwork tw = new TaskNetwork();
    String actName = "ACT";
    TaskNetworkAdapter tnw = new TaskNetworkAdapter(tw);
    if (start != null) {
      tnw.addStartInterval(actName, start.start, start.end);
    }
    if (end != null) {
      tnw.addStartInterval(actName, end.start, end.end);
    }
    if (dur != null) {
      tnw.addDurationInterval(actName, dur.start, dur.end);
    }
    if (enveloppe != null) {
      tnw.addEnveloppe(actName, "ENV", enveloppe.start, enveloppe.end);
    }
    if (tnw.solveConstraints()) {
      return tnw.getAllData(actName);
    } else {
      return null;
    }
  }
}
