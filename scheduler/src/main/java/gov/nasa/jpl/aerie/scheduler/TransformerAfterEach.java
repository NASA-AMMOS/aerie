package gov.nasa.jpl.aerie.scheduler;

public class TransformerAfterEach implements TimeWindowsTransformer {

  Duration dur;

  public TransformerAfterEach(Duration dur) {
    this.dur = dur;
  }


  @Override
  public TimeWindows transformWindows(Plan plan, TimeWindows windows) {
    var retWin = new TimeWindows(windows);
    retWin.complement();
    retWin.removeFirst();
    retWin.contractBy(dur, Duration.ofZero());
    return retWin;
  }
}
