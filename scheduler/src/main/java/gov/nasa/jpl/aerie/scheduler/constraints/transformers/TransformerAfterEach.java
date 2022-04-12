package gov.nasa.jpl.aerie.scheduler.constraints.transformers;

import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

public class TransformerAfterEach implements TimeWindowsTransformer {

  private final Duration dur;

  public TransformerAfterEach(Duration dur) {
    this.dur = dur;
  }


  @Override
  public Windows transformWindows(Plan plan, Windows windows) {
    var retWin = new Windows(windows);
    retWin = retWin.complement();
    retWin = retWin.removeFirst();
    retWin = retWin.contractBy(dur, Duration.ZERO);
    return retWin;
  }
}
