package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

public interface SystemModel {

    /*
    For now, putting all the methods here that are necessary to make system models
    work in this current configuration.  We can rework later.
     */
    public void step(Slice slice, Duration dt);

    public void registerSelf();

    public MissionModelGlue.Registry getRegistry();

    public MissionModelGlue.MasterSystemModel getMasterSystemModel();

    public Slice getInitialSlice();

    public String getName();

    public void mapStateNameToSystemModelName(String stateName);
}
