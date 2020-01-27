package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

public interface SystemModel {

    public Slice step(Slice slice, Duration dt);

    public void registerSelf();

    public Slice saveToSlice();

    public MissionModelGlue.Registry getRegistry();

    public MissionModelGlue.EventApplier getEventAplier();


    public Slice getSlice();

}
