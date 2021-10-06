package gov.nasa.jpl.aerie.scheduler;

public interface TimeWindowsTransformer {

    public TimeWindows transformWindows(Plan plan, TimeWindows windows);

}
