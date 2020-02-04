package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

public interface SystemModel<SliceType extends Slice> {
    /*
    For now, putting all the methods here that are necessary to make system models
    work in this current configuration.  We can rework later.
    */

    SliceType getInitialSlice();
}
