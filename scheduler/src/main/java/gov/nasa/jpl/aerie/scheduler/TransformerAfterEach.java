package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

public class TransformerAfterEach implements TimeWindowsTransformer {

  Duration dur;

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
