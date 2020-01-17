package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

public interface SystemModel {

    public void step(Slice slice);

    public void registerSelf();

}
