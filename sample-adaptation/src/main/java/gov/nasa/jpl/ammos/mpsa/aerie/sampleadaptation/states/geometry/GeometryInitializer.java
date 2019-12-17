package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.geometry;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.Globals.Apsis;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.Globals.Body;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.StateModels.ApsidesTimesModel;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.Config;

public class GeometryInitializer {
    public static ApsidesTimesModel initPerijoveTimesModel(Config config) {
        return initJovianApsidesTimesModel(Apsis.PERIAPSIS,config);
    }

    public static ApsidesTimesModel initApojoveTimesModel(Config config) {
        return initJovianApsidesTimesModel(Apsis.APOAPSIS,config);
    }

    public static ApsidesTimesModel initJovianApsidesTimesModel(Apsis apsisType, Config config) {
        double filter;

        switch (apsisType) {
        case APOAPSIS:
            filter = 0;
            break;
        case PERIAPSIS:
            filter = Double.POSITIVE_INFINITY;
            break;
        default:
            throw new Error(
                    "unexpected " + apsisType.getClass().getSimpleName() + " with value " + String.valueOf(apsisType));
        }
        ApsidesTimesModel model = new ApsidesTimesModel();
        // do I need to set the engine here?
        // model.setEngine(engine);
        model.setStart(config.missionStartTime);
        model.setEnd(config.missionEndTime);
        model.setFilter(filter);
        model.setTarget(Body.JUPITER_BARYCENTER);
        model.setObserver(Body.JUNO);
        model.setApsisType(apsisType);
        return model;
    }
}