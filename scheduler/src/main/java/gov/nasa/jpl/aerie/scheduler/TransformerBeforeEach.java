package gov.nasa.jpl.aerie.scheduler;

public class TransformerBeforeEach implements TimeWindowsTransformer {

  Duration dur;

  public TransformerBeforeEach(Duration dur) {
    this.dur = dur;
  }


  @Override
  public TimeWindows transformWindows(Plan plan, TimeWindows windows) {
    var retWin = new TimeWindows(windows);
    retWin.complement();
    retWin.removeLast();
    retWin.contractBy(Duration.ofZero(), dur);
    return retWin;
  }
}
